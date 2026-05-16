package com.iacontext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    record Parte(String text) {}
    record Conteudo(String role, List<Parte> parts) {}
    record RequisicaoGemini(List<Conteudo> contents) {}

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Gson gson = new Gson();
        ClienteIA inteligenciaArtificial = new GeminiService();
        GerenciadorBancoDeDados bancoDeDados = new GerenciadorBancoDeDados();

        List<Conteudo> historicoConversa = new ArrayList<>();

        System.out.println("=== AI Context Manager (Iniciando Motor RAG Local) ===");

        // --- RESTAURAÇÃO DE MEMÓRIA ---
        List<String[]> mensagensSalvas = bancoDeDados.carregarHistorico();
        for (String[] msg : mensagensSalvas) {
            String role = msg[0];
            String texto = msg[1];
            historicoConversa.add(new Conteudo(role, List.of(new Parte(texto))));
        }

        System.out.println("Memória restaurada: " + historicoConversa.size() + " interações passadas encontradas no disco.");
        System.out.println("A IA lembra de tudo. Digite algo (ou 'sair' para encerrar):");

        // --- LOOP PRINCIPAL ---
        while (true) {
            System.out.print("\n> ");
            String entradaUsuario = scanner.nextLine();

            if (entradaUsuario.equalsIgnoreCase("sair")) {
                System.out.println("O histórico está salvo em segurança no arquivo memoria.db. Encerrando...");
                break;
            }

            // 1. Guarda na memória RAM e salva fisicamente no Disco
            historicoConversa.add(new Conteudo("user", List.of(new Parte(entradaUsuario))));
            bancoDeDados.salvarMensagem("user", entradaUsuario);

            // 2. Transforma tudo em JSON e envia
            RequisicaoGemini requisicao = new RequisicaoGemini(historicoConversa);
            String jsonParaEnviar = gson.toJson(requisicao);

            System.out.println("A IA está refletindo sobre o histórico...");
            String respostaBrutaJSON = inteligenciaArtificial.enviarRequisicao(jsonParaEnviar);

            try {
                JsonObject jsonObjeto = JsonParser.parseString(respostaBrutaJSON).getAsJsonObject();
                String textoRespostaIA = jsonObjeto.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();

                System.out.println("\nIA: " + textoRespostaIA);

                // 3. Guarda a resposta da IA na RAM e também salva no Disco
                historicoConversa.add(new Conteudo("model", List.of(new Parte(textoRespostaIA))));
                bancoDeDados.salvarMensagem("model", textoRespostaIA);

            } catch (Exception e) {
                System.out.println("Erro crítico: A estrutura da resposta da IA falhou.");
                System.out.println("Dump do erro: " + respostaBrutaJSON);
            }
        }

        scanner.close();
    }
}