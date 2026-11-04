import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api.js'
import { dataHora, tempoAtras } from '../format.js'
import { useAgora, useFocoDeLuz } from '../hooks.js'
import Prazo from '../components/Prazo.jsx'
import ProgressoDocumentos from '../components/ProgressoDocumentos.jsx'
import Carregando from '../components/Carregando.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import Icone from '../components/Icone.jsx'
import { SimboloMarca } from '../components/Marca.jsx'
import MascoteDuvida from '../components/MascoteDuvida.jsx'
import logoCarimbo from '../assets/logo-carimbo.png'

// Abas de filtro: cada uma junta um ou mais status
const ABAS = [
  { chave: 'todas', label: 'Todas', status: null },
  { chave: 'andamento', label: 'Em andamento', status: ['RECEBIDO', 'AGUARDANDO_DOCUMENTO', 'EM_PROCESSAMENTO'] },
  { chave: 'aprovadas', label: 'Aprovadas', status: ['APROVADO'] },
  { chave: 'reprovadas', label: 'Reprovadas', status: ['REPROVADO'] },
  { chave: 'encerradas', label: 'Encerradas', status: ['CANCELADO', 'EXPIRADO'] },
]

export default function Lista() {
  const navegar = useNavigate()
  // A aba fica na URL (?filtro=reprovadas): dá para salvar o link já filtrado
  const [parametros, setParametros] = useSearchParams()
  const aba = ABAS.find((a) => a.chave === parametros.get('filtro')) || ABAS[0]
  const escolherAba = (chave) => setParametros(chave === 'todas' ? {} : { filtro: chave }, { replace: true })

  const [busca, setBusca] = useState('')
  const [itens, setItens] = useState(null)
  const [erro, setErro] = useState('')
  const agora = useAgora(30000)   // "há X min"; o prazo tem o próprio relógio de 1s
  const focoDeLuz = useFocoDeLuz()  // brilho do título que segue o mouse

  // Busca todas as solicitações a cada 5s (o status muda sozinho no backend); o filtro é feito aqui
  useEffect(() => {
    let ativo = true
    async function carregar() {
      try {
        const lista = await api.listar()
        if (ativo) { setItens(lista); setErro('') }
      } catch (e) {
        if (ativo) setErro(e.message)
      }
    }
    carregar()
    const t = setInterval(carregar, 5000)
    return () => { ativo = false; clearInterval(t) }
  }, [])

  const contagem = useMemo(() => Object.fromEntries(ABAS.map((a) => [
    a.chave, (itens || []).filter((s) => !a.status || a.status.includes(s.status)).length,
  ])), [itens])

  // Para o aviso do título: quem ainda espera documento e o prazo que vence primeiro
  const aguardandoEnvio = useMemo(() => (itens || []).filter((s) => s.status === 'AGUARDANDO_DOCUMENTO' && s.prazoDocumento), [itens])
  const proximoPrazo = useMemo(() => aguardandoEnvio.map((s) => s.prazoDocumento).sort()[0], [aguardandoEnvio])

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return (itens || [])
      .filter((s) => !aba.status || aba.status.includes(s.status))
      .filter((s) => !termo || s.solicitacao.toLowerCase().includes(termo))
  }, [itens, aba, busca])

  return (
    <div className="pagina">
      {/* Título centralizado, sem caixa: selo da marca, título, descrição e o que precisa de atenção agora */}
      <header className="titulo-pagina" {...focoDeLuz}>
        <span className="titulo-pagina-selo"><img src={logoCarimbo} alt="" height="18" width="25" />Portal Carimbô</span>
        <h1 data-luz>Solicitações</h1>
        <p>Acompanhe cada solicitação, do envio dos documentos ao resultado da análise.</p>
        {itens && (aguardandoEnvio.length > 0 ? (
          <button type="button" className="titulo-pagina-aviso" onClick={() => escolherAba('andamento')}
                  title="Ver as solicitações em andamento">
            <span className="titulo-pagina-ponto" aria-hidden="true" />
            <span><strong className="tabular">{aguardandoEnvio.length}</strong> {aguardandoEnvio.length === 1 ? 'aguardando documento' : 'aguardando documentos'}</span>
            <span className="titulo-pagina-sep" aria-hidden="true" />
            <span>próximo prazo</span>
            <Prazo ate={proximoPrazo} />
          </button>
        ) : (
          <span className="titulo-pagina-aviso titulo-pagina-livre">
            <Icone nome="check" tamanho={15} />Nenhum documento pendente
          </span>
        ))}
      </header>

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

        {itens === null && !erro && <div className="vazio"><Carregando texto="Carregando solicitações…" /></div>}

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
                  const aguardando = s.status === 'AGUARDANDO_DOCUMENTO' && s.prazoDocumento
                  return (
                    <tr key={s.id} onClick={() => navegar(`/solicitacoes/${s.id}`)}>
                      <td data-rotulo="Solicitação">
                        <Link to={`/solicitacoes/${s.id}`} className="numero-solicitacao" onClick={(e) => e.stopPropagation()}>{s.solicitacao}</Link>
                        <span className="id-curto mono" title={s.id}>#{s.id.slice(0, 8)}</span>
                      </td>
                      <td data-rotulo="Status"><StatusBadge status={s.status} /></td>
                      <td data-rotulo="Documentos"><ProgressoDocumentos recebidos={s.documentosRecebidos} esperados={s.documentosEsperados} /></td>
                      <td data-rotulo="Criada">
                        <span className="data-dupla">
                          <span>{tempoAtras(s.dataCriacao, agora)}</span>
                          <small className="tabular">{dataHora(s.dataCriacao)}</small>
                        </span>
                      </td>
                      <td data-rotulo="Prazo">
                        {aguardando
                          ? <Prazo ate={s.prazoDocumento} />
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
