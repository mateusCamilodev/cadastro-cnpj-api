package com.mateus.cadastro_cnpj_api.service;

import com.mateus.cadastro_cnpj_api.exception.ArquivoInvalidoException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

// Lê o texto do PDF e transforma as linhas "Campo: valor" em um Map
// Ex.: "Razão Social: Empresa Teste LTDA" -> chave "razaosocial", valor "Empresa Teste LTDA"
@Component
public class LeitorDocumentoPdf {

    public Map<String, String> ler(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("O arquivo enviado está vazio");
        }

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado");
        }

        // Confere pelo conteúdo (todo PDF começa com "%PDF"), não pela extensão do nome
        if (!ehPdf(bytes)) {
            throw new ArquivoInvalidoException("O arquivo enviado não é um PDF");
        }

        String texto;
        try (PDDocument documento = Loader.loadPDF(bytes)) {
            texto = new PDFTextStripper().getText(documento);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("PDF corrompido ou protegido por senha");
        }

        if (texto.isBlank()) {
            throw new ArquivoInvalidoException("O PDF não tem texto legível (PDF escaneado/imagem não é suportado)");
        }

        Map<String, String> campos = new HashMap<>();
        for (String linha : texto.split("\\R")) {
            int doisPontos = linha.indexOf(':');
            if (doisPontos <= 0) {
                continue; // linha sem "Campo:" é ignorada (ex.: título do documento)
            }
            String rotulo = normalizarRotulo(linha.substring(0, doisPontos));
            String valor = linha.substring(doisPontos + 1).trim();
            campos.putIfAbsent(rotulo, valor);
        }
        return campos;
    }

    private boolean ehPdf(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    // "Data de Nascimento" -> "datadenascimento" | "E-mail" -> "email" | "Razão Social" -> "razaosocial"
    private String normalizarRotulo(String rotulo) {
        String semAcento = Normalizer.normalize(rotulo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().replaceAll("[^a-z]", "");
    }
}
