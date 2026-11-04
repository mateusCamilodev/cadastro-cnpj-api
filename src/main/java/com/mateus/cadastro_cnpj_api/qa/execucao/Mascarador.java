package com.mateus.cadastro_cnpj_api.qa.execucao;

import java.util.regex.Pattern;

// Esconde dados sensíveis antes de qualquer evidência ser salva ou mostrada no front:
// tokens (JWT e accessToken), senhas, CPF, CNPJ, e-mail e telefone. Também limita o tamanho.
public final class Mascarador {

    public static final int LIMITE = 3000;

    private static final Pattern JWT = Pattern.compile("eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+");
    private static final Pattern TOKEN = Pattern.compile("(\"(?:accessToken|token)\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern SENHA = Pattern.compile("(\"senha\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern CPF = Pattern.compile("(\"cpf\"\\s*:\\s*\")(\\d{3})\\d*(\\d{2})(\")");
    private static final Pattern CNPJ = Pattern.compile("(\"cnpj\"\\s*:\\s*\")(\\d{2})\\d*(\\d{2})(\")");
    private static final Pattern EMAIL = Pattern.compile("(\"email\"\\s*:\\s*\")([^\"@])[^\"@]*(@[^\"]*\")");
    private static final Pattern TELEFONE = Pattern.compile("(\"telefone\"\\s*:\\s*\")\\d*(\\d{4})(\")");

    private Mascarador() {
    }

    public static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String m = JWT.matcher(valor).replaceAll("eyJ••••");
        m = TOKEN.matcher(m).replaceAll("$1••••$2");
        m = SENHA.matcher(m).replaceAll("$1••••$2");
        m = CPF.matcher(m).replaceAll("$1$2.***.***-$3$4");
        m = CNPJ.matcher(m).replaceAll("$1$2.***.***/****-$3$4");
        m = EMAIL.matcher(m).replaceAll("$1$2***$3");
        m = TELEFONE.matcher(m).replaceAll("$1*******$2$3");
        return m.length() > LIMITE ? m.substring(0, LIMITE) + "… (truncado)" : m;
    }

    // Header Authorization: nunca mostra o token
    public static String autorizacao(String valor) {
        if (valor == null || valor.isBlank()) {
            return "(sem token)";
        }
        return valor.startsWith("Bearer ") ? "Bearer ••••" : "••••";
    }
}
