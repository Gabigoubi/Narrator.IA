package com.gabigoubi.narradoria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    /**
     * Permite que outras classes (como o GameEventListener) consultem o status atual.
     */
    public static boolean isNarrating() {
        return isNarrating.get();
    }
// ========================================================================
    // HANDSHAKE DE BOOT (ÉPICO 3)
    // ========================================================================
    
    /**
     * Faz uma requisição SÍNCRONA para o backend Python avaliando a compatibilidade de versão.
     * @param clientVersion A versão atual do mod Java.
     * @return "OK" em caso de sucesso, uma String de erro em caso de defasagem, ou null se o servidor estiver offline.
     */
    public static String checkVersionWithServer(String clientVersion) {
        try {
            // Substitui a rota de narração pela rota de handshake
            String handshakeUrl = API_URL.replace("/narrate", "/handshake");
            
            // Monta o payload enviando a versão do cliente
            JsonObject payload = new JsonObject();
            payload.addProperty("client_version", clientVersion);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(handshakeUrl))
                    .timeout(Duration.ofSeconds(5)) // Fast-fail: não queremos travar a tela de boot do jogo por muito tempo
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            // Chamada SÍNCRONA (client.send em vez de sendAsync)
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Interpreta a resposta do Python
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String status = jsonResponse.has("status") ? jsonResponse.get("status").getAsString() : "error";
                
                if ("ok".equals(status)) {
                    return "OK";
                } else {
                    // Retorna a mensagem exata de quem precisa ser atualizado
                    return jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "Incompatibilidade desconhecida.";
                }
            } else {
                return "O servidor retornou um código HTTP " + response.statusCode();
            }
            
        } catch (java.net.ConnectException e) {
            // Se cair aqui, o servidor Python está desligado
            return null; 
        } catch (Exception e) {
            NarradorIAMod.LOGGER.error("[HttpAssistant] Erro interno durante o Handshake: " + e.getMessage());
            return "Falha interna de comunicação com o servidor local.";
        }
    }


}
