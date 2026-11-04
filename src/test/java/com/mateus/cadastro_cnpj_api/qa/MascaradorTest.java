package com.mateus.cadastro_cnpj_api.qa;

import com.mateus.cadastro_cnpj_api.qa.execucao.Mascarador;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Nenhum dado sensível pode sair cru nas evidências do QA Center
class MascaradorTest {

    @Test
    void escondeTokenSenhaEDocumentos() {
        String json = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.assinatura\",\"senha\":\"segredo123\","
                + "\"cpf\":\"12345678909\",\"cnpj\":\"11222333000181\",\"email\":\"mateus@empresa.com\",\"telefone\":\"19999998888\"}";

        String m = Mascarador.texto(json);

        assertFalse(m.contains("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0"), "JWT exposto: " + m);
        assertFalse(m.contains("segredo123"), "senha exposta: " + m);
        assertFalse(m.contains("12345678909"), "CPF exposto: " + m);
        assertFalse(m.contains("11222333000181"), "CNPJ exposto: " + m);
        assertFalse(m.contains("mateus@"), "e-mail exposto: " + m);
        assertFalse(m.contains("19999998888"), "telefone exposto: " + m);
        assertTrue(m.contains("\"cpf\":\"123.***.***-09\""), m);
        assertTrue(m.contains("\"cnpj\":\"11.***.***/****-81\""), m);
    }

    @Test
    void jwtSoltoNoTextoTambemEhEscondido() {
        String m = Mascarador.texto("Authorization recebido: eyJabc.eyJdef.ghi fim");
        assertEquals("Authorization recebido: eyJ•••• fim", m);
    }

    @Test
    void headerAuthorizationNuncaMostraOToken() {
        assertEquals("Bearer ••••", Mascarador.autorizacao("Bearer eyJabc.eyJdef.ghi"));
        assertEquals("(sem token)", Mascarador.autorizacao(null));
    }

    @Test
    void textoGrandeEhTruncado() {
        String m = Mascarador.texto("x".repeat(Mascarador.LIMITE + 500));
        assertTrue(m.endsWith("(truncado)"));
        assertTrue(m.length() < Mascarador.LIMITE + 20);
    }
}
