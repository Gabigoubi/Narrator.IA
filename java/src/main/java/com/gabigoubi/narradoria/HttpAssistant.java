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

    public static boolean isNarrating = false;

    public static CompletableFuture<byte[]> sendNarrateRequest(String eventType, String contextDetails, String voiceModel) {

        System.out.println("[HttpAssistant] Evento disparado: " + eventType);

        if (isNarrating) {
            System.out.println("[HttpAssistant] [BLOCKED] Ignorando evento. Edson Calotas ocupado.");
            return CompletableFuture.completedFuture(new byte[0]);
        }

        System.out.println("[HttpAssistant] [LOCK] isNarrating = true. Bloqueando novas requisicoes.");
        isNarrating = true;

        String jsonPayload = String.format(
                "{\"event_type\":\"%s\",\"context_details\":\"%s\",\"voice_model\":\"%s\"}",
                eventType, contextDetails, voiceModel
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("[HttpAssistant] Enviando POST para o Python...");

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    System.out.println("[HttpAssistant] Resposta do Python: Status " + response.statusCode());

                    if (response.statusCode() == 200) {
                        System.out.println("[HttpAssistant] Sucesso. Passando audio para o AudioPlayer.");
                        AudioPlayer.play(response.body());
                        return response.body();
                    }

                    NarradorIAMod.LOGGER.error("[HttpAssistant] Erro na API. Destravando sistema.");
                    isNarrating = false;
                    return new byte[0];
                })
                .exceptionally(ex -> {
                    NarradorIAMod.LOGGER.error("[HttpAssistant] Exception no POST. Destravando sistema. Motivo: " + ex.getMessage());
                    isNarrating = false;
                    return new byte[0];
                });
    }
}