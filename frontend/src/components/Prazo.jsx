import { useAgora } from '../hooks.js'
import { segundosAte, tempoRestante, urgenciaPrazo } from '../format.js'
import Icone from './Icone.jsx'

// Contagem regressiva do prazo de envio. A cor acompanha a urgência real:
// normal (neutro) → atenção (≤ 2 min, laranja) → crítico (≤ 30 s, vermelho) → vencido.
// O relógio fica aqui dentro: só este pedaço da tela redesenha a cada segundo.
export default function Prazo({ ate, grande = false }) {
  const agora = useAgora(1000)
  const segundos = segundosAte(ate, agora)
  const nivel = urgenciaPrazo(segundos)
  return (
    <span className={`prazo prazo-${nivel}${grande ? ' prazo-grande' : ''}`}
          title={`Prazo até ${new Date(ate).toLocaleString('pt-BR')}`}>
      <Icone nome="relogio" tamanho={grande ? 16 : 14} />
      <span className="tabular">{nivel === 'vencido' ? 'Prazo encerrado' : tempoRestante(ate, agora)}</span>
    </span>
  )
}
