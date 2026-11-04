import { FINAIS, STATUS } from '../format.js'
import Icone from './Icone.jsx'

// Progresso da solicitação em 4 etapas (mesma regra de antes, só a apresentação mudou).
// Cada etapa é uma coluna do mesmo tamanho; a linha de ligação vai de marcador a marcador,
// então todas têm o mesmo comprimento. No celular vira uma lista vertical.
const ETAPAS = [
  { nome: 'Recebida', ajuda: 'Solicitação registrada' },
  { nome: 'Envio de documentos', ajuda: 'PDFs declarados na criação' },
  { nome: 'Análise', ajuda: 'Conferência campo a campo' },
  { nome: 'Resultado', ajuda: 'Aprovada ou reprovada' },
]

function indiceEtapa(status) {
  return { RECEBIDO: 0, AGUARDANDO_DOCUMENTO: 1, EM_PROCESSAMENTO: 2, APROVADO: 3, REPROVADO: 3 }[status] ?? 0
}

export default function Etapas({ status }) {
  const etapa = indiceEtapa(status)
  const finalizada = FINAIS.includes(status)

  return (
    <ol className="etapas" aria-label="Andamento da solicitação">
      {ETAPAS.map((e, i) => {
        const concluida = i < etapa || (finalizada && i === etapa)
        const atual = i === etapa && !finalizada
        const reprovada = i === 3 && status === 'REPROVADO'
        const nome = i === 3 && finalizada ? STATUS[status].label : e.nome
        const estado = reprovada ? 'reprovada' : concluida ? 'concluida' : atual ? 'atual' : 'futura'
        return (
          <li key={e.nome} className={`etapa etapa-${estado}`} aria-current={atual ? 'step' : undefined}>
            <span className="etapa-marcador">
              {concluida ? <Icone nome={reprovada ? 'x' : 'check'} tamanho={14} /> : <span className="tabular">{i + 1}</span>}
            </span>
            <span className="etapa-textos">
              <span className="etapa-nome">{nome}</span>
              <span className="etapa-ajuda">{atual ? 'Etapa atual' : i === 3 && finalizada ? 'Análise concluída' : e.ajuda}</span>
            </span>
          </li>
        )
      })}
    </ol>
  )
}
