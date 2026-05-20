package com.iacontext;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class LeitorPDF {

    public static String extrairTexto(String caminhoArquivo) {
        File arquivo = new File(caminhoArquivo);

        if (!arquivo.exists()) {
            System.out.println("Erro: O arquivo não foi encontrado no caminho especificado.");
            return null;
        }

        try (PDDocument documento = Loader.loadPDF(arquivo)) {
            PDFTextStripper extrator = new PDFTextStripper();
            return extrator.getText(documento);
        } catch (Exception e) {
            System.out.println("Erro crítico ao tentar ler o PDF: " + e.getMessage());
            return null;
        }
    }
}