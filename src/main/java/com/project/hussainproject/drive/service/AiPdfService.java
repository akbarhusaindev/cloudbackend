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
 *  - Google Gemini  → summarise or answer questions about the text
 *
 * Extracted text is cached in-memory per file ID to avoid re-downloading
 * the PDF on every request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPdfService {

    private final FileRepository fileRepository;
    private final ShareRepository shareRepository;
    private final StorageService storageService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // ── In-memory cache: fileId → extracted PDF text ───────────────────
    private final ConcurrentHashMap<UUID, String> pdfTextCache = new ConcurrentHashMap<>();

    private static final String[] GEMINI_MODELS = {
            "gemini-3.8-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
    };

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Generate a concise summary of the PDF.
     */
    public String summarise(UUID fileId, User user) {
        String text = extractText(fileId, user);
        String prompt = "You are a helpful assistant. Summarise the following document in 3–5 clear bullet points. "
                + "Focus on the key topics, findings, and conclusions.\n\nDocument:\n" + truncate(text);
        return callGemini(prompt);
    }

    /**
     * Answer a specific question about the PDF.
     */
    public String chat(UUID fileId, User user, String question) {
        String text = extractText(fileId, user);
        String prompt = "You are a helpful assistant. Use ONLY the document below to answer the user's question. "
                + "If the answer is not in the document, say so clearly.\n\n"
                + "Document:\n" + truncate(text)
                + "\n\nQuestion: " + question;
        return callGemini(prompt);
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
     * Call Google Gemini generateContent REST API.
     * Uses the 'x-goog-api-key' header (required for newer 'AQ.' API key format)
     * and tries modern models ('gemini-1.5-flash', 'gemini-2.0-flash').
     */
    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.contains("dummy")) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured. Please set GEMINI_API_KEY in your environment or Render settings.");
        }

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
            try {
                ResponseEntity<Map> response = rest.postForEntity(url, request, Map.class);

                // Parse: response.candidates[0].content.parts[0].text
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates == null || candidates.isEmpty()) {
                    throw new RuntimeException("No response generated by Gemini.");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");

            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                lastException = e;
                log.warn("Gemini model {} returned status {}: {}", model, e.getStatusCode(), e.getResponseBodyAsString());
                // If 404, try next model in GEMINI_MODELS
                if (e.getStatusCode().value() == 404) {
                    continue;
                }
                throw new RuntimeException("Gemini API error: " + e.getStatusCode() + ". Check your API key or quota.", e);
            } catch (Exception e) {
                lastException = e;
                log.error("Gemini API call failed for model {}: {}", model, e.getMessage());
            }
        }

        throw new RuntimeException("Gemini API call failed: " + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    /**
     * Truncate text to ~28 000 characters to stay within Gemini's context window.
     * Gemini 1.5 Flash supports ~1M tokens; 28k chars ≈ ~7k tokens — plenty for most PDFs.
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

