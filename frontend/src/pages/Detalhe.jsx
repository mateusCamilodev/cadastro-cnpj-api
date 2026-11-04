import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { FINAIS, STATUS, dataCurta, dataHora, tempoAtras } from '../format.js'
import { useAgora } from '../hooks.js'
import StatusBadge from '../components/StatusBadge.jsx'
import Icone from '../components/Icone.jsx'
import Mascote from '../components/Mascote.jsx'
import Carimbo from '../components/Carimbo.jsx'
import Etapas from '../components/Etapas.jsx'
import Documentos from '../components/Documentos.jsx'
import Historico from '../components/Historico.jsx'
import Prazo from '../components/Prazo.jsx'
import ProgressoDocumentos from '../components/ProgressoDocumentos.jsx'
import Carregando from '../components/Carregando.jsx'

const NOMES_CAMPOS = {
  solicitacao: 'número da solicitação', nome: 'nome', cpf: 'CPF', email: 'e-mail', telefone: 'telefone', dataNascimento: 'data de nascimento',
  cnpj: 'CNPJ', razaoSocial: 'razão social', nomeFantasia: 'nome fantasia',
}

// "Análise concluída: ... Campos com divergência: cpf, solicitacao" → "CPF e número da solicitação"
function camposDivergentes(historico) {
  const texto = historico.find((ev) => ev.status === 'REPROVADO')?.descricao || ''
  const campos = (texto.split('divergência:')[1] || '').split(',').map((c) => NOMES_CAMPOS[c.trim()] || c.trim()).filter(Boolean)
  if (campos.length <= 1) return campos[0] || 'campos não informados'
  return `${campos.slice(0, -1).join(', ')} e ${campos.at(-1)}`
}

// Botão de copiar o ID completo (o ID curto é o que aparece na tela)
function CopiarId({ id }) {
  const [copiado, setCopiado] = useState(false)
  async function copiar() {
    try {
      await navigator.clipboard.writeText(id)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 1600)
    } catch { /* navegador sem permissão de área de transferência: o ID continua visível no title */ }
  }
  return (
    <button type="button" className={`id-copiar${copiado ? ' copiado' : ''}`} onClick={copiar} title={id}
            aria-label={copiado ? 'ID copiado' : 'Copiar ID completo'}>
      <span className="mono">#{id.slice(0, 8)}</span>
      <Icone nome={copiado ? 'check' : 'copiar'} tamanho={13} />
    </button>
  )
}

export default function Detalhe() {
  const { id } = useParams()
  const [detalhe, setDetalhe] = useState(null)   // GET /solicitacoes/{id} → status + histórico
  const [resumo, setResumo] = useState(null)     // item do GET /solicitacoes → número, datas, prazo e arquivos
  const [erro, setErro] = useState('')
  const agora = useAgora(30000)                  // só para o "há X min" do cabeçalho

  const carregar = useCallback(async () => {
    try {
      const [d, lista] = await Promise.all([api.buscar(id), api.listar()])
      setDetalhe(d)
      setResumo(lista.find((s) => s.id === id) || null)
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [id])

  // Atualiza a cada 3s enquanto a solicitação ainda pode mudar
  const statusAtual = useRef(null)
  useEffect(() => { statusAtual.current = detalhe?.status }, [detalhe])
  useEffect(() => {
    carregar()
    const t = setInterval(() => {
      if (!FINAIS.includes(statusAtual.current)) carregar()
    }, 3000)
    return () => clearInterval(t)
  }, [carregar])

  const trilha = (
    <nav className="trilha" aria-label="Você está em">
      <Link to="/solicitacoes" className="trilha-voltar"><Icone nome="voltar" tamanho={15} />Solicitações</Link>
    </nav>
  )

  if (erro && !detalhe) {
    return (
      <div className="pagina">
        {trilha}
        <h1 className="detalhe-titulo">Solicitação não encontrada</h1>
        <div className="mensagem mensagem-erro"><Icone nome="alerta" />{erro}</div>
      </div>
    )
  }
  if (!detalhe) return <div className="pagina"><Carregando texto="Carregando solicitação…" /></div>

  const status = detalhe.status
  const interrompida = status === 'CANCELADO' || status === 'EXPIRADO'
  const aguardando = status === 'AGUARDANDO_DOCUMENTO'
  const temResultado = status === 'APROVADO' || status === 'REPROVADO'
  const numero = resumo?.solicitacao || 'Solicitação'

  return (
    <div className="pagina">
      {trilha}

      {/* Cabeçalho: número + status lado a lado (o dado mais importante fica no topo) */}
      <header className="detalhe-cabecalho">
        <div className="detalhe-titulo-linha">
          <h1 className="detalhe-titulo">{numero}</h1>
          <StatusBadge status={status} />
        </div>
        <p className="detalhe-meta">
          {resumo && <>Criada {tempoAtras(resumo.dataCriacao, agora)} <span className="ponto-sep" aria-hidden="true" /> </>}
          <CopiarId id={id} />
        </p>
        {/* Leitor de tela: avisa quando o status muda com a atualização automática */}
        <p className="somente-leitor" aria-live="polite">Status atual: {STATUS[status]?.label}</p>
      </header>

      {erro && (
        <div className="mensagem mensagem-alerta" role="status">
          <Icone nome="alerta" />Não foi possível atualizar agora ({erro}). Mostrando os últimos dados recebidos.
        </div>
      )}

      {/* Números principais */}
      <dl className="painel metricas">
        <div>
          <dt>Prazo de envio</dt>
          <dd>{aguardando && resumo?.prazoDocumento ? <Prazo ate={resumo.prazoDocumento} grande /> : <span className="suave">{status === 'RECEBIDO' ? 'Liberado em instantes' : 'Encerrado'}</span>}</dd>
        </div>
        <div>
          <dt>Documentos</dt>
          <dd>{resumo ? <ProgressoDocumentos recebidos={resumo.documentosRecebidos} esperados={resumo.documentosEsperados} rotulo /> : '—'}</dd>
        </div>
        <div>
          <dt>Criada em</dt>
          <dd className="tabular">{dataHora(resumo?.dataCriacao)}</dd>
        </div>
        <div>
          <dt>Última atualização</dt>
          <dd className="tabular">{dataHora(resumo?.dataAtualizacao)}</dd>
        </div>
      </dl>

      {!interrompida && <section className="painel painel-etapas"><Etapas status={status} /></section>}

      {interrompida && (
        <div className="mensagem mensagem-neutra">
          <Icone nome="alerta" />
          Solicitação {status === 'CANCELADO' ? 'cancelada' : 'expirada'}. O número da solicitação e o CNPJ ficaram disponíveis para uma nova solicitação.
        </div>
      )}

      {temResultado && (
        <section className={`resultado resultado-${status === 'APROVADO' ? 'aprovado' : 'reprovado'}`}>
          <Mascote humor={status === 'APROVADO' ? 'feliz' : 'triste'} />
          <div className="resultado-texto">
            <h2>{status === 'APROVADO' ? 'Solicitação aprovada' : 'Solicitação reprovada'}</h2>
            {status === 'APROVADO' ? (
              <p>Os dados de todos os documentos conferem com os informados na criação.</p>
            ) : (
              <>
                <p>Os documentos não conferem com a solicitação em: <strong>{camposDivergentes(detalhe.historico)}</strong>.</p>
                <p className="resultado-dica">Para tentar de novo, abra uma nova solicitação pela API. O número da solicitação e o CNPJ já estão liberados.</p>
              </>
            )}
          </div>
          <Carimbo aprovado={status === 'APROVADO'} data={dataCurta(resumo?.dataAtualizacao)} />
        </section>
      )}

      <div className="colunas">
        <Documentos id={id} arquivos={resumo?.arquivos} status={status} prazo={resumo?.prazoDocumento} aoEnviar={carregar} />
        <Historico eventos={detalhe.historico} />
      </div>
    </div>
  )
}
