import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { dataHora, hora, tempoRestante } from '../format.js'
import StatusBadge from '../components/StatusBadge.jsx'
import Cabecalho from '../components/Cabecalho.jsx'
import Icone from '../components/Icone.jsx'
import { SimboloMarca } from '../components/Marca.jsx'
import MascoteDuvida from '../components/MascoteDuvida.jsx'

// Abas de filtro: cada uma junta um ou mais status
const ABAS = [
  { chave: 'todas', label: 'Todas', status: null },
  { chave: 'andamento', label: 'Em andamento', status: ['RECEBIDO', 'AGUARDANDO_DOCUMENTO', 'EM_PROCESSAMENTO'] },
  { chave: 'aprovadas', label: 'Aprovadas', status: ['APROVADO'] },
  { chave: 'reprovadas', label: 'Reprovadas', status: ['REPROVADO'] },
  { chave: 'encerradas', label: 'Encerradas', status: ['CANCELADO', 'EXPIRADO'] },
]

function tempoAtras(iso, agora) {
  const s = Math.max(0, (agora - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'agora mesmo'
  if (s < 3600) return `há ${Math.floor(s / 60)} min`
  if (s < 86400) return `há ${Math.floor(s / 3600)} h`
  return `há ${Math.floor(s / 86400)} d`
}

export default function Lista() {
  const navegar = useNavigate()
  // A aba fica na URL (?filtro=reprovadas): dá para salvar o link já filtrado
  const [parametros, setParametros] = useSearchParams()
  const aba = ABAS.find((a) => a.chave === parametros.get('filtro')) || ABAS[0]
  const escolherAba = (chave) => setParametros(chave === 'todas' ? {} : { filtro: chave }, { replace: true })

  const [busca, setBusca] = useState('')
  const [itens, setItens] = useState(null)
  const [erro, setErro] = useState('')
  const [atualizadoEm, setAtualizadoEm] = useState(null)
  const [agora, setAgora] = useState(Date.now())

  // Busca todas as solicitações a cada 5s (o status muda sozinho no backend); o filtro é feito aqui
  useEffect(() => {
    let ativo = true
    async function carregar() {
      try {
        const lista = await api.listar()
        if (ativo) { setItens(lista); setErro(''); setAtualizadoEm(new Date().toISOString()) }
      } catch (e) {
        if (ativo) setErro(e.message)
      }
    }
    carregar()
    const t = setInterval(carregar, 5000)
    return () => { ativo = false; clearInterval(t) }
  }, [])

  // Relógio para a contagem regressiva do prazo e o "há X min"
  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])

  const contagem = useMemo(() => Object.fromEntries(ABAS.map((a) => [
    a.chave, (itens || []).filter((s) => !a.status || a.status.includes(s.status)).length,
  ])), [itens])

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return (itens || [])
      .filter((s) => !aba.status || aba.status.includes(s.status))
      .filter((s) => !termo || s.solicitacao.toLowerCase().includes(termo))
  }, [itens, aba, busca])

  return (
    <div className="pagina">
      <Cabecalho titulo="Solicitações" descricao="Acompanhe cada solicitação, do envio dos documentos ao resultado da análise.">
        <span className={`ao-vivo${erro ? ' com-erro' : ''}`}>
          <span className="ao-vivo-ponto" />
          {erro ? 'Sem conexão com a API' : atualizadoEm ? <>Ao vivo · atualizado às <span className="tabular">{hora(atualizadoEm)}</span></> : 'Conectando…'}
        </span>
      </Cabecalho>

      <section className="caixa-lista">
        {/* Abas com contagem (esquerda) + busca (direita) na mesma faixa */}
        <div className="caixa-topo">
          <nav className="filtros" aria-label="Filtrar solicitações">
            {ABAS.map((a) => (
              <button key={a.chave} type="button" className={`filtro${a.chave === aba.chave ? ' ativo' : ''}`}
                      onClick={() => escolherAba(a.chave)} aria-pressed={a.chave === aba.chave}>
                {a.label}
                <span className="filtro-total tabular">{itens ? contagem[a.chave] : '–'}</span>
              </button>
            ))}
          </nav>
          <label className="busca">
            <Icone nome="busca" tamanho={16} />
            <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Buscar por solicitação" aria-label="Buscar por solicitação" />
            {busca && <button type="button" className="busca-limpar" onClick={() => setBusca('')} aria-label="Limpar busca"><Icone nome="x" tamanho={14} /></button>}
          </label>
        </div>

        {erro && <div className="mensagem mensagem-erro margem"><Icone nome="alerta" />{erro}</div>}

        {itens === null && !erro && <div className="vazio">Carregando…</div>}

        {itens && filtrados.length === 0 && (
          <div className="vazio">
            {busca || aba.status ? <MascoteDuvida /> : <SimboloMarca tamanho={56} />}
            <strong>{busca || aba.status ? 'Nada por aqui com esse filtro' : 'Nenhuma solicitação ainda'}</strong>
            <span>{busca || aba.status ? 'Tente outra aba ou limpe a busca.' : 'As solicitações criadas pela API aparecem aqui automaticamente.'}</span>
          </div>
        )}

        {filtrados.length > 0 && (
          <div className="tabela-rolagem">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Solicitação</th>
                  <th>Status</th>
                  <th>Documentos</th>
                  <th>Criada</th>
                  <th>Prazo de envio</th>
                  <th aria-label="Abrir" />
                </tr>
              </thead>
              <tbody>
                {filtrados.map((s) => {
                  const pct = s.documentosEsperados ? (s.documentosRecebidos / s.documentosEsperados) * 100 : 0
                  const aguardando = s.status === 'AGUARDANDO_DOCUMENTO' && s.prazoDocumento
                  return (
                    <tr key={s.id} onClick={() => navegar(`/solicitacoes/${s.id}`)}>
                      <td>
                        <Link to={`/solicitacoes/${s.id}`} className="numero-solicitacao" onClick={(e) => e.stopPropagation()}>{s.solicitacao}</Link>
                        <span className="id-curto mono" title={s.id}>#{s.id.slice(0, 8)}</span>
                      </td>
                      <td><StatusBadge status={s.status} /></td>
                      <td>
                        <span className="docs">
                          <span className="docs-barra"><span style={{ width: `${pct}%` }} className={pct === 100 ? 'completo' : ''} /></span>
                          <span className="tabular">{s.documentosRecebidos}/{s.documentosEsperados}</span>
                        </span>
                      </td>
                      <td>
                        <span className="data-dupla">
                          <span>{tempoAtras(s.dataCriacao, agora)}</span>
                          <small className="tabular">{dataHora(s.dataCriacao)}</small>
                        </span>
                      </td>
                      <td>
                        {aguardando
                          ? <span className="prazo-contagem tabular"><Icone nome="relogio" tamanho={14} />{tempoRestante(s.prazoDocumento, agora)}</span>
                          : <span className="suave">—</span>}
                      </td>
                      <td className="seta-linha"><Icone nome="seta" tamanho={16} /></td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {itens && itens.length > 0 && (
          <footer className="caixa-rodape">
            Mostrando <strong className="tabular">{filtrados.length}</strong> de <strong className="tabular">{itens.length}</strong> solicitações
          </footer>
        )}
      </section>
    </div>
  )
}
