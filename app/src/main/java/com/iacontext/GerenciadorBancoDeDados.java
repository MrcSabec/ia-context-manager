package com.iacontext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GerenciadorBancoDeDados {
    // Caminho do arquivo: será criado um arquivo 'memoria.db' na mesma pasta onde o código rodar
    private static final String URL_BANCO = "jdbc:sqlite:memoria.db";

    public GerenciadorBancoDeDados() {
        criarTabelaSeNaoExistir();
    }

    private void criarTabelaSeNaoExistir() {
        // Comando SQL para criar a estrutura da tabela
        String sql = "CREATE TABLE IF NOT EXISTS historico ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "role TEXT NOT NULL, "
                + "texto TEXT NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL_BANCO);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            System.out.println("Erro crítico ao criar o banco de dados: " + e.getMessage());
        }
    }

    public void salvarMensagem(String role, String texto) {
        // Usa PreparedStatement para evitar ataques de SQL Injection e tratar caracteres especiais (como aspas duplas nas histórias)
        String sql = "INSERT INTO historico(role, texto) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL_BANCO);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role);
            pstmt.setString(2, texto);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Erro ao salvar mensagem no disco: " + e.getMessage());
        }
    }

    public List<String[]> carregarHistorico() {
        List<String[]> mensagensPassadas = new ArrayList<>();
        String sql = "SELECT role, texto FROM historico ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(URL_BANCO);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Varre o banco de dados linha por linha
            while (rs.next()) {
                String[] mensagem = new String[2];
                mensagem[0] = rs.getString("role");
                mensagem[1] = rs.getString("texto");
                mensagensPassadas.add(mensagem);
            }
        } catch (Exception e) {
            System.out.println("Erro ao ler o histórico do disco: " + e.getMessage());
        }
        return mensagensPassadas;
    }
}