import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import { useAgora } from '../../hooks.js'
import Icone from '../../components/Icone.jsx'
import {
  CabecalhoQa, Dialogo, ErroCarga, Esqueleto, MetodoBadge, PainelLateral, QaStatus, dataHoraCurta, duracao,
} from '../../components/qa/QaComum.jsx'
import { CORES_STATUS } from '../../components/qa/QaGraficos.jsx'

const ATIVOS = ['PENDING', 'RUNNING']
const FILTROS = [
  { id: '', label: 'Todos' },
  { id: 'FAILED', label: 'Reprovados' },
  { id: 'ERROR', label: 'Erros' },
  { id: 'SKIPPED', label: 'Bloqueados' },
  { id: 'CANCELLED', label: 'Cancelados' },
  { id: 'PASSED', label: 'Aprovados' },
  { id: 'ATIVOS', label: 'Em andamento' },
]

// Acompanhamento ao vivo de uma execução. O estado vem do SSE (snapshot ao conectar + eventos);
// por isso um F5 no meio recupera tudo. Quando o stream não está disponível, cai para consulta periódica.
export default function QaExecucao() {
  const { id } = useParams()
  const { pode } = useAuth()
  const navegar = useNavigate()
  const [execucao, setExecucao] = useState(null)
  const [resultados, setResultados] = useState([])
  const [eventos, setEventos] = useState([])
  const [erro, setErro] = useState('')
  const [conexao, setConexao] = useState('conectando')
  const [filtro, setFiltro] = useState('')
  const [aberto, setAberto] = useState(null)
  const [confirmarCancelar, setConfirmarCancelar] = useState(false)
  const [acao, setAcao] = useState('')
  const [erroAcao, setErroAcao] = useState('')

  const aplicarDetalhe = useCallback((d) => {
    setExecucao(d.execucao)
    setResultados(d.resultados)
    setEventos(d.eventos)
  }, [])

  useEffect(() => {
    setExecucao(null)
    setErro('')
    setConexao('conectando')
    let parar = () => {}
    let ativo = true
    // Primeiro uma consulta normal (mostra 404 com mensagem clara), depois o tempo real
    qa.execucao(id).then((d) => {
      if (!ativo) return
      aplicarDetalhe(d)
      parar = qa.ouvirEventos(id, (tipo, dados) => {
        setConexao('ao-vivo')
        if (tipo === 'snapshot') aplicarDetalhe(dados)
        else if (tipo === 'execucao') setExecucao(dados)
        else if (tipo === 'resultado') {
          setResultados((lista) => {
            const i = lista.findIndex((r) => r.id === dados.id)
            if (i < 0) return [...lista, dados]
            const nova = [...lista]
            nova[i] = dados
            return nova
          })
        } else if (tipo === 'log') setEventos((lista) => [...lista, dados])
        else if (tipo === 'fim') {
          setConexao('encerrada')
          // Busca o estado final consolidado (inclui os logs de encerramento)
          qa.execucao(id).then((final) => ativo && aplicarDetalhe(final)).catch(() => {})
        }
      }, () => setConexao('reconectando'))
    }).catch((e) => ativo && setErro(e.status === 404 ? 'Execução não encontrada (o histórico fica em memória e zera quando a API reinicia).' : e.message))
    return () => { ativo = false; parar() }
  }, [id, aplicarDetalhe])

  const rodando = execucao && ATIVOS.includes(execucao.status)
  const agora = useAgora(rodando ? 1000 : 60000)
  const decorrido = execucao?.iniciadaEm
    ? (execucao.finalizadaEm ? execucao.duracaoMs : agora - new Date(execucao.iniciadaEm).getTime())
    : null
  const progresso = execucao?.total ? Math.round((execucao.concluidos / execucao.total) * 100) : 0

  const visiveis = useMemo(() => resultados.filter((r) =>
    !filtro || (filtro === 'ATIVOS' ? ATIVOS.includes(r.status) : r.status === filtro)), [resultados, filtro])

  async function cancelar() {
    setAcao('cancelar')
    setErroAcao('')
    try {
      setExecucao(await qa.cancelar(id))
      setConfirmarCancelar(false)
    } catch (e) {
      setErroAcao(e.message)
    } finally {
      setAcao('')
    }
  }

  async function reexecutar() {
    setAcao('reexecutar')
    setErroAcao('')
    try {
      const nova = await qa.reexecutar(id)
      navegar(`/qa/execucoes/${nova.id}`)
    } catch (e) {
      setErroAcao(e.message)
    } finally {
      setAcao('')
    }
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo={execucao ? `Execução #${id.slice(0, 8)}` : 'Execução'}
                   descricao={execucao ? `${execucao.suiteNome ? `Suíte ${execucao.suiteNome} · ` : ''}${execucao.ambienteRotulo} · por ${execucao.usuario} · criada ${dataHoraCurta(execucao.criadaEm)}${execucao.canceladaPor ? ` · cancelada por ${execucao.canceladaPor}` : ''}` : null}>
        {rodando && pode('qa.cancel') && (
          <button type="button" className="botao botao-secundario" onClick={() => setConfirmarCancelar(true)}>
            <Icone nome="x" tamanho={16} />Cancelar execução
          </button>
        )}
        {execucao && !rodando && pode('qa.execute') && (
          <button type="button" className="botao botao-primario" onClick={reexecutar} disabled={acao === 'reexecutar'}>
            <Icone nome="refazer" tamanho={16} />{acao === 'reexecutar' ? 'Iniciando…' : 'Reexecutar'}
          </button>
        )}
      </CabecalhoQa>

      {erro && <ErroCarga erro={erro} />}
      {erroAcao && <div className="mensagem mensagem-erro" role="alert"><Icone nome="alerta" />{erroAcao}</div>}
      {!execucao && !erro && <div className="painel qa-bloco"><Esqueleto linhas={8} /></div>}

      {execucao && (
        <>
          <section className="painel qa-bloco qa-progresso-bloco">
            <div className="qa-progresso-topo">
              <QaStatus status={execucao.status} tipo="execucao" />
              <span className={`qa-conexao qa-conexao-${conexao}`}>
                {conexao === 'ao-vivo' && rodando ? 'Ao vivo' : conexao === 'reconectando' ? 'Reconectando…' : conexao === 'conectando' && rodando ? 'Conectando…' : 'Finalizada'}
              </span>
              <span className="qa-progresso-tempo tabular">
                <Icone nome="relogio" tamanho={15} />{decorrido === null ? 'aguardando vaga' : duracao(Math.max(0, decorrido))}
              </span>
              {execucao.origemId && (
                <Link className="qa-link" to={`/qa/execucoes/${execucao.origemId}`}>Reexecução de #{execucao.origemId.slice(0, 8)}</Link>
              )}
            </div>

            <div className="qa-progresso" role="progressbar" aria-valuemin={0} aria-valuemax={execucao.total} aria-valuenow={execucao.concluidos}
                 aria-label="Progresso da execução">
              {CORES_STATUS.filter((c) => execucao[c.chave] > 0).map((c) => (
                <span key={c.chave} style={{ flex: execucao[c.chave], background: c.cor }} />
              ))}
              {execucao.total - execucao.concluidos > 0 && <span className="qa-progresso-resto" style={{ flex: execucao.total - execucao.concluidos }} />}
            </div>
            <div className="qa-contadores">
              <span><b className="tabular">{progresso}%</b> concluído · {execucao.concluidos} de {execucao.total}</span>
              {CORES_STATUS.map((c) => (
                <span key={c.chave}><span className="grafico-legenda-cor" style={{ background: c.cor }} />{c.rotulo} <b className="tabular">{execucao[c.chave]}</b></span>
              ))}
            </div>
            {execucao.mensagem && <p className="qa-execucao-msg">{execucao.mensagem}</p>}
          </section>

          <div className="qa-grade-execucao">
            <section className="painel qa-bloco">
              <header className="qa-bloco-topo">
                <h2>Cenários</h2>
                <div className="qa-chips" role="group" aria-label="Filtrar resultados">
                  {FILTROS.map((f) => {
                    const n = f.id === '' ? resultados.length
                      : f.id === 'ATIVOS' ? resultados.filter((r) => ATIVOS.includes(r.status)).length
                        : resultados.filter((r) => r.status === f.id).length
                    return (
                      <button key={f.id} type="button" className={`qa-chip${filtro === f.id ? ' ativo' : ''}`} aria-pressed={filtro === f.id}
                              onClick={() => setFiltro(f.id)}>{f.label} <span className="tabular">{n}</span></button>
                    )
                  })}
                </div>
              </header>
              <div className="qa-tabela-rolagem">
                <table className="qa-tabela">
                  <thead>
                    <tr><th>Cenário</th><th>Endpoint</th><th>Status</th><th className="num">HTTP</th><th className="num">Tempo</th></tr>
                  </thead>
                  <tbody>
                    {visiveis.map((r) => (
                      <tr key={r.id} className={`qa-linha qa-linha-${r.status.toLowerCase()}`} tabIndex={0}
                          onClick={() => setAberto(r)} onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), setAberto(r))}>
                        <td>
                          <span className="mono qa-id">{r.cenarioId}</span>
                          <span className="qa-cenario-nome">{r.cenarioNome}</span>
                          {r.mensagem && r.status !== 'PASSED' && <span className="qa-cenario-msg">{r.mensagem}</span>}
                        </td>
                        <td><MetodoBadge metodo={r.metodo} /> <code className="qa-caminho">{r.caminho}</code></td>
                        <td><QaStatus status={r.status} /></td>
                        <td className="num tabular">{r.httpStatus ?? '—'}</td>
                        <td className="num tabular">{duracao(r.duracaoMs)}</td>
                      </tr>
                    ))}
                    {!visiveis.length && <tr><td colSpan={5} className="qa-vazio-curto">Nenhum cenário neste filtro.</td></tr>}
                  </tbody>
                </table>
              </div>
            </section>

            <section className="painel qa-bloco qa-log">
              <header className="qa-bloco-topo"><h2>Log</h2><span>{eventos.length} evento(s)</span></header>
              <ol className="qa-log-lista" aria-live="polite">
                {[...eventos].reverse().map((ev, i) => (
                  <li key={eventos.length - i} className={`qa-log-${ev.tipo.toLowerCase()}`}>
                    <time className="tabular">{new Date(ev.dataHora).toLocaleTimeString('pt-BR')}</time>
                    <span>{ev.mensagem}</span>
                  </li>
                ))}
              </ol>
            </section>
          </div>
        </>
      )}

      <DetalheTeste resultado={aberto} execucaoId={id} aoFechar={() => setAberto(null)} />

      <Dialogo aberto={confirmarCancelar} titulo="Cancelar execução?" aoFechar={() => acao !== 'cancelar' && setConfirmarCancelar(false)}
               acoes={<>
                 <button type="button" className="botao botao-secundario" onClick={() => setConfirmarCancelar(false)} disabled={acao === 'cancelar'}>Continuar rodando</button>
                 <button type="button" className="botao botao-primario" onClick={cancelar} disabled={acao === 'cancelar'}>
                   {acao === 'cancelar' ? 'Cancelando…' : 'Cancelar execução'}
                 </button>
               </>}>
        <p>Os cenários que já começaram terminam normalmente; os que ainda estão na fila serão marcados como <strong>pulados</strong>. Os resultados já obtidos ficam salvos.</p>
      </Dialogo>
    </div>
  )
}

// Painel lateral com as evidências do teste (requisição/resposta já mascaradas pelo backend)
function DetalheTeste({ resultado, execucaoId, aoFechar }) {
  const [completo, setCompleto] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!resultado) return
    setCompleto(null)
    setErro('')
    if (ATIVOS.includes(resultado.status)) return
    qa.resultados(execucaoId)
      .then((lista) => setCompleto(lista.find((r) => r.id === resultado.id) || null))
      .catch((e) => setErro(e.message))
  }, [resultado, execucaoId])

  const ev = completo?.evidencias
  return (
    <PainelLateral aberto={!!resultado} aoFechar={aoFechar} largura={640}
                   titulo={resultado ? `${resultado.cenarioId} · ${resultado.cenarioNome}` : ''}
                   subtitulo={resultado ? `${resultado.metodo} ${resultado.caminho}` : ''}>
      {resultado && (
        <div className="qa-detalhe">
          <div className="qa-detalhe-topo">
            <QaStatus status={resultado.status} />
            {resultado.httpStatus && <span className="qa-pilula tabular">HTTP {resultado.httpStatus}</span>}
            <span className="qa-pilula tabular">{duracao(resultado.duracaoMs)}</span>
            {ev?.criticidade && <span className="qa-pilula">criticidade {ev.criticidade.toLowerCase()}</span>}
          </div>
          {resultado.mensagem && <p className={`qa-detalhe-msg${resultado.status === 'PASSED' ? '' : ' problema'}`}>{resultado.mensagem}</p>}

          {ATIVOS.includes(resultado.status) && <p className="suave">As evidências aparecem quando o cenário terminar.</p>}
          {erro && <ErroCarga erro={erro} />}
          {!ATIVOS.includes(resultado.status) && !completo && !erro && <Esqueleto linhas={6} />}

          {ev && (
            <>
              <dl className="qa-detalhe-info">
                {ev.descricao && <><dt>O que testa</dt><dd>{ev.descricao}</dd></>}
                {ev.preCondicoes && <><dt>Pré-condições</dt><dd>{ev.preCondicoes}</dd></>}
                {ev.esperado && <><dt>Esperado</dt><dd>{ev.esperado}</dd></>}
              </dl>

              {ev.validacoes?.length > 0 && (
                <>
                  <h3>Validações</h3>
                  <ul className="qa-validacoes">
                    {ev.validacoes.map((v, i) => (
                      <li key={i} className={v.ok ? 'ok' : 'falhou'}>
                        <Icone nome={v.ok ? 'check' : 'x'} tamanho={15} />
                        <div>
                          <strong>{v.descricao}</strong>
                          {!v.ok && <span className="mono">esperado: {v.esperado} · recebido: {v.recebido}</span>}
                        </div>
                      </li>
                    ))}
                  </ul>
                </>
              )}

              {ev.chamadas?.length > 0 && <h3>Chamadas HTTP</h3>}
              {ev.chamadas?.map((c, i) => (
                <details key={i} className="qa-chamada" open={c.principal}>
                  <summary>
                    <MetodoBadge metodo={c.metodo} />
                    <code className="qa-caminho">{c.caminho}</code>
                    <span className="suave">{c.rotulo}</span>
                    <span className="qa-pilula tabular">{c.status ?? '—'} · {duracao(c.duracaoMs)}</span>
                  </summary>
                  {c.headers && Object.keys(c.headers).length > 0 && (
                    <>
                      <h4>Headers enviados</h4>
                      <pre className="qa-codigo">{Object.entries(c.headers).map(([k, v]) => `${k}: ${v}`).join('\n')}</pre>
                    </>
                  )}
                  {c.corpoEnviado && <><h4>Corpo enviado</h4><pre className="qa-codigo">{formatar(c.corpoEnviado)}</pre></>}
                  <h4>Resposta</h4>
                  <pre className="qa-codigo">{c.corpoRecebido ? formatar(c.corpoRecebido) : '(sem corpo)'}</pre>
                </details>
              ))}
              <p className="qa-ajuda">Tokens, senhas, CPF, CNPJ, e-mails e telefones são mascarados pelo backend antes de serem salvos.</p>
            </>
          )}
        </div>
      )}
    </PainelLateral>
  )
}

function formatar(texto) {
  try {
    return JSON.stringify(JSON.parse(texto), null, 2)
  } catch {
    return texto
  }
}
