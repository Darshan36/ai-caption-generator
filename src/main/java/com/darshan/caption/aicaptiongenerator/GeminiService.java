package com.darshan.caption.aicaptiongenerator;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Set;

@Service
public class GeminiService {

    private static final String API_KEY = "YOUR_GEMINI_API_KEY";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );

    private String getPromptForStyle(String style) {
        return switch (style) {
            case "Funny" -> "Generate exactly 3 funny, witty, or sarcastic captions for this image. Each caption must be on a new line and start with a *. Do not add any other text or titles.";
            case "Inspirational" -> "Generate exactly 3 inspirational or motivational captions for this image. Each caption must be on a new line and start with a *. Do not add any other text or titles.";
            case "One Word" -> "Generate exactly 3 single-word captions that describe the essence of this image. Each caption must be on a new line and start with a *. Do not add any other text or titles.";
            case "As a Question" -> "Generate exactly 3 engaging questions about this image that would make a good caption. Each caption must be on a new line and start with a *. Do not add any other text or titles.";
            default -> "Generate exactly 3 short and meaningful captions for this image. Each caption must be on a new line and start with a *. Do not add any other text or titles.";
        };
    }

    public String generateCaption(MultipartFile image, String style) throws IOException {
        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            return "Error: Unsupported image format. Please use JPEG, PNG, or WebP.";
        }
        
        String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        OkHttpClient client = new OkHttpClient();

        JSONObject textPart = new JSONObject().put("text", getPromptForStyle(style));
        JSONObject inlineDataValue = new JSONObject().put("mime_type", contentType).put("data", base64Image);
        JSONObject inlineDataPart = new JSONObject().put("inline_data", inlineDataValue);
        JSONArray partsArray = new JSONArray().put(textPart).put(inlineDataPart);
        JSONObject content = new JSONObject().put("parts", partsArray);
        JSONArray contentsArray = new JSONArray().put(content);
        
        JSONObject finalBody = new JSONObject().put("contents", contentsArray);

        JSONObject generationConfig = new JSONObject()
            .put("temperature", 0.4)
            .put("topK", 32)
            .put("topP", 1)
            .put("maxOutputTokens", 1024);
        finalBody.put("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(finalBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(API_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            String responseString = response.body() != null ? response.body().string() : "{}";

            // --- NEW: Handle the 429 Rate Limit error gracefully ---
            if (response.code() == 429) {
                System.err.println("Gemini API rate limit exceeded (429).");
                return "Error: Too many requests. Please wait a moment and try again.";
            }
            // --- END OF NEW FIX ---

            if (response.code() == 503) {
                System.err.println("Gemini API is currently unavailable (503).");
                return "Error: The AI service is temporarily unavailable. Please try again in a few minutes.";
            }

            if (!response.isSuccessful()) {
                System.err.println("Error from Gemini API: " + responseString);
                return "Error: " + response.code() + " - " + response.message();
            }

            JSONObject jsonResponse = new JSONObject(responseString);
            JSONArray candidates = jsonResponse.optJSONArray("candidates");

            if (candidates != null && !candidates.isEmpty()) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                if (firstCandidate.has("finishReason") && !firstCandidate.getString("finishReason").equals("STOP")) {
                    return "Caption generation stopped. Reason: " + firstCandidate.getString("finishReason") + ". This could be due to safety settings.";
                }
                
                JSONObject contentNode = firstCandidate.optJSONObject("content");
                if (contentNode != null) {
                    JSONArray parts = contentNode.optJSONArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return parts.getJSONObject(0).optString("text", "No caption found in response parts.");
                    }
                }
            }
            
            return "Could not generate a caption. The API returned an empty response.";
        } catch (Exception e) {
            e.printStackTrace();
            return "An exception occurred while contacting the API: " + e.getMessage();
        }
    }
}

