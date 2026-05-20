package com.iacontext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GerenciadorBancoDeDados {

    private final String urlBanco;

    public GerenciadorBancoDeDados(String nomeChat) {
        String nomeSanitizado = nomeChat.toLowerCase().replaceAll("[^a-z0-9]", "_");
        this.urlBanco = "jdbc:sqlite:" + nomeSanitizado + ".db";

        criarTabelaSeNaoExistir();
        atualizarTabelaLegada();
    }

    private void criarTabelaSeNaoExistir() {
        String sql = "CREATE TABLE IF NOT EXISTS historico ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "role TEXT NOT NULL, "
                + "texto TEXT NOT NULL, "
                + "categoria TEXT DEFAULT 'chat' NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(this.urlBanco);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            System.out.println("Erro crítico ao criar o banco de dados: " + e.getMessage());
        }
    }

    private void atualizarTabelaLegada() {
        String sql = "ALTER TABLE historico ADD COLUMN categoria TEXT DEFAULT 'chat' NOT NULL;";
        try (Connection conn = DriverManager.getConnection(this.urlBanco);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e){
        }
    }

    public void salvarMensagem(String role, String texto, String categoria) {
        String sql = "INSERT INTO historico(role, texto, categoria) VALUES(?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(this.urlBanco);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role);
            pstmt.setString(2, texto);
            pstmt.setString(3, categoria);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao salvar mensagem no disco: " + e.getMessage());
        }
    }

    // 1. Método para a Interface Gráfica (Carrega tudo para desenhar a tela)
    public List<String[]> carregarHistoricoCompleto() {
        List<String[]> mensagens = new ArrayList<>();
        String sql = "SELECT role, texto, categoria FROM historico ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(this.urlBanco);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mensagens.add(new String[]{rs.getString("role"), rs.getString("texto"), rs.getString("categoria")});
            }
        } catch (Exception e) {
            System.out.println("Erro ao ler o histórico completo: " + e.getMessage());
        }
        return mensagens;
    }

    public List<String[]> carregarJanelaDeslizante(int limiteDeMensagens) {
        List<String[]> mensagens = new ArrayList<>();
        String sql = "SELECT role, texto FROM historico WHERE categoria = 'chat' ORDER BY id DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(this.urlBanco);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limiteDeMensagens);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mensagens.add(new String[]{rs.getString("role"), rs.getString("texto")});
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar a janela deslizante: " + e.getMessage());
        }

        // Inverte a lista para restaurar a ordem cronológica antes de enviar para a IA
        Collections.reverse(mensagens);
        return mensagens;
    }

    public List<String> carregarDocumentos() {
        List<String> documentos = new ArrayList<>();
        String sql = "SELECT texto FROM historico WHERE categoria = 'documento' ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(this.urlBanco);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                documentos.add(rs.getString("texto"));
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar os documentos: " + e.getMessage());
        }
        return documentos;
    }
}