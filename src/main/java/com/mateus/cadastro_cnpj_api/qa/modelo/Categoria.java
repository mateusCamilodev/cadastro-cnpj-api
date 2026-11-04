package com.mateus.cadastro_cnpj_api.qa.modelo;

// Categorias de teste. Uma categoria sem cenários no catálogo aparece desabilitada no front
// (ex.: AUTORIZACAO — a API de negócio não tem perfis, então não há o que testar).
public enum Categoria {
    CONTRATO("Testes de contrato"),
    STATUS_HTTP("Testes de status HTTP"),
    AUTENTICACAO("Testes de autenticação"),
    AUTORIZACAO("Testes de autorização"),
    CAMPOS_OBRIGATORIOS("Testes de campos obrigatórios"),
    DADOS_INVALIDOS("Testes de dados inválidos"),
    INTEGRACAO("Testes de integração"),
    REGRAS_NEGOCIO("Testes de regras de negócio"),
    UPLOAD_PDF("Testes de upload de PDF"),
    VALIDACAO_EMPRESA("Testes de validação da empresa");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() { return rotulo; }
}
