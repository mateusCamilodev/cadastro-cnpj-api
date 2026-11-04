package com.mateus.cadastro_cnpj_api.qa.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Configurações do executor alteráveis pela tela (permissão qa.configure). Registro único (id = 1).
// Os limites máximos ficam no código/properties: a tela só ajusta dentro deles.
@Entity
@Table(name = "qa_configuracoes")
public class QaConfiguracao {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    private int timeoutRequisicaoSegundos;
    private int timeoutExecucaoMinutos;
    private int cenariosParalelos;
    private int execucoesPorUsuario;

    @Column(length = 150)
    private String atualizadaPor;

    private LocalDateTime atualizadaEm;

    protected QaConfiguracao() {
    }

    public QaConfiguracao(int timeoutRequisicaoSegundos, int timeoutExecucaoMinutos, int cenariosParalelos,
                          int execucoesPorUsuario, String atualizadaPor) {
        alterar(timeoutRequisicaoSegundos, timeoutExecucaoMinutos, cenariosParalelos, execucoesPorUsuario, atualizadaPor);
    }

    public void alterar(int timeoutRequisicaoSegundos, int timeoutExecucaoMinutos, int cenariosParalelos,
                        int execucoesPorUsuario, String usuario) {
        this.timeoutRequisicaoSegundos = timeoutRequisicaoSegundos;
        this.timeoutExecucaoMinutos = timeoutExecucaoMinutos;
        this.cenariosParalelos = cenariosParalelos;
        this.execucoesPorUsuario = execucoesPorUsuario;
        this.atualizadaPor = usuario;
        this.atualizadaEm = LocalDateTime.now();
    }

    public int getTimeoutRequisicaoSegundos() { return timeoutRequisicaoSegundos; }
    public int getTimeoutExecucaoMinutos() { return timeoutExecucaoMinutos; }
    public int getCenariosParalelos() { return cenariosParalelos; }
    public int getExecucoesPorUsuario() { return execucoesPorUsuario; }
    public String getAtualizadaPor() { return atualizadaPor; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
