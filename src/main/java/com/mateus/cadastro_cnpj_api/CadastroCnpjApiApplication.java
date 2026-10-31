package com.mateus.cadastro_cnpj_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // liga o agendador (ProcessamentoAgendado)
public class CadastroCnpjApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CadastroCnpjApiApplication.class, args);
	}

}
