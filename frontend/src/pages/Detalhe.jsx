import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { FINAIS, STATUS, dataHora, hora, tempoRestante } from '../format.js'
import StatusBadge from '../components/StatusBadge.jsx'
import Cabecalho from '../components/Cabecalho.jsx'
import Icone from '../components/Icone.jsx'
import Mascote from '../components/Mascote.jsx'
import Carimbo from '../components/Carimbo.jsx'

// Tela só de consulta: criação, envio de PDF e cancelamento acontecem pela API (Postman, curl...).
// Ela se atualiza sozinha enquanto a solicitação ainda pode mudar.

const ETAPAS = ['Recebida', 'Envio de documentos', 'Análise', 'Resultado']

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

function dataCurta(iso) {
  return iso ? new Date(iso).toLocaleDateString('pt-BR') : null
}

function indiceEtapa(status) {
  return { RECEBIDO: 0, AGUARDANDO_DOCUMENTO: 1, EM_PROCESSAMENTO: 2, APROVADO: 3, REPROVADO: 3 }[status] ?? 0
}

export default function Detalhe() {
  const { id } = useParams()
  const [detalhe, setDetalhe] = useState(null)   // GET /solicitacoes/{id} → status + histórico
  const [resumo, setResumo] = useState(null)     // item do GET /solicitacoes → arquivos declarados
  const [erro, setErro] = useState('')
  const [agora, setAgora] = useState(Date.now())
  const [atualizadoEm, setAtualizadoEm] = useState(null)

  const carregar = useCallback(async () => {
    try {
      const [d, lista] = await Promise.all([api.buscar(id), api.listar()])
      setDetalhe(d)
      setResumo(lista.find((s) => s.id === id) || null)
      setErro('')
      setAtualizadoEm(new Date().toISOString())
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

  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])

  if (erro && !detalhe) {
    return (
      <div className="pagina">
        <Cabecalho trilha={[{ label: 'Solicitações', para: '/solicitacoes' }, { label: 'Detalhe' }]} titulo="Solicitação" />
        <div className="mensagem mensagem-erro"><Icone nome="alerta" />{erro}</div>
        <Link to="/solicitacoes" className="botao botao-secundario"><Icone nome="voltar" />Voltar para a lista</Link>
      </div>
    )
  }
  if (!detalhe) return <div className="pagina"><p className="carregando">Carregando…</p></div>

  const status = detalhe.status
  const etapa = indiceEtapa(status)
  const finalizada = FINAIS.includes(status)
  const interrompida = status === 'CANCELADO' || status === 'EXPIRADO'
  const aguardando = status === 'AGUARDANDO_DOCUMENTO'
  const numero = resumo?.solicitacao || 'Solicitação'

  return (
    <div className="pagina">
      <Cabecalho trilha={[{ label: 'Solicitações', para: '/solicitacoes' }, { label: numero }]} titulo={numero}>
        <Link to="/solicitacoes" className="botao botao-secundario"><Icone nome="voltar" tamanho={16} />Voltar para a lista</Link>
      </Cabecalho>

      {/* Resumo */}
      <dl className="painel resumo">
        <div><dt>Status</dt><dd><StatusBadge status={status} /></dd></div>
        <div><dt>Criada em</dt><dd>{dataHora(resumo?.dataCriacao)}</dd></div>
        <div><dt>Documentos</dt><dd className="tabular">{resumo ? `${resumo.documentosRecebidos} de ${resumo.documentosEsperados}` : '—'}</dd></div>
        <div>
          <dt>Prazo de envio</dt>
          <dd>{aguardando && resumo?.prazoDocumento
            ? <span className="prazo"><Icone nome="relogio" tamanho={16} /><span className="tabular">{tempoRestante(resumo.prazoDocumento, agora)}</span></span>
            : '—'}</dd>
        </div>
        <div className="resumo-id"><dt>ID</dt><dd className="mono">{id}</dd></div>
      </dl>

      {/* Etapas */}
      {!interrompida && (
        <ol className="painel etapas">
          {ETAPAS.map((nome, i) => {
            const concluida = i < etapa || (finalizada && i === etapa)
            const atual = i === etapa && !finalizada
            const reprovada = i === 3 && status === 'REPROVADO'
            const rotulo = i === 3 && finalizada ? STATUS[status].label : nome
            return (
              <li key={nome} className={`etapa${concluida ? ' concluida' : ''}${atual ? ' atual' : ''}${reprovada ? ' reprovada' : ''}`}>
                <span className="etapa-marcador">
                  {concluida ? <Icone nome={reprovada ? 'x' : 'check'} tamanho={14} /> : i + 1}
                </span>
                <span className="etapa-nome">{rotulo}</span>
              </li>
            )
          })}
        </ol>
      )}

      {interrompida && (
        <div className="mensagem mensagem-neutra">
          <Icone nome="alerta" />
          Solicitação {STATUS[status].label.toLowerCase()}. O número da solicitação e o CNPJ ficaram disponíveis para uma nova solicitação.
        </div>
      )}
      {(status === 'APROVADO' || status === 'REPROVADO') && (
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
        {/* Documentos (só a situação de cada um) */}
        <section className="painel">
          <div className="painel-cabecalho">
            <h2>Documentos</h2>
            <span className="painel-info">Enviados pela API</span>
          </div>

          <ul className="documentos">
            {resumo?.arquivos.map((a) => (
              <li key={a.nomeArquivo}>
                <Icone nome="arquivo" />
                <span className="documento-nome">{a.nomeArquivo}</span>
                {a.recebido
                  ? <span className="documento-situacao recebido"><Icone nome="check" tamanho={14} />Recebido</span>
                  : <span className="documento-situacao">Pendente</span>}
              </li>
            ))}
          </ul>

          {status === 'RECEBIDO' && <p className="painel-nota">O envio de documentos é liberado em alguns segundos.</p>}
          {aguardando && <p className="painel-nota">Aguardando o envio dos documentos pendentes.</p>}
        </section>

        {/* Histórico */}
        <section className="painel">
          <div className="painel-cabecalho">
            <h2>Histórico</h2>
            {atualizadoEm && <span className="painel-info">Atualizado às {hora(atualizadoEm)}</span>}
          </div>
          <ol className="historico">
            {detalhe.historico.map((ev, i) => (
              <li key={i}>
                <time className="tabular" title={dataHora(ev.dataHora)}>{hora(ev.dataHora)}</time>
                <div>
                  <StatusBadge status={ev.status} />
                  <p>{ev.descricao}</p>
                </div>
              </li>
            ))}
          </ol>
        </section>
      </div>
    </div>
  )
}
