import { useLayoutEffect, useRef, useState } from 'react'

// Gráficos do QA Center em SVG puro (sem biblioteca).
// Cores de STATUS validadas com o validador de paleta (faixa de luminosidade, croma, daltonismo e contraste)
// contra a superfície dos painéis (#13254a). A ordem da pilha separa verde/âmbar e âmbar/vermelho.
// "pulados" no backend = bloqueados (pré-condição falhou); "cancelados" = não rodaram por cancelamento.
export const CORES_STATUS = [
  { chave: 'aprovados', rotulo: 'Aprovados', cor: '#2a9a5c' },
  { chave: 'pulados', rotulo: 'Bloqueados', cor: '#7b7fd0' },
  { chave: 'erros', rotulo: 'Erros', cor: '#ab8c0d' },
  { chave: 'reprovados', rotulo: 'Reprovados', cor: '#e04f7a' },
  { chave: 'cancelados', rotulo: 'Cancelados', cor: '#2f94b8' },
]
const COR_FALHA = '#e04f7a'

function Legenda({ itens }) {
  return (
    <ul className="grafico-legenda">
      {itens.map((i) => (
        <li key={i.chave}><span className="grafico-legenda-cor" style={{ background: i.cor }} />{i.rotulo}</li>
      ))}
    </ul>
  )
}

function Tabela({ colunas, linhas }) {
  return (
    <details className="grafico-tabela">
      <summary>Ver dados em tabela</summary>
      <table>
        <thead><tr>{colunas.map((c) => <th key={c}>{c}</th>)}</tr></thead>
        <tbody>{linhas.map((l, i) => <tr key={i}>{l.map((v, j) => <td key={j} className="tabular">{v}</td>)}</tr>)}</tbody>
      </table>
    </details>
  )
}

// Colunas empilhadas: resultados dos testes por período (tendência)
export function GraficoTendencia({ pontos }) {
  const [foco, setFoco] = useState(null)
  // Desenha na largura real do container (textos do eixo ficam sempre em 11px, sem esticar)
  const [L, setL] = useState(640)
  const area = useRef(null)
  useLayoutEffect(() => {
    const el = area.current
    if (!el) return
    const medir = () => setL(Math.max(280, Math.round(el.clientWidth)))
    medir()
    const obs = new ResizeObserver(medir)
    obs.observe(el)
    return () => obs.disconnect()
  }, [])
  const A = 220, M = { e: 34, d: 8, t: 12, b: 26 }
  const largura = L - M.e - M.d, altura = A - M.t - M.b
  const totais = pontos.map((p) => CORES_STATUS.reduce((s, c) => s + p[c.chave], 0))
  const maximo = Math.max(4, ...totais)
  // passo "redondo" (1, 2, 5, 10, 20, 50...) que divide o eixo em no máximo 5 intervalos
  const passo = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000].find((p) => Math.ceil(maximo / p) <= 5) || 10000
  const topo = Math.ceil(maximo / passo) * passo
  const marcas = Array.from({ length: topo / passo + 1 }, (_, i) => i * passo)
  const banda = largura / pontos.length
  const barra = Math.max(4, Math.min(28, banda * 0.6))
  const y = (v) => M.t + altura - (v / topo) * altura
  const mostrarRotulo = (i) => pontos.length <= 10 || i % Math.ceil(pontos.length / 8) === 0

  return (
    <div className="grafico">
      <Legenda itens={CORES_STATUS} />
      <div className="grafico-area" ref={area}>
        <svg viewBox={`0 0 ${L} ${A}`} width={L} height={A} role="img" aria-label="Resultados dos testes por período">
          {marcas.map((v) => (
            <g key={v}>
              <line className="grafico-grade" x1={M.e} x2={L - M.d} y1={y(v)} y2={y(v)} />
              <text className="grafico-eixo" x={M.e - 8} y={y(v) + 4} textAnchor="end">{v}</text>
            </g>
          ))}
          {pontos.map((p, i) => {
            const x = M.e + i * banda + (banda - barra) / 2
            let base = 0
            const segmentos = CORES_STATUS.filter((c) => p[c.chave] > 0)
            return (
              <g key={p.rotulo + i} onMouseEnter={() => setFoco(i)} onMouseLeave={() => setFoco(null)}
                 opacity={foco === null || foco === i ? 1 : 0.45}>
                {/* área de toque maior que a barra */}
                <rect x={M.e + i * banda} y={M.t} width={banda} height={altura} fill="transparent" />
                {segmentos.map((c, k) => {
                  const h = (p[c.chave] / topo) * altura
                  const yTopo = y(base + p[c.chave])
                  base += p[c.chave]
                  const ultimo = k === segmentos.length - 1
                  const alt = Math.max(1, h - (ultimo ? 0 : 2))   // 2px de espaço entre segmentos
                  return ultimo
                    ? <path key={c.chave} fill={c.cor} d={`M${x},${yTopo + alt} V${yTopo + 4} Q${x},${yTopo} ${x + 4},${yTopo} H${x + barra - 4} Q${x + barra},${yTopo} ${x + barra},${yTopo + 4} V${yTopo + alt} Z`} />
                    : <rect key={c.chave} fill={c.cor} x={x} y={yTopo + 2} width={barra} height={alt} />
                })}
                {mostrarRotulo(i) && <text className="grafico-eixo" x={M.e + i * banda + banda / 2} y={A - 8} textAnchor="middle">{p.rotulo}</text>}
              </g>
            )
          })}
          <line className="grafico-base" x1={M.e} x2={L - M.d} y1={y(0)} y2={y(0)} />
        </svg>
        {foco !== null && (
          <div className="grafico-dica" style={{ left: `${((M.e + foco * banda + banda / 2) / L) * 100}%` }}>
            <strong>{pontos[foco].rotulo}</strong>
            <span>{pontos[foco].execucoes} execução(ões)</span>
            {CORES_STATUS.map((c) => (
              <span key={c.chave} className="grafico-dica-linha">
                <span className="grafico-legenda-cor" style={{ background: c.cor }} />{c.rotulo}: <b className="tabular">{pontos[foco][c.chave]}</b>
              </span>
            ))}
          </div>
        )}
      </div>
      <Tabela colunas={['Período', 'Execuções', ...CORES_STATUS.map((c) => c.rotulo)]}
              linhas={pontos.map((p) => [p.rotulo, p.execucoes, ...CORES_STATUS.map((c) => p[c.chave])])} />
    </div>
  )
}

// Barras horizontais: quantidade de falhas por grupo (endpoint ou categoria), com o valor ao lado
export function BarrasFalhas({ grupos, vazio = 'Nenhuma falha registrada.' }) {
  const comFalha = grupos.filter((g) => g.falhas > 0)
  if (!comFalha.length) return <p className="qa-vazio-curto">{vazio}</p>
  const maximo = Math.max(...comFalha.map((g) => g.falhas))
  return (
    <div className="grafico">
      <ul className="barras-falhas">
        {comFalha.map((g) => (
          <li key={g.chave} title={`${g.falhas} falha(s) em ${g.total} teste(s)`}>
            <span className="barras-falhas-rotulo">
              <strong>{g.nome}</strong>
              {g.detalhe && <small className="mono">{g.detalhe}</small>}
            </span>
            <span className="barras-falhas-trilho">
              <span style={{ width: `${Math.max(3, (g.falhas / maximo) * 100)}%`, background: COR_FALHA }} />
            </span>
            <span className="barras-falhas-valor tabular">{g.falhas}<small> de {g.total}</small></span>
          </li>
        ))}
      </ul>
      <Tabela colunas={['Grupo', 'Falhas', 'Testes']} linhas={grupos.map((g) => [g.nome, g.falhas, g.total])} />
    </div>
  )
}

// Barra única de distribuição (aprovados/pulados/erros/reprovados de um conjunto)
export function BarraDistribuicao({ valores }) {
  const total = CORES_STATUS.reduce((s, c) => s + (valores[c.chave] || 0), 0)
  if (!total) return <p className="qa-vazio-curto">Sem testes no período.</p>
  return (
    <div className="distribuicao">
      <div className="distribuicao-barra" role="img"
           aria-label={CORES_STATUS.map((c) => `${c.rotulo}: ${valores[c.chave] || 0}`).join(', ')}>
        {CORES_STATUS.filter((c) => valores[c.chave] > 0).map((c) => (
          <span key={c.chave} style={{ flex: valores[c.chave], background: c.cor }} title={`${c.rotulo}: ${valores[c.chave]}`} />
        ))}
      </div>
      <ul className="distribuicao-legenda">
        {CORES_STATUS.map((c) => (
          <li key={c.chave}>
            <span className="grafico-legenda-cor" style={{ background: c.cor }} />
            {c.rotulo}<b className="tabular">{valores[c.chave] || 0}</b>
            <small className="tabular">{Math.round(((valores[c.chave] || 0) / total) * 100)}%</small>
          </li>
        ))}
      </ul>
    </div>
  )
}
