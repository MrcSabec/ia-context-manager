package com.iacontext;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OllamaService {

    // A URL padrão onde o Ollama roda
    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL_NAME = "llama3";

    private final Gson gson;

    public OllamaService() {
        this.gson = new Gson();
    }

    /**
     * Envia a mensagem e o contexto das anotações para o Ollama.
     * @param userMessage A mensagem digitada por você no site.
     * @param context As notas lidas do Obsidian (ou um resumo delas).
     * @param isCreative Define se a IA deve inventar coisas (true) ou ser rígida (false).
     * @return A resposta gerada pelo Llama 3 ou o erro detalhado.
     */
    public String sendMessage(String userMessage, String context, boolean isCreative) {
        try {
            URL url = new URL(OLLAMA_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // 1. Construção do Payload JSON usando Gson
            JsonObject payload = new JsonObject();
            payload.addProperty("model", MODEL_NAME);
            payload.addProperty("stream", false);

            // --- CHAVE DE IGNIÇÃO DINÂMICA (Criatividade vs. Lógica) ---
            JsonObject options = new JsonObject();
            options.addProperty("temperature", isCreative ? 0.8 : 0.1);
            options.addProperty("top_p", isCreative ? 0.9 : 0.5);
            payload.add("options", options);

            JsonArray messagesArray = new JsonArray();

            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");

            String promptMestre;
            if (isCreative) {
                promptMestre = "Você é o ÉDEN, atuando agora no MODO CRIATIVO como Mestre de RPG. " +
                        "Sua tarefa é gerar conteúdo novo, interessante e imersivo com base no pedido do usuário. " +
                        "Você tem permissão total para inventar nomes, lugares, lore e situações para enriquecer a narrativa. " +
                        "Use os dados de contexto abaixo como inspiração para manter a coerência com o mundo da campanha:\n\n" + context;
            } else {
                promptMestre = "Você é o ÉDEN, um assistente virtual analítico e de altíssima precisão. " +
                        "REGRAS ABSOLUTAS:\n" +
                        "1. Você DEVE responder baseando-se EXCLUSIVAMENTE no [CONHECIMENTO EXTRAÍDO] fornecido abaixo.\n" +
                        "2. Se a resposta para a pergunta não estiver claramente escrita nestes dados, diga APENAS: 'Mestre, não possuo esta informação nos meus registros atuais.'\n" +
                        "3. NUNCA invente, presuma ou deduza nomes, datas, lore ou regras que não estejam no texto.\n\n" +
                        "Analise estes dados: \n\n" + context;
            }
            // -----------------------------------------------------------

            systemMessage.addProperty("content", promptMestre);
            messagesArray.add(systemMessage);

            JsonObject userJsonMessage = new JsonObject();
            userJsonMessage.addProperty("role", "user");
            userJsonMessage.addProperty("content", userMessage);
            messagesArray.add(userJsonMessage);

            payload.add("messages", messagesArray);
            
            String jsonInputString = gson.toJson(payload);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                return "Erro Crítico do Servidor Local: HTTP " + statusCode;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
                return jsonResponse.getAsJsonObject("message").get("content").getAsString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Falha na comunicação com o Ollama: " + e.getMessage();
        }
    }
}