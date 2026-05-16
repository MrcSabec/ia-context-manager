package com.iacontext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService implements ClienteIA {
    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    @Override
    public String enviarRequisicao(String corpoJson) {
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            return "Erro: A variável de ambiente GEMINI_API_KEY não foi configurada.";
        }

        String urlCompleta = URL_BASE + apiKey;

        try {
            // Cria o cliente HTTP do Java 21
            HttpClient client = HttpClient.newHttpClient();

            // Monta a requisição POST com o JSON do histórico
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlCompleta))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpoJson))
                    .build();

            // Dispara para os servidores do Google e aguarda a resposta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Retorna o corpo da resposta (que virá em formato JSON)
            return response.body();

        } catch (Exception e) {
            return "Erro crítico na comunicação com o Gemini: " + e.getMessage();
        }
    }
}