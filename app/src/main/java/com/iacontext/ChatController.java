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

    private final OllamaService inteligenciaArtificial = new OllamaService();
    private final Gson gson = new Gson();

    public record MensagemRequest(String texto) {}
    public record MensagemResponse(String texto) {}
    public record RegistroHistorico(String role, String texto) {}


    @GetMapping("/chats")
    public List<String> listarChats() {
        List<String> nomesChats = new ArrayList<>();
        File diretorioAtual = new File(".");
        File[] arquivos = diretorioAtual.listFiles((dir, nome) -> nome.endsWith(".db"));

        if (arquivos != null) {
            for (File arquivo : arquivos) {
                nomesChats.add(arquivo.getName().replace(".db", ""));
            }
        }
        return nomesChats;
    }


    @GetMapping("/chat-historico/{nomeSessao}")
    public List<RegistroHistorico> carregarHistoricoSessao(@PathVariable String nomeSessao) {
        GerenciadorBancoDeDados db = new GerenciadorBancoDeDados(nomeSessao);
        List<String[]> mensagensSalvas = db.carregarHistoricoCompleto();

        List<RegistroHistorico> historicoParaEnvio = new ArrayList<>();
        for (String[] msg : mensagensSalvas) {
            if (!msg[2].equals("documento")) {
                historicoParaEnvio.add(new RegistroHistorico(msg[0], msg[1]));
            }
        }
        return historicoParaEnvio;
    }

    @PostMapping("/chat/{nomeSessao}")
    public MensagemResponse processarMensagem(@PathVariable String nomeSessao, @RequestBody MensagemRequest request) {
        String entradaUsuario = request.texto();
        GerenciadorBancoDeDados db = new GerenciadorBancoDeDados(nomeSessao);

        if (entradaUsuario.startsWith("/obsidian ")) {
            String caminhoPasta = entradaUsuario.substring(10).trim();
            System.out.println("Iniciando varredura em lote da pasta: " + caminhoPasta);
            String contextoCofre = LeitorObsidian.extrairPastaCompleta(caminhoPasta);

            if (contextoCofre != null) {
                db.salvarMensagem("user", contextoCofre, "documento");
                return new MensagemResponse("✨ **Omnisciência Ativada!** Base de dados do Obsidian indexada localmente no cluster do ÉDEN.");
            }
            return new MensagemResponse("**Erro:** Não foi possível acessar a pasta especificada no Ubuntu.");
        }

        if (entradaUsuario.startsWith("/canvas ")) {
            String caminhoCanvas = entradaUsuario.substring(8).trim();
            String textoCanvas = LeitorObsidian.extrairCanvas(caminhoCanvas);

            if (textoCanvas != null) {
                db.salvarMensagem("user", textoCanvas, "documento");
                return new MensagemResponse("Mapa mental sincronizado com sucesso como conhecimento frio.");
            }
            return new MensagemResponse("**Erro:** Não foi possível ler o arquivo `.canvas`.");
        }

        if (entradaUsuario.startsWith("/nota ")) {
            String caminhoNota = entradaUsuario.substring(6).trim();
            String textoNota = LeitorObsidian.extrairNota(caminhoNota);

            if (textoNota != null) {
                db.salvarMensagem("user", textoNota, "documento");
                return new MensagemResponse("Nota isolada adicionada à biblioteca de consulta.");
            }
            return new MensagemResponse("**Erro:** Arquivo `.md` não localizado.");
        }

        if (entradaUsuario.startsWith("/pdf ")) {
            String caminho = entradaUsuario.substring(5).trim();
            String textoPdf = LeitorPDF.extrairTexto(caminho);
            if (textoPdf != null) {
                db.salvarMensagem("user", textoPdf, "documento");
                return new MensagemResponse("Documento PDF integrado com sucesso.");
            }
            return new MensagemResponse("Erro crítico ao ler o arquivo PDF.");
        }


        db.salvarMensagem("user", entradaUsuario, "chat");


        boolean modoCriativo = false;
        String mensagemProcessada = entradaUsuario;

        if (entradaUsuario.toLowerCase().startsWith("/criar ")) {
            modoCriativo = true;
            mensagemProcessada = entradaUsuario.substring(7).trim();
        }

        String[] palavras = mensagemProcessada.toLowerCase().split("\\s+");
        List<String> termosChave = new ArrayList<>();
        for (String p : palavras) {
            String limpa = p.replaceAll("[^a-zA-Z0-9áéíóúâêôãõç]", "");
            if (limpa.length() > 3) { // Ignora conectivos curtos
                termosChave.add(limpa);
            }
        }

        List<String> todosDocumentos = db.carregarDocumentos();
        StringBuilder contextoDocumentalRelevante = new StringBuilder();

        for (String doc : todosDocumentos) {
            boolean contemTermo = false;
            String docMinusculo = doc.toLowerCase();
            for (String termo : termosChave) {
                if (docMinusculo.contains(termo)) {
                    contemTermo = true;
                    break;
                }
            }
            if (contemTermo) {
                contextoDocumentalRelevante.append(doc).append("\n\n");
            }
        }


        List<String[]> historicoChatRecente = db.carregarJanelaDeslizante(6);


        StringBuilder payloadContextoFocalizado = new StringBuilder();

        if (contextoDocumentalRelevante.length() > 0) {
            payloadContextoFocalizado.append("[CONHECIMENTO EXTRAÍDO DOS DOCUMENTOS EM DISCO]:\n")
                    .append(contextoDocumentalRelevante)
                    .append("--------------------------------------------------\n\n");
        }

        payloadContextoFocalizado.append("[HISTÓRICO RECENTE DA CONVERSA]:\n");
        for (String[] msg : historicoChatRecente) {
            String papel = msg[0].equals("model") ? "ÉDEN" : "Usuário";
            if (!msg[1].equals(entradaUsuario)) {
                payloadContextoFocalizado.append("[").append(papel).append("]: ").append(msg[1]).append("\n\n");
            }
        }


        System.out.println("================ PACOTE ENVIADO PARA A IA ================");
        System.out.println("MODO CRIATIVO ATIVADO: " + modoCriativo);
        System.out.println(payloadContextoFocalizado.toString());
        System.out.println("==========================================================");

        try {
            String respostaIA = inteligenciaArtificial.sendMessage(mensagemProcessada, payloadContextoFocalizado.toString(), modoCriativo);

            if (respostaIA.startsWith("Erro Crítico") || respostaIA.startsWith("Falha na comunicação")) {
                return new MensagemResponse("⚠️ **" + respostaIA + "**\n\nVerifique o terminal do seu Ubuntu.");
            }

            db.salvarMensagem("model", respostaIA, "chat");
            return new MensagemResponse(respostaIA);

        } catch (Exception e) {
            e.printStackTrace();
            return new MensagemResponse("💥 **Erro no processamento do fluxo:** " + e.getMessage());
        }
    }
}