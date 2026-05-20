package com.gabigoubi.narradoria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpAssistant {
    private static final String API_URL = "http://localhost:8000/narrate";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofMinutes(5))
            .build();


    public static CompletableFuture<byte[]> sendNarrateRequest(String eventType, String contextDetails, String voiceModel) {

        String jsonPayload = String.format(
                "{\"event_type\":\"%s\",\"context_details\":\"%s\",\"voice_model\":\"%s\"}",
                eventType, contextDetails, voiceModel
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {

                        AudioPlayer.play(response.body());
                        return response.body();
                    }
                    NarradorIAMod.LOGGER.error("API error code: " + response.statusCode());
                    return new byte[0];
                })
                .exceptionally(ex -> {
                    NarradorIAMod.LOGGER.error("Failed to connect to Python API: " + ex.getMessage());
                    return new byte[0];
                });
    }
}