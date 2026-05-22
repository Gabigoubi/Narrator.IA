package com.gabigoubi.narradoria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpAssistant {
    
    private static final String API_URL = "http://localhost:8000/narrate";
    
    // Fail-fast on connection, but allow long times for LLM inference (read timeout)
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) 
            .build();

    // Thread-safe lock mechanism to prevent race conditions across async boundaries
    private static final AtomicBoolean isNarrating = new AtomicBoolean(false);

    /**
     * Receives the pre-compiled JSON payload from GameEventListener and dispatches it.
     */
    public static void sendStructuredTelemetry(String jsonPayload) {
        
        // Atomic Check-and-Set: Guarantees absolute thread safety
        if (!isNarrating.compareAndSet(false, true)) {
            System.out.println("[HttpAssistant] [BLOCKED] System is busy narrating. Payload discarded.");
            return;
        }

        System.out.println("[HttpAssistant] [LOCK] Acquired. Transmitting structural payload...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(5)) // Allows up to 5 mins for Python to generate Audio
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    System.out.println("[HttpAssistant] API Response Status: " + response.statusCode());

                    if (response.statusCode() == 200) {
                        System.out.println("[HttpAssistant] Success. Delegating byte array to AudioPlayer.");
                        // Note: AudioPlayer is responsible for calling releaseLock() when playback finishes
                        AudioPlayer.play(response.body());
                    } else {
                        NarradorIAMod.LOGGER.error("[HttpAssistant] API Error. Status Code: " + response.statusCode());
                        releaseLock();
                    }
                })
                .exceptionally(ex -> {
                    NarradorIAMod.LOGGER.error("[HttpAssistant] Critical HTTP POST Exception: " + ex.getMessage());
                    releaseLock();
                    return null;
                });
    }

    /**
     * Safely exposes the unlock mechanism for the AudioPlayer class.
     */
    public static void releaseLock() {
        isNarrating.set(false);
        System.out.println("[HttpAssistant] [UNLOCK] System released for new telemetry events.");
    }
}
