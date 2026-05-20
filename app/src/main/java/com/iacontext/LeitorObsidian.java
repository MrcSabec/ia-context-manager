package com.iacontext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LeitorObsidian {

    /**
     * Lê o conteúdo exato de uma nota Markdown (.md)
     */
    public static String extrairNota(String caminhoCompletoArquivo) {
        try {
            Path caminho = Paths.get(caminhoCompletoArquivo);

            // Verifica se o arquivo existe e se é um arquivo .md
            if (!Files.exists(caminho) || !caminhoCompletoArquivo.endsWith(".md")) {
                return null;
            }

            // O Java 21 lê todo o texto do arquivo de forma extremamente otimizada
            return Files.readString(caminho);

        } catch (Exception e) {
            System.out.println("Erro crítico ao tentar ler a nota do Obsidian: " + e.getMessage());
            return null;
        }
    }

    /**
     * Utilitário para listar todas as notas de uma pasta (útil para o futuro)
     */
    public static String listarNotasNoVault(String caminhoPastaVault) {
        try (Stream<Path> caminhos = Files.walk(Paths.get(caminhoPastaVault))) {
            return caminhos
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(nome -> nome.endsWith(".md"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "Erro ao mapear a pasta do Obsidian: " + e.getMessage();
        }
    }
    public static String extrairCanvas(String caminhoCompletoArquivo) {
        try {
            Path caminho = Paths.get(caminhoCompletoArquivo);

            if (!Files.exists(caminho) || !caminhoCompletoArquivo.endsWith(".canvas")) {
                return null;
            }

            // Lê o arquivo JSON inteiro
            String conteudoJson = Files.readString(caminho);
            JsonObject canvasData = JsonParser.parseString(conteudoJson).getAsJsonObject();
            JsonArray nodes = canvasData.getAsJsonArray("nodes");

            StringBuilder contextoMapeado = new StringBuilder();
            contextoMapeado.append("--- MAPA MENTAL DE CONTEXTO (CANVAS) ---\n");

            // Percorre todos os cartões do mapa
            for (JsonElement elemento : nodes) {
                JsonObject node = elemento.getAsJsonObject();
                String tipo = node.has("type") ? node.get("type").getAsString() : "";

                // Se o cartão for um bloco de texto, extraímos o conteúdo
                if (tipo.equals("text") && node.has("text")) {
                    contextoMapeado.append("- ").append(node.get("text").getAsString()).append("\n");
                }
            }
            contextoMapeado.append("--- FIM DO MAPA MENTAL ---");

            return contextoMapeado.toString();

        } catch (Exception e) {
            System.out.println("Erro crítico ao tentar decodificar o Canvas: " + e.getMessage());
            return null;
        }
    }
    /**
     * NOVO: Varre uma pasta inteira de forma recursiva, lê todas as notas .md
     * e arquivos .canvas, unificando todo o contexto em uma única String.
     */
    public static String extrairPastaCompleta(String caminhoPasta) {
        File pastaRaiz = new File(caminhoPasta);

        if (!pastaRaiz.exists() || !pastaRaiz.isDirectory()) {
            return null;
        }

        StringBuilder contextoUnificado = new StringBuilder();
        contextoUnificado.append("=== SINCRONIZAÇÃO COMPLETA DO COFRE OBSIDIAN ===\n\n");

        try (Stream<Path> caminhos = Files.walk(Paths.get(caminhoPasta))) {
            // Filtra e processa os arquivos encontrados
            caminhos.filter(Files::isRegularFile).forEach(path -> {
                String caminhoString = path.toString();

                if (caminhoString.endsWith(".md")) {
                    String conteudoNota = extrairNota(caminhoString);
                    if (conteudoNota != null && !conteudoNota.isBlank()) {
                        contextoUnificado.append("--- NOTA: ").append(path.getFileName()).append(" ---\n");
                        contextoUnificado.append(conteudoNota).append("\n\n");
                    }
                } else if (caminhoString.endsWith(".canvas")) {
                    String conteudoCanvas = extrairCanvas(caminhoString);
                    if (conteudoCanvas != null && !conteudoCanvas.isBlank()) {
                        contextoUnificado.append("--- MAPA MENTAL (CANVAS): ").append(path.getFileName()).append(" ---\n");
                        contextoUnificado.append(conteudoCanvas).append("\n\n");
                    }
                }
            });

            contextoUnificado.append("=== FIM DA SINCRONIZAÇÃO DO COFRE ===");
            return contextoUnificado.toString();

        } catch (Exception e) {
            System.out.println("Erro crítico ao varrer a pasta do Obsidian: " + e.getMessage());
            return null;
        }
    }
}