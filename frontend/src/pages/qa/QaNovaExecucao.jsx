import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { qa } from '../../api.js'
import Icone from '../../components/Icone.jsx'
import { CabecalhoQa, Dialogo, ErroCarga, Esqueleto, MetodoBadge, QaStatus } from '../../components/qa/QaComum.jsx'

const MODOS = [
  { id: 'suite', label: 'Test Suite', ajuda: 'Uma coleção pronta de cenários' },
  { id: 'cenarios', label: 'Cenários', ajuda: 'Escolha um a um' },
  { id: 'endpoints', label: 'Endpoints e categorias', ajuda: 'Tudo o que combinar' },
]

// Configuração de uma execução. Pode chegar pré-preenchida (state da navegação) a partir de
// Endpoints, Cenários ou Test Suites. Nada roda no navegador: o backend executa em segundo plano.
export default function QaNovaExecucao() {
  const navegar = useNavigate()
  const { state } = useLocation()
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')

  const [modo, setModo] = useState(state?.modo || 'suite')
  const [suiteId, setSuiteId] = useState(state?.suiteId || '')
  const [cenariosMarcados, setCenariosMarcados] = useState(() => new Set(state?.cenarioIds || []))
  const [endpointsMarcados, setEndpointsMarcados] = useState(() => new Set(state?.endpointIds || []))
  const [categorias, setCategorias] = useState(() => new Set())
  const [busca, setBusca] = useState('')
  const [ambiente, setAmbiente] = useState('DESENVOLVIMENTO')
  const [timeout, setTimeout_] = useState('')

  const [confirmando, setConfirmando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [erroExecucao, setErroExecucao] = useState('')

  const carregar = useCallback(async () => {
    try {
      const [endpoints, cenarios, cats, ambientes, suites, config] = await Promise.all([
        qa.endpoints(), qa.cenarios(), qa.categorias(), qa.ambientes(), qa.suites(), qa.configuracoes(),
      ])
      setDados({ endpoints, cenarios, categorias: cats, ambientes, suites, config })
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [])
  useEffect(() => { carregar() }, [carregar])

  // Vindo de Endpoints sem categorias escolhidas: já marca todas as que têm cenário
  useEffect(() => {
    if (dados && state?.modo === 'endpoints' && categorias.size === 0) {
      setCategorias(new Set(dados.categorias.filter((c) => c.cenarios > 0).map((c) => c.id)))
    }
  }, [dados]) // eslint-disable-line react-hooks/exhaustive-deps

  const porId = useMemo(() => new Map((dados?.cenarios || []).map((c) => [c.id, c])), [dados])
  const suite = dados?.suites.find((s) => s.id === suiteId)

  // Lista final de cenários, conforme o modo
  const selecionados = useMemo(() => {
    if (!dados) return []
    if (modo === 'suite') return suite ? suite.cenarioIds.map((id) => porId.get(id)).filter(Boolean) : []
    if (modo === 'cenarios') return dados.cenarios.filter((c) => cenariosMarcados.has(c.id))
    return dados.cenarios.filter((c) => endpointsMarcados.has(c.endpointId) && categorias.has(c.categoria))
  }, [dados, modo, suite, porId, cenariosMarcados, endpointsMarcados, categorias])

  const ambienteInfo = dados?.ambientes.find((a) => a.id === ambiente)
  const timeoutNum = timeout === '' ? null : Number(timeout)
  const timeoutMax = dados?.config.limites.timeoutRequisicaoMax || 60
  const timeoutInvalido = timeoutNum !== null && (!Number.isInteger(timeoutNum) || timeoutNum < 1 || timeoutNum > timeoutMax)
  const lentos = selecionados.filter((c) => c.categoria === 'VALIDACAO_EMPRESA' || c.categoria === 'UPLOAD_PDF').length

  function alternar(conjunto, setConjunto, valor) {
    const novo = new Set(conjunto)
    if (novo.has(valor)) novo.delete(valor); else novo.add(valor)
    setConjunto(novo)
  }

  // Cenários agrupados por endpoint, filtrados pela busca (modo "Cenários")
  const gruposCenarios = useMemo(() => {
    if (!dados) return []
    const termo = busca.trim().toLowerCase()
    const mapa = new Map()
    dados.cenarios
      .filter((c) => !termo || `${c.id} ${c.nome} ${c.caminho} ${c.categoriaRotulo}`.toLowerCase().includes(termo))
      .forEach((c) => mapa.set(c.endpointId, [...(mapa.get(c.endpointId) || []), c]))
    return [...mapa.entries()].map(([id, lista]) => ({ endpoint: dados.endpoints.find((e) => e.id === id), lista }))
  }, [dados, busca])

  function alternarLista(lista) {
    const novo = new Set(cenariosMarcados)
    const todos = lista.every((c) => novo.has(c.id))
    lista.forEach((c) => (todos ? novo.delete(c.id) : novo.add(c.id)))
    setCenariosMarcados(novo)
  }

  async function executar() {
    setEnviando(true)
    setErroExecucao('')
    const pedido = { ambiente, ...(timeoutNum ? { timeoutRequisicaoSegundos: timeoutNum } : {}) }
    if (modo === 'suite') pedido.suiteId = suiteId
    else if (modo === 'cenarios') pedido.cenarioIds = selecionados.map((c) => c.id)
    else {
      pedido.endpointIds = [...endpointsMarcados]
      pedido.categorias = [...categorias]
    }
    try {
      const execucao = await qa.criarExecucao(pedido)
      navegar(`/qa/execucoes/${execucao.id}`)
    } catch (e) {
      setErroExecucao(e.message)
      setEnviando(false)
    }
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Nova execução" descricao="Escolha o que testar e onde. Os testes rodam no servidor, em segundo plano, com dados de teste marcados “QA-”." />

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {!dados && !erro && <div className="painel qa-bloco"><Esqueleto linhas={8} altura={22} /></div>}

      {dados && (
        <div className="qa-layout-config">
          <section className="painel qa-bloco">
            <div className="qa-segmentado" role="tablist" aria-label="Forma de seleção">
              {MODOS.map((m) => (
                <button key={m.id} type="button" role="tab" aria-selected={modo === m.id}
                        className={`qa-segmento${modo === m.id ? ' ativo' : ''}`} onClick={() => setModo(m.id)}>
                  <strong>{m.label}</strong><span>{m.ajuda}</span>
                </button>
              ))}
            </div>

            {modo === 'suite' && (
              <ul className="qa-suites-escolha">
                {dados.suites.map((s) => (
                  <li key={s.id}>
                    <label className={suiteId === s.id ? 'selecionado' : ''}>
                      <input type="radio" name="suite" checked={suiteId === s.id} onChange={() => setSuiteId(s.id)} />
                      <span className="qa-suite-texto">
                        <span className="qa-suite-nome">{s.nome}{s.padrao && <span className="qa-tag-neutra">padrão</span>}</span>
                        {s.descricao && <span className="qa-endpoint-desc">{s.descricao}</span>}
                      </span>
                      <span className="qa-suite-meta">
                        <span className="tabular">{s.cenarioIds.length} cenário(s)</span>
                        {s.ultimoStatus ? <QaStatus status={s.ultimoStatus} tipo="execucao" /> : <span className="suave">Nunca executada</span>}
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
            )}

            {modo === 'cenarios' && (
              <>
                <div className="qa-filtros">
                  <label className="busca">
                    <Icone nome="busca" tamanho={16} />
                    <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar cenário" aria-label="Buscar cenário" />
                  </label>
                  {cenariosMarcados.size > 0 && (
                    <button type="button" className="botao botao-fantasma botao-compacto" onClick={() => setCenariosMarcados(new Set())}>
                      Limpar ({cenariosMarcados.size})
                    </button>
                  )}
                </div>
                {gruposCenarios.map(({ endpoint, lista }) => {
                  const marcados = lista.filter((c) => cenariosMarcados.has(c.id)).length
                  return (
                    <div key={endpoint?.id} className="qa-grupo">
                      <label className="qa-grupo-topo">
                        <input type="checkbox" checked={marcados === lista.length}
                               ref={(el) => { if (el) el.indeterminate = marcados > 0 && marcados < lista.length }}
                               onChange={() => alternarLista(lista)} />
                        <MetodoBadge metodo={endpoint?.metodo} />
                        <strong>{endpoint?.nome}</strong>
                        <code className="qa-caminho">{endpoint?.caminho}</code>
                      </label>
                      <ul className="qa-cenarios-escolha">
                        {lista.map((c) => (
                          <li key={c.id}>
                            <label>
                              <input type="checkbox" checked={cenariosMarcados.has(c.id)} onChange={() => alternar(cenariosMarcados, setCenariosMarcados, c.id)} />
                              <span className="mono qa-id-inline">{c.id}</span>
                              <span>{c.nome}</span>
                              <span className="suave qa-cenario-cat">{c.categoriaRotulo}</span>
                            </label>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )
                })}
              </>
            )}

            {modo === 'endpoints' && (
              <div className="qa-endpoints-categorias">
                <fieldset className="qa-campo">
                  <legend>Endpoints</legend>
                  {dados.endpoints.map((e) => (
                    <label key={e.id} className="qa-opcao">
                      <input type="checkbox" checked={endpointsMarcados.has(e.id)} onChange={() => alternar(endpointsMarcados, setEndpointsMarcados, e.id)} />
                      <MetodoBadge metodo={e.metodo} /> {e.nome}
                      <span className="suave">{e.cenarios}</span>
                    </label>
                  ))}
                </fieldset>
                <fieldset className="qa-campo">
                  <legend>Categorias</legend>
                  {dados.categorias.map((c) => (
                    <label key={c.id} className={`qa-opcao${c.cenarios === 0 ? ' desabilitada' : ''}`}
                           title={c.cenarios === 0 ? 'Ainda não há cenários desta categoria no catálogo' : ''}>
                      <input type="checkbox" checked={categorias.has(c.id)} disabled={c.cenarios === 0}
                             onChange={() => alternar(categorias, setCategorias, c.id)} />
                      {c.rotulo}
                      <span className="suave">{c.cenarios === 0 ? 'sem cenários' : c.cenarios}</span>
                    </label>
                  ))}
                  <button type="button" className="qa-link"
                          onClick={() => setCategorias(new Set(dados.categorias.filter((c) => c.cenarios > 0).map((c) => c.id)))}>
                    Marcar todas
                  </button>
                </fieldset>
              </div>
            )}
          </section>

          <aside className="painel qa-bloco qa-config">
            <h2>Onde e como</h2>
            <fieldset className="qa-campo">
              <legend>Ambiente</legend>
              {dados.ambientes.map((a) => (
                <label key={a.id} className={`qa-opcao${a.disponivel ? '' : ' desabilitada'}`}>
                  <input type="radio" name="ambiente" value={a.id} checked={ambiente === a.id} disabled={!a.disponivel}
                         onChange={() => setAmbiente(a.id)} />
                  {a.rotulo}
                  <span className={`qa-disponivel${a.disponivel ? '' : ' fora'}`}><span className="qa-disponivel-ponto" />{a.disponivel ? 'no ar' : 'fora do ar'}</span>
                </label>
              ))}
              <p className="qa-ajuda">Produção não aparece: os testes criam e cancelam dados de teste.</p>
            </fieldset>

            <label className="qa-campo-texto">
              <span>Tempo limite por requisição (s)</span>
              <input type="number" min={1} max={timeoutMax} inputMode="numeric" value={timeout}
                     placeholder={`padrão: ${dados.config.timeoutRequisicaoSegundos}`} onChange={(e) => setTimeout_(e.target.value)}
                     aria-invalid={timeoutInvalido} />
              {timeoutInvalido && <small className="qa-campo-erro">Entre 1 e {timeoutMax} segundos.</small>}
            </label>

            <div className="qa-resumo">
              <div><span>Ambiente</span><strong>{ambienteInfo?.rotulo}</strong></div>
              <div><span>Seleção</span><strong>{modo === 'suite' ? (suite?.nome || '—') : MODOS.find((m) => m.id === modo).label}</strong></div>
              <div><span>Paralelismo</span><strong className="tabular">{dados.config.cenariosParalelos} por vez</strong></div>
              <div className="qa-resumo-total"><span>Cenários</span><strong className="tabular">{selecionados.length}</strong></div>
            </div>
            {lentos > 0 && <p className="qa-ajuda">{lentos} cenário(s) dependem da análise automática e podem levar cerca de 1 minuto.</p>}

            {erroExecucao && <div className="mensagem mensagem-erro" role="alert"><Icone nome="alerta" />{erroExecucao}</div>}
            <button type="button" className="botao botao-primario botao-largo"
                    disabled={!selecionados.length || !ambienteInfo?.disponivel || timeoutInvalido}
                    onClick={() => setConfirmando(true)}>
              <Icone nome="play" tamanho={16} />Revisar e executar
            </button>
          </aside>
        </div>
      )}

      <Dialogo aberto={confirmando} titulo="Confirmar execução" aoFechar={() => !enviando && setConfirmando(false)}
               acoes={<>
                 <button type="button" className="botao botao-secundario" onClick={() => setConfirmando(false)} disabled={enviando}>Voltar</button>
                 <button type="button" className="botao botao-primario" onClick={executar} disabled={enviando}>
                   {enviando ? 'Iniciando…' : `Executar ${selecionados.length} cenário(s)`}
                 </button>
               </>}>
        <p>
          Os testes rodam em segundo plano contra <strong>{ambienteInfo?.rotulo}</strong>
          {modo === 'suite' && suite ? <> usando a suíte <strong>{suite.nome}</strong></> : null}
          {timeoutNum ? <>, com tempo limite de <strong>{timeoutNum} s</strong> por requisição</> : null}.
        </p>
        <ul className="qa-confirmacao">
          {selecionados.map((c) => (
            <li key={c.id}><span className="mono">{c.id}</span> {c.nome} <MetodoBadge metodo={c.metodo} /></li>
          ))}
        </ul>
        {erroExecucao && <div className="mensagem mensagem-erro"><Icone nome="alerta" />{erroExecucao}</div>}
      </Dialogo>
    </div>
  )
}
