import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import { useAgora } from '../../hooks.js'
import Icone from '../../components/Icone.jsx'
import { SimboloMarca } from '../../components/Marca.jsx'
import { CabecalhoQa, ErroCarga, Esqueleto, QaStatus, duracao } from '../../components/qa/QaComum.jsx'
import { CORES_STATUS } from '../../components/qa/QaGraficos.jsx'

// Execuções na fila ou rodando agora (de todos os usuários), atualizadas a cada 2 s.
// Para acompanhar cenário a cenário (tempo real via SSE), abra a execução.
export default function QaAndamento() {
  const { pode } = useAuth()
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState('')
  const [cancelando, setCancelando] = useState('')
  const agora = useAgora(1000)

  const carregar = useCallback(async () => {
    try {
      setDados(await qa.execucoes({ ativas: true, tamanho: 50 }))
      setErro('')
    } catch (e) {
      setErro(e.message)
    }
  }, [])

  useEffect(() => {
    carregar()
    const t = setInterval(carregar, 2000)
    return () => clearInterval(t)
  }, [carregar])

  async function cancelar(id) {
    setCancelando(id)
    try {
      await qa.cancelar(id)
      await carregar()
    } catch (e) {
      setErro(e.message)
    } finally {
      setCancelando('')
    }
  }

  return (
    <div className="pagina qa-pagina">
      <CabecalhoQa titulo="Em andamento" descricao="Execuções na fila ou rodando agora. A lista se atualiza sozinha.">
        {pode('qa.execute') && <Link to="/qa/nova" className="botao botao-primario"><Icone nome="play" tamanho={16} />Nova execução</Link>}
      </CabecalhoQa>

      {erro && <ErroCarga erro={erro} tentarDeNovo={carregar} />}
      {!dados && !erro && <div className="painel qa-bloco"><Esqueleto linhas={4} altura={28} /></div>}

      {dados && dados.itens.length === 0 && (
        <div className="painel qa-vazio">
          <SimboloMarca tamanho={52} />
          <strong>Nada rodando agora</strong>
          <span>Quando uma execução começar, ela aparece aqui com o progresso em tempo real.</span>
          <Link to="/qa/execucoes" className="qa-link">Ver o histórico</Link>
        </div>
      )}

      <div className="qa-andamento">
        {dados?.itens.map((e) => {
          const pct = e.total ? Math.round((e.concluidos / e.total) * 100) : 0
          const decorrido = e.iniciadaEm ? agora - new Date(e.iniciadaEm).getTime() : null
          return (
            <article key={e.id} className="painel qa-andamento-item">
              <header>
                <Link to={`/qa/execucoes/${e.id}`} className="mono qa-id-link">#{e.id.slice(0, 8)}</Link>
                <QaStatus status={e.status} tipo="execucao" />
                {e.suiteNome && <span className="qa-tag-neutra">{e.suiteNome}</span>}
                <span className="suave">{e.ambienteRotulo} · {e.usuario}</span>
                <span className="qa-andamento-tempo tabular">
                  <Icone nome="relogio" tamanho={14} />{decorrido === null ? 'na fila' : duracao(Math.max(0, decorrido))}
                </span>
              </header>
              <div className="qa-progresso" role="progressbar" aria-valuemin={0} aria-valuemax={e.total} aria-valuenow={e.concluidos}
                   aria-label={`Progresso da execução ${e.id.slice(0, 8)}`}>
                {CORES_STATUS.filter((c) => e[c.chave] > 0).map((c) => <span key={c.chave} style={{ flex: e[c.chave], background: c.cor }} />)}
                {e.total - e.concluidos > 0 && <span className="qa-progresso-resto" style={{ flex: e.total - e.concluidos }} />}
              </div>
              <footer>
                <span className="tabular"><b>{pct}%</b> · {e.concluidos} de {e.total} · {e.total - e.concluidos} pendente(s)</span>
                <span className="qa-andamento-contadores">
                  {CORES_STATUS.filter((c) => e[c.chave] > 0).map((c) => (
                    <span key={c.chave}><span className="grafico-legenda-cor" style={{ background: c.cor }} />{c.rotulo} <b className="tabular">{e[c.chave]}</b></span>
                  ))}
                </span>
                <span className="qa-andamento-acoes">
                  {pode('qa.cancel') && (
                    <button type="button" className="botao botao-fantasma botao-compacto" disabled={cancelando === e.id || !!e.canceladaPor}
                            onClick={() => cancelar(e.id)}>
                      {e.canceladaPor ? 'Cancelando…' : 'Cancelar'}
                    </button>
                  )}
                  <Link to={`/qa/execucoes/${e.id}`} className="botao botao-secundario botao-compacto">Acompanhar</Link>
                </span>
              </footer>
            </article>
          )
        })}
      </div>
    </div>
  )
}
