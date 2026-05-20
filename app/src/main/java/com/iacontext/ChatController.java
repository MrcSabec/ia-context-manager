package com.iacontext;

import org.springframework.web.bind.annotation.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ClienteIA inteligenciaArtificial = new GeminiService();
    private final Gson gson = new Gson();

    // --- MINI STRUCTS PARA A WEB ---
    public record MensagemRequest(String texto) {}
    public record MensagemResponse(String texto) {}
    public record RegistroHistorico(String role, String texto) {}

    // 1. ROTA: Lista todos os chats locais (arquivos .db)
    @GetMapping("/chats")
    public List<String> listarChats() {
        List<String> nomesChats = new ArrayList<>();
        File diretorioAtual = new File(".");
        // Filtra arquivos que terminam com .db
        File[] arquivos = diretorioAtual.listFiles((dir, nome) -> nome.endsWith(".db"));

        if (arquivos != null) {
            for (File arquivo : arquivos) {
                // Adiciona na lista removendo a extensão .db
                nomesChats.add(arquivo.getName().replace(".db", ""));
            }
        }
        return nomesChats;
    }

    // 2. ROTA: Carrega o histórico completo de um chat específico
    @GetMapping("/chat-historico/{nomeSessao}")
    public List<RegistroHistorico> carregarHistoricoSessao(@PathVariable String nomeSessao) {
        // Instancia o banco dinamicamente para ler
        GerenciadorBancoDeDados db = new GerenciadorBancoDeDados(nomeSessao);
        List<String[]> mensagensSalvas = db.carregarHistorico();

        List<RegistroHistorico> historicoParaEnvio = new ArrayList<>();
        for (String[] msg : mensagensSalvas) {
            historicoParaEnvio.add(new RegistroHistorico(msg[0], msg[1]));
        }
        return historicoParaEnvio;
    }

    // 3. ROTA: Processa mensagem de um chat específico (URL Dinâmica)
    @PostMapping("/chat/{nomeSessao}")
    public MensagemResponse processarMensagem(@PathVariable String nomeSessao, @RequestBody MensagemRequest request) {
        String entradaUsuario = request.texto();

        // Instancia o banco dinamicamente com base na URL
        GerenciadorBancoDeDados db = new GerenciadorBancoDeDados(nomeSessao);

        // Reconstrói a janela de contexto completa na memória RAM antes de enviar
        List<App.Conteudo> historicoContexto = new ArrayList<>();
        List<String[]> mensagensSalvas = db.carregarHistorico();
        // --- TRATAMENTO DE SINCRONIZAÇÃO DE PASTA COMPLETA ---
        if (entradaUsuario.startsWith("/obsidian ")) {
            String caminhoPasta = entradaUsuario.substring(10).trim();
            System.out.println("Iniciando varredura em lote da pasta: " + caminhoPasta);

            String contextoCofre = LeitorObsidian.extrairPastaCompleta(caminhoPasta);

            if (contextoCofre != null) {
                String diretriz = "[DIRETRIZ DE SISTEMA/CONHECIMENTO ABSOLUTO]: Você está recebendo a base de dados completa do Obsidian do usuário. Use todas essas informações unificadas para guiar suas próximas respostas: \n\n" + contextoCofre;

                historicoContexto.add(new App.Conteudo("user", List.of(new App.Parte(diretriz))));
                db.salvarMensagem("user", diretriz);

                return new MensagemResponse("✨ **Omnisciência Ativada!** O ÉDEN varreu a pasta do seu Obsidian e absorveu todas as notas `.md` e mapas `.canvas` simultaneamente.");
            }
            return new MensagemResponse("**Erro:** Não foi possível acessar a pasta especificada. Verifique se o caminho está correto no Ubuntu.");
        }
        if (entradaUsuario.startsWith("/canvas ")) {
            String caminhoCanvas = entradaUsuario.substring(8).trim();
            String textoCanvas = LeitorObsidian.extrairCanvas(caminhoCanvas);

            if (textoCanvas != null) {
                String diretriz = "[DIRETRIZ DE SISTEMA/OBSIDIAN CANVAS]: O usuário compartilhou dados de um mapa mental. Memorize estes fragmentos de conhecimento: \n\n" + textoCanvas;

                historicoContexto.add(new App.Conteudo("user", List.of(new App.Parte(diretriz))));
                db.salvarMensagem("user", diretriz);

                return new MensagemResponse("Mapa mental sincronizado! O ÉDEN absorveu a estrutura visual do seu Canvas.");
            }
            return new MensagemResponse("**Erro:** Não foi possível localizar ou interpretar o arquivo `.canvas`.");
        }
        if (entradaUsuario.startsWith("/nota ")) {
            String caminhoNota = entradaUsuario.substring(6).trim();
            String textoNota = LeitorObsidian.extrairNota(caminhoNota);

            if (textoNota != null) {
                String diretriz = "[DIRETRIZ DE SISTEMA/OBSIDIAN]: O usuário compartilhou uma anotação do seu Cofre local. Memorize e use como contexto primário: \n\n" + textoNota;

                historicoContexto.add(new App.Conteudo("user", List.of(new App.Parte(diretriz))));
                db.salvarMensagem("user", diretriz);

                return new MensagemResponse("Nota do Obsidian sincronizada com sucesso ao núcleo do ÉDEN!");
            }
            return new MensagemResponse("**Erro:** Não foi possível localizar ou ler a nota. Verifique se o caminho no Ubuntu está correto e termina em `.md`.");
        }
        for (String[] msg : mensagensSalvas) {
            historicoContexto.add(new App.Conteudo(msg[0], List.of(new App.Parte(msg[1]))));
        }

        // --- TRATAMENTO DE PDF (Suporte mantido) ---
        if (entradaUsuario.startsWith("/pdf ")) {
            String caminho = entradaUsuario.substring(5).trim();
            String textoPdf = LeitorPDF.extrairTexto(caminho);
            if (textoPdf != null) {
                String diretriz = "[DIRETRIZ DE SISTEMA]: Leia e use o contexto a seguir como base: " + textoPdf;
                historicoContexto.add(new App.Conteudo("user", List.of(new App.Parte(diretriz))));
                db.salvarMensagem("user", diretriz);
                return new MensagemResponse("Mestre, documento PDF integrado à memória do **" + nomeSessao.toUpperCase() + "** com sucesso!");
            }
            return new MensagemResponse("Erro crítico: Não foi possível ler o arquivo PDF no caminho especificado.");
        }

        // 1. Salva e injeta no contexto
        historicoContexto.add(new App.Conteudo("user", List.of(new App.Parte(entradaUsuario))));
        db.salvarMensagem("user", entradaUsuario);

        // 2. Envia para o Gemini
        App.RequisicaoGemini requisicao = new App.RequisicaoGemini(historicoContexto);
        String jsonParaEnviar = gson.toJson(requisicao);
        String respostaBrutaJSON = inteligenciaArtificial.enviarRequisicao(jsonParaEnviar);

        // 3. Trata e Devolve para o Frontend (Markdown mantido)
        try {
            JsonObject jsonObjeto = JsonParser.parseString(respostaBrutaJSON).getAsJsonObject();
            String textoRespostaIA = jsonObjeto.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();


            db.salvarMensagem("model", textoRespostaIA);

            return new MensagemResponse(textoRespostaIA);

        } catch (Exception e) {
            return new MensagemResponse("**Erro de Comunicação:** Os servidores do Google estão sobrecarregados ou responderam em um formato inválido. Tente novamente em alguns segundos.");
        }
    }
}