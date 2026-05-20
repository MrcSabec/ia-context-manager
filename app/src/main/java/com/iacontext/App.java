package com.iacontext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.List;

@SpringBootApplication
public class App {

    public record Parte(String text) {}
    public record Conteudo(String role, List<Parte> parts) {}
    public record RequisicaoGemini(List<Conteudo> contents) {}

    public static void main(String[] args) {

        SpringApplication.run(App.class, args);

        System.out.println("\n=======================================================");
        System.out.println("🚀 Motor Web Iniciado com Sucesso!");
        System.out.println("🌐 Abra o seu navegador e acesse: http://localhost:8080");
        System.out.println("=======================================================\n");
    }
}