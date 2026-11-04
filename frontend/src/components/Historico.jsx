import { useAgora } from '../hooks.js'
import { STATUS, dataHora, horaCurta, tempoAtras } from '../format.js'

// Linha do tempo vertical com os eventos reais da API.
// O mais recente fica em cima e destacado; cada ponto tem a cor do status do evento.
export default function Historico({ eventos }) {
  const agora = useAgora(30000)  // "há X min" não precisa atualizar a cada segundo
  const ordenados = [...(eventos || [])].reverse()

  return (
    <section className="painel" aria-labelledby="titulo-historico">
      <header className="painel-cabecalho">
        <div>
          <h2 id="titulo-historico">Histórico</h2>
          <p className="painel-sub">{ordenados.length} {ordenados.length === 1 ? 'evento' : 'eventos'}</p>
        </div>
      </header>

      <ol className="linha-tempo">
        {ordenados.map((ev, i) => {
          const tom = STATUS[ev.status]?.tom || 'neutro'
          // o nome do status só aparece quando muda em relação ao evento anterior (mais antigo)
          const mudouStatus = i === 0 || ordenados[i + 1]?.status !== ev.status
          return (
            <li key={`${ev.dataHora}-${i}`} className={`evento evento-${tom}${i === 0 ? ' evento-atual' : ''}`}>
              <span className="evento-ponto" aria-hidden="true" />
              <div className="evento-corpo">
                <div className="evento-topo">
                  <span className={`evento-status${mudouStatus ? '' : ' evento-status-repetido'}`}>{STATUS[ev.status]?.label || ev.status}</span>
                  <time dateTime={ev.dataHora} title={dataHora(ev.dataHora)}>
                    {horaCurta(ev.dataHora)} · {tempoAtras(ev.dataHora, agora)}
                  </time>
                </div>
                <p className="evento-descricao">{ev.descricao}</p>
              </div>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
