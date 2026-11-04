import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../../components/Icone.jsx'
import MascoteDuvida from '../../components/MascoteDuvida.jsx'
import {
  CabecalhoQa, ErroCarga, Esqueleto, PainelLateral, QaStatus, STATUS_EXECUCAO, dataHoraCurta, duracao,
} from '../../components/qa/QaComum.jsx'

const TAMANHO = 15
const SEM_FILTROS = { status: '', ambiente: '', usuario: '', de: '', ate: '', busca: '', endpoint: '', suite: '', resultado: '' }

// Histórico de execuções: filtros (data, usuário, ambiente, endpoint, resultado, suíte), paginação,
// comparação de duas execuções (com regressões destacadas) e reexecução
export default function QaExecucoes() {
  const { pode } = useAuth()
  const navegar = useNavigate()
  const { state } = useLocation()
  const [filtros, setFiltros] = useState(() => ({ ...SEM_FILTROS, suite: state?.suite || '' }))
  const [opcoes, setOpcoes] = useState({ endpoints: [], suites: [] })
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')
  const [marcadas, setMarcadas] = useState([])
  const [comparacao, setComparacao] = useState(null)
  const [erroAcao, setErroAcao] = useState('')

  useEffect(() => {
    Promise.all([qa.endpoints(), qa.suites()]).then(([endpoints, suites]) => setOpcoes({ endpoints, suites })).catch(() => {})
  }, [])

  const carregar = useCallback(async () => {
    try {
      setDados(await qa.execucoes({ ...filtros, pagina, tamanho: TAMANHO }))
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [filtros, pagina])

  // Busca com pequeno atraso para não disparar uma requisição a cada tecla
  useEffect(() => {
    const t = setTimeout(carregar, 250)
    return () => clearTimeout(t)
  }, [carregar])

  const emAndamento = dados?.itens.some((e) => e.status === 'PENDING' || e.status === 'RUNNING')
  useEffect(() => {
    if (!emAndamento) return
    const t = setInterval(carregar, 4000)
    return () => clearInterval(t)
  }, [emAndamento, carregar])

  function mudar(campo, valor) {
    setFiltros((f) => ({ ...f, [campo]: valor }))
    setPagina(0)
  }

  function marcar(id) {
    setMarcadas((m) => (m.includes(id) ? m.filter((x) => x !== id) : [...m.slice(-1), id]))
  }

  async function comparar() {
    setErroAcao('')
    try {
      // A = a mais antiga, B = a mais nova: "regressão" é o que passava em A e passou a falhar em B
      const criada = (id) => new Date(dados?.itens.find((e) => e.id === id)?.criadaEm || 0).getTime()
      const [a, b] = [...marcadas].sort((x, y) => criada(x) - criada(y))
      setComparacao(await qa.comparacao(a, b))
    } catch (e) {
      setErroAcao(e.message)
    }
  }

  async function reexecutar(id) {
    setErroAcao('')
    try {
      const nova = await qa.reexecutar(id)
      navegar(`/qa/execucoes/${nova.id}`)
    } catch (e) {
      setErroAcao(e.message)
    }
  }

  const temFiltro = Object.values(filtros).some(Boolean)
  const paginas = dados ? Math.max(1, Math.ceil(dados.total / TAMANHO)) : 1

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Histórico" descricao="Todas as execuções de teste (ficam em memória: zeram quando a API reinicia).">
        {marcadas.length === 2 && pode('qa.reports.view') && (
          <button type="button" className="botao botao-secundario" onClick={comparar}><Icone nome="comparar" tamanho={16} />Comparar as 2 marcadas</button>
        )}
        {pode('qa.execute') && <Link to="/qa/nova" className="botao botao-primario"><Icone nome="play" tamanho={16} />Nova execução</Link>}
      </CabecalhoQa>

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {erroAcao && <div className="mensagem mensagem-erro" role="alert"><Icone nome="alerta" />{erroAcao}</div>}

      <section className="painel qa-bloco">
        <div className="qa-filtros">
          <label className="busca">
            <Icone nome="busca" tamanho={16} />
            <input value={filtros.busca} onChange={(e) => mudar('busca', e.target.value)} placeholder="Buscar por id ou usuário" aria-label="Buscar execução" />
          </label>
          <select className="qa-select" value={filtros.status} onChange={(e) => mudar('status', e.target.value)} aria-label="Status">
            <option value="">Todos os status</option>
            {Object.entries(STATUS_EXECUCAO).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
          </select>
          <select className="qa-select" value={filtros.ambiente} onChange={(e) => mudar('ambiente', e.target.value)} aria-label="Ambiente">
            <option value="">Todos os ambientes</option>
            <option value="DESENVOLVIMENTO">Desenvolvimento</option>
            <option value="HOMOLOGACAO">Homologação</option>
          </select>
          <select className="qa-select" value={filtros.resultado} onChange={(e) => mudar('resultado', e.target.value)} aria-label="Resultado">
            <option value="">Qualquer resultado</option>
            <option value="COM_FALHAS">Com falhas</option>
            <option value="SEM_FALHAS">Sem falhas</option>
          </select>
          <select className="qa-select" value={filtros.endpoint} onChange={(e) => mudar('endpoint', e.target.value)} aria-label="Endpoint">
            <option value="">Todos os endpoints</option>
            {opcoes.endpoints.map((ep) => <option key={ep.id} value={ep.id}>{ep.metodo} {ep.caminho} · {ep.nome}</option>)}
          </select>
          <select className="qa-select" value={filtros.suite} onChange={(e) => mudar('suite', e.target.value)} aria-label="Test Suite">
            <option value="">Todas as suítes</option>
            {opcoes.suites.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
          </select>
          <label className="qa-data">De <input type="date" value={filtros.de} onChange={(e) => mudar('de', e.target.value)} /></label>
          <label className="qa-data">Até <input type="date" value={filtros.ate} onChange={(e) => mudar('ate', e.target.value)} /></label>
          {temFiltro && (
            <button type="button" className="botao botao-fantasma botao-compacto"
                    onClick={() => { setFiltros(SEM_FILTROS); setPagina(0) }}>
              Limpar filtros
            </button>
          )}
        </div>
        {pode('qa.reports.view') && <p className="qa-ajuda">Marque duas execuções para comparar os resultados cenário a cenário.</p>}

        {!dados && !erro && <Esqueleto linhas={8} altura={22} />}
        {dados && dados.itens.length === 0 && (
          <div className="vazio">
            <MascoteDuvida />
            <strong>{temFiltro ? 'Nenhuma execução com esses filtros' : 'Nenhuma execução ainda'}</strong>
            <span>{temFiltro ? 'Ajuste ou limpe os filtros.' : 'Configure a primeira em Nova execução.'}</span>
          </div>
        )}

        {dados && dados.itens.length > 0 && (
          <div className="qa-tabela-rolagem">
            <table className="qa-tabela">
              <thead>
                <tr>
                  {pode('qa.reports.view') && <th aria-label="Comparar" />}
                  <th>Execução</th><th>Status</th><th>Resultado</th><th>Ambiente</th><th>Usuário</th><th>Criada</th><th className="num">Duração</th><th aria-label="Ações" />
                </tr>
              </thead>
              <tbody>
                {dados.itens.map((e) => (
                  <tr key={e.id} className="qa-linha">
                    {pode('qa.reports.view') && (
                      <td><input type="checkbox" checked={marcadas.includes(e.id)} onChange={() => marcar(e.id)} aria-label={`Marcar #${e.id.slice(0, 8)} para comparar`} /></td>
                    )}
                    <td>
                      <Link to={`/qa/execucoes/${e.id}`} className="mono qa-id-link">#{e.id.slice(0, 8)}</Link>
                      {e.suiteNome && <span className="qa-cenario-msg">suíte {e.suiteNome}</span>}
                      {e.origemId && <span className="qa-cenario-msg">reexecução de #{e.origemId.slice(0, 8)}</span>}
                    </td>
                    <td><QaStatus status={e.status} tipo="execucao" /></td>
                    <td className="tabular">
                      <span className="qa-placar">
                        <span><b>{e.aprovados}</b>/{e.total}</span>
                        {e.reprovados > 0 && <span className="qa-placar-ruim">{e.reprovados} reprov.</span>}
                        {e.erros > 0 && <span className="qa-placar-alerta">{e.erros} erro(s)</span>}
                        {e.pulados > 0 && <span className="suave">{e.pulados} bloqueado(s)</span>}
                        {e.cancelados > 0 && <span className="suave">{e.cancelados} cancelado(s)</span>}
                      </span>
                    </td>
                    <td>{e.ambienteRotulo}</td>
                    <td className="suave">{e.usuario}</td>
                    <td className="tabular suave">{dataHoraCurta(e.criadaEm)}</td>
                    <td className="num tabular">{duracao(e.duracaoMs)}</td>
                    <td className="qa-acoes-linha">
                      {pode('qa.execute') && !['PENDING', 'RUNNING'].includes(e.status) && (
                        <button type="button" className="botao botao-fantasma botao-compacto" onClick={() => reexecutar(e.id)} title="Reexecutar com a mesma seleção">
                          <Icone nome="refazer" tamanho={15} />Reexecutar
                        </button>
                      )}
                      <Link to={`/qa/execucoes/${e.id}`} className="botao botao-fantasma botao-compacto" aria-label="Abrir"><Icone nome="seta" tamanho={15} /></Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {dados && dados.total > TAMANHO && (
          <div className="qa-paginacao">
            <button type="button" className="botao botao-secundario botao-compacto" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button>
            <span className="tabular">Página {pagina + 1} de {paginas} · {dados.total} execuções</span>
            <button type="button" className="botao botao-secundario botao-compacto" disabled={pagina + 1 >= paginas} onClick={() => setPagina(pagina + 1)}>Próxima</button>
          </div>
        )}
      </section>

      <PainelLateral aberto={!!comparacao} aoFechar={() => setComparacao(null)} largura={680}
                     titulo="Comparação de execuções"
                     subtitulo={comparacao ? `#${comparacao.a.slice(0, 8)} (A, mais antiga) × #${comparacao.b.slice(0, 8)} (B, mais nova)` : ''}>
        {comparacao && (
          <>
            <div className="qa-comparacao-resumo">
              <span className={comparacao.regressoes ? 'ruim' : ''}><b className="tabular">{comparacao.regressoes}</b> regressão(ões)</span>
              <span className={comparacao.correcoes ? 'bom' : ''}><b className="tabular">{comparacao.correcoes}</b> correção(ões)</span>
              <span><b className="tabular">{comparacao.mudancas}</b> mudança(s) no total</span>
            </div>
            <table className="qa-tabela">
              <thead><tr><th>Cenário</th><th>A</th><th>B</th></tr></thead>
              <tbody>
                {comparacao.linhas.map((l) => (
                  <tr key={l.cenarioId} className={l.regressao ? 'qa-linha-regressao' : l.correcao ? 'qa-linha-correcao' : l.mudou ? 'qa-linha-mudou' : ''}>
                    <td>
                      <span className="mono qa-id">{l.cenarioId}</span><span className="qa-cenario-nome">{l.cenarioNome}</span>
                      {l.regressao && <span className="qa-selo-regressao">Regressão</span>}
                      {l.correcao && <span className="qa-selo-correcao">Corrigido</span>}
                    </td>
                    <td>{l.statusA ? <QaStatus status={l.statusA} /> : <span className="suave">não rodou</span>}</td>
                    <td>{l.statusB ? <QaStatus status={l.statusB} /> : <span className="suave">não rodou</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </PainelLateral>
    </div>
  )
}
