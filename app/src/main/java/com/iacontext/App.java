package com.iacontext;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // A Lista atua como a nossa "Janela de Contexto" temporária
        List<String> historicoDeContexto = new ArrayList<>();

        System.out.println("=== AI Context Manager (Modo Local MOCK) ===");
        System.out.println("O sistema está rodando. Digite algo (ou 'sair' para encerrar):");

        while (true) {
            System.out.print("\n> ");
            String entradaUsuario = scanner.nextLine();
            if (entradaUsuario.equalsIgnoreCase("sair")) {
                System.out.println("Salvando contexto (simulado) e encerrando...");
                break;
            }

            // 1. Armazena a mensagem do usuário na memória
            historicoDeContexto.add("Usuário: " + entradaUsuario);

            // 2. Simula o processamento da IA
            // Em breve, esta linha chamará a classe de requisição HTTP para o Gemini
            String respostaIA = "Mensagem recebida e guardada! Tamanho atual do contexto: "
                    + historicoDeContexto.size() + " registro(s).";

            // 3. Exibe a resposta e também a salva na memória
            System.out.println("IA: " + respostaIA);
            historicoDeContexto.add("IA: " + respostaIA);
        }

        scanner.close();
    }
}