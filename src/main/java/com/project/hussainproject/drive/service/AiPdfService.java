package com.project.hussainproject.drive.service;

import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.ShareRepository;
import com.project.hussainproject.drive.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles AI-powered PDF analysis using:
 *  - Apache PDFBox  → extract text from PDF stored on Supabase S3
 *  - Groq (Llama 3.3 70B) & Google Gemini → ultra-fast summary and Q&A
 *
 * Groq is preferred when GROQ_API_KEY is present for high-speed LPU inference,
 * with automatic fallback to Gemini if needed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPdfService {

    private final FileRepository fileRepository;
    private final ShareRepository shareRepository;
    private final StorageService storageService;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    // ── In-memory cache: fileId → extracted PDF text ───────────────────
    private final ConcurrentHashMap<UUID, String> pdfTextCache = new ConcurrentHashMap<>();

    private static final String[] GROQ_MODELS = {
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "qwen/qwen3.8-27b"
    };

    private static final String[] GEMINI_MODELS = {
            "gemini-3.8-flash",
            "gemini-3.1-pro-preview"
    };

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Generate a concise summary of the PDF.
     */
    public String summarise(UUID fileId, User user) {
        String text = extractText(fileId, user);
        String prompt = "You are a helpful assistant. Summarise the following document in 3–5 clear bullet points. "
                + "Focus on the key topics, findings, and conclusions. Do not use double asterisks (**) or markdown bold formatting in your response; output plain, clean text.\n\nDocument:\n" + truncate(text);
        return callAi(prompt);
    }

    /**
     * Answer a specific question about the PDF.
     */
    public String chat(UUID fileId, User user, String question) {
        String text = extractText(fileId, user);
        String prompt = "You are a helpful assistant. Use ONLY the document below to answer the user's question. "
                + "If the answer is not in the document, say so clearly. Do not use double asterisks (**) or markdown bold formatting in your response; output plain, clean text.\n\n"
                + "Document:\n" + truncate(text)
                + "\n\nQuestion: " + question;
        return callAi(prompt);
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Verify access, then return extracted PDF text (cached after first call).
     */
    private String extractText(UUID fileId, User user) {
        // Check access
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        boolean isOwner   = file.getOwner().getId().equals(user.getId());
        boolean isShared  = shareRepository.findByFileIdAndSharedWithId(fileId, user.getId()).isPresent();
        if (!isOwner && !isShared) {
            throw new SecurityException("Access denied: you do not have permission to analyse this file.");
        }

        if (!file.getMimeType().equals("application/pdf")) {
            throw new IllegalArgumentException("AI analysis is only supported for PDF files.");
        }

        // Return cached text if available
        return pdfTextCache.computeIfAbsent(fileId, id -> downloadAndExtract(file));
    }

    /**
     * Download PDF from S3 presigned URL and extract its text via PDFBox.
     */
    private String downloadAndExtract(FileMetadata file) {
        try {
            // Generate a short-lived presigned GET URL
            String downloadUrl = storageService.generateDownloadUrl(file.getStorageKey(), "inline");

            try (InputStream is = URI.create(downloadUrl).toURL().openStream();
                 PDDocument doc = Loader.loadPDF(is.readAllBytes())) {

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc).strip();

                if (text.isBlank()) {
                    throw new IllegalStateException("The PDF appears to be empty or image-based (no extractable text).");
                }
                log.info("Extracted {} characters from PDF '{}'", text.length(), file.getOriginalName());
                return text;
            }
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF '{}': {}", file.getOriginalName(), e.getMessage());
            throw new RuntimeException("Failed to read PDF content. Please ensure the file is a valid, text-based PDF.", e);
        }
    }

    /**
     * Unified AI Dispatcher: prefers Groq for instant inference without 503 errors,
     * with seamless fallback to Gemini.
     */
    private String callAi(String prompt) {
        String rawResult;
        boolean hasGroq = groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.contains("dummy");
        boolean hasGemini = geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.contains("dummy");

        if (hasGroq) {
            try {
                rawResult = callGroq(prompt);
                return cleanResponse(rawResult);
            } catch (Exception e) {
                log.warn("Groq request failed, trying Gemini fallback: {}", e.getMessage());
                if (hasGemini) {
                    rawResult = callGemini(prompt);
                    return cleanResponse(rawResult);
                }
                throw e;
            }
        }

        if (hasGemini) {
            rawResult = callGemini(prompt);
            return cleanResponse(rawResult);
        }

        throw new IllegalStateException("Neither GROQ_API_KEY nor GEMINI_API_KEY is configured. Please set at least one in your environment or Render settings.");
    }

    /**
     * Strips markdown bold asterisks (**) and ensures clean plain text output.
     */
    private String cleanResponse(String text) {
        if (text == null) return "";
        return text.replace("**", "").trim();
    }

    /**
     * Call Groq Cloud API (openai/gpt-oss-120b / openai/gpt-oss-20b / qwen/qwen3.8-27b).
     */
    private String callGroq(String prompt) {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey.trim());

        Exception lastEx = null;

        for (String model : GROQ_MODELS) {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an intelligent, accurate document analysis assistant. Answer clearly and concisely based on the document text provided. Do not use double asterisks (**) or markdown bolding in your output; return clean plain text."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.2
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            try {
                ResponseEntity<Map> response = rest.postForEntity(GROQ_URL, request, Map.class);
                if (response.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            } catch (Exception e) {
                lastEx = e;
                log.warn("Groq model {} failed: {}", model, e.getMessage());
            }
        }

        throw new RuntimeException("Groq API error: " + (lastEx != null ? lastEx.getMessage() : "Unknown error"), lastEx);
    }

    /**
     * Call Google Gemini generateContent REST API with header auth and model failover.
     */
    private String callGemini(String prompt) {
        RestTemplate rest = new RestTemplate();

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey.trim());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Exception lastException = null;

        for (String model : GEMINI_MODELS) {
            String url = GEMINI_BASE_URL + model + ":generateContent";

            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    ResponseEntity<Map> response = rest.postForEntity(url, request, Map.class);

                    // Parse: response.candidates[0].content.parts[0].text
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates =
                            (List<Map<String, Object>>) response.getBody().get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                } catch (org.springframework.web.client.HttpStatusCodeException e) {
                    lastException = e;
                    int statusCode = e.getStatusCode().value();
                    log.warn("Gemini model {} (attempt {}) returned status {}: {}", model, attempt, statusCode, e.getResponseBodyAsString());

                    // If 404 (model not found), switch to next model immediately
                    if (statusCode == 404) {
                        break;
                    }

                    // If 503 (server overloaded) or 429 (rate limit) or 500, back off and retry once
                    if ((statusCode == 503 || statusCode == 429 || statusCode == 500) && attempt < 2) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }

                    // Otherwise fall back to the next model
                    break;
                } catch (Exception e) {
                    lastException = e;
                    log.error("Gemini API call failed for model {} (attempt {}): {}", model, attempt, e.getMessage());
                    break;
                }
            }
        }

        String msg = lastException != null ? lastException.getMessage() : "Unknown error";
        if (msg.contains("503") || msg.contains("SERVICE_UNAVAILABLE")) {
            throw new RuntimeException("Google Gemini service is temporarily overloaded. Please try asking again in a few moments.");
        }
        throw new RuntimeException("Gemini API call failed: " + msg, lastException);
    }

    /**
     * Truncate text to ~28 000 characters to stay within context limits.
     */
    private String truncate(String text) {
        int limit = 28_000;
        if (text.length() <= limit) return text;
        return text.substring(0, limit) + "\n\n[... document truncated for AI context ...]";
    }

    /**
     * Evict a file from the text cache (call when a file is deleted or replaced).
     */
    public void evictCache(UUID fileId) {
        pdfTextCache.remove(fileId);
    }
}
