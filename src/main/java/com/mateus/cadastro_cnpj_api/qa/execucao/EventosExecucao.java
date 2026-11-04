package com.mateus.cadastro_cnpj_api.qa.execucao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

// Tempo real via SSE (Server-Sent Events). Ao conectar, o navegador recebe um "snapshot" completo
// (assim um F5 no meio da execução recupera tudo) e depois cada mudança como evento.
@Component
public class EventosExecucao {

    private static final Logger log = LoggerFactory.getLogger(EventosExecucao.class);
    private static final long TEMPO_MAXIMO = Duration.ofMinutes(30).toMillis();

    private final Map<UUID, List<SseEmitter>> inscritos = new ConcurrentHashMap<>();

    public SseEmitter inscrever(UUID execucaoId, Supplier<Object> snapshot, boolean jaTerminou) {
        SseEmitter emitter = new SseEmitter(TEMPO_MAXIMO);
        List<SseEmitter> lista = inscritos.computeIfAbsent(execucaoId, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);
        Runnable remover = () -> lista.remove(emitter);
        emitter.onCompletion(remover);
        emitter.onTimeout(remover);
        emitter.onError(e -> remover.run());

        enviar(emitter, "snapshot", snapshot.get());
        if (jaTerminou) {
            enviar(emitter, "fim", Map.of("execucaoId", execucaoId));
            emitter.complete();
        }
        return emitter;
    }

    public void publicar(UUID execucaoId, String tipo, Object dados) {
        for (SseEmitter emitter : inscritos.getOrDefault(execucaoId, List.of())) {
            enviar(emitter, tipo, dados);
        }
    }

    public void encerrar(UUID execucaoId, Object dadosFinais) {
        List<SseEmitter> lista = inscritos.remove(execucaoId);
        if (lista == null) return;
        for (SseEmitter emitter : lista) {
            enviar(emitter, "fim", dadosFinais);
            emitter.complete();
        }
    }

    private void enviar(SseEmitter emitter, String tipo, Object dados) {
        try {
            emitter.send(SseEmitter.event().name(tipo).data(dados));
        } catch (IOException | IllegalStateException e) {
            // navegador fechou a aba: só para de mandar para ele
            log.debug("Inscrito SSE desconectado: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
