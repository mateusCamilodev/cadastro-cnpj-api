package com.mateus.cadastro_cnpj_api.qa.modelo;

// Ambientes que o QA Center pode testar. Não existe "produção": testes que criam dados
// nunca podem ser apontados para lá.
public enum Ambiente {
    DESENVOLVIMENTO("Desenvolvimento", "qa.ambientes.desenvolvimento.url"),
    HOMOLOGACAO("Homologação", "qa.ambientes.homologacao.url");

    private final String rotulo;
    private final String propriedadeUrl;

    Ambiente(String rotulo, String propriedadeUrl) {
        this.rotulo = rotulo;
        this.propriedadeUrl = propriedadeUrl;
    }

    public String rotulo() { return rotulo; }
    public String propriedadeUrl() { return propriedadeUrl; }
}
