import { useEffect, useState } from 'react'

// Papel de parede animado (fica atrás de tudo, escurecido e sem receber cliques):
//  1) Pistas: bonecos correm da esquerda para a direita e carimbam os documentos
//     do caminho — o ✓ ou o ✗ aparece exatamente quando o carimbo bate na folha.
//  2) Folhas soltas subindo devagar, umas aprovadas, outras reprovadas.
// Variantes: "completa" (login: 3 pistas e todas as folhas) e "discreta" (telas internas:
// só a pista da frente e poucas folhas, mais apagadas, para não competir com o conteúdo).
// Não há palavras APROVADO/REPROVADO no fundo: elas contradiziam o status real da solicitação.
// A animação pausa quando a aba do navegador fica escondida.
// Tudo em CSS (transform/opacity); com "reduzir movimento" no sistema, a cena fica parada.

import { PIXEL as COR } from '../pixel.js'

const VISTO = [[1, 4], [2, 5], [3, 6], [4, 5], [5, 4], [6, 3]]
const XIS = [[2, 2], [3, 3], [4, 4], [5, 5], [6, 6], [6, 2], [5, 3], [3, 5], [2, 6]]

// Pistas: altura na tela, tamanho do "pixel" (mais longe = menor), duração da corrida,
// posição de cada documento (fração da largura) e o resultado de cada um
const PISTAS = [
  { topo: '32%', px: 3, duracao: 26, atraso: -9, docs: [[0.08, 1], [0.27, 0], [0.46, 1], [0.64, 1], [0.83, 0]] },
  { topo: '58%', px: 4, duracao: 20, atraso: -3, docs: [[0.14, 0], [0.37, 1], [0.6, 1], [0.86, 0]] },
  { topo: '84%', px: 5, duracao: 16, atraso: -12, docs: [[0.2, 1], [0.52, 0], [0.78, 1]] },
]

const FOLHAS = [
  { x: 4, t: 9, dur: 46, atraso: -5, giro: -14, ok: 1 }, { x: 15, t: 6, dur: 58, atraso: -31, giro: 10, ok: 0 },
  { x: 26, t: 11, dur: 50, atraso: -18, giro: -6, ok: 1 }, { x: 38, t: 7, dur: 62, atraso: -44, giro: 18, ok: 1 },
  { x: 51, t: 10, dur: 54, atraso: -9, giro: -20, ok: 0 }, { x: 63, t: 6, dur: 48, atraso: -27, giro: 8, ok: 1 },
  { x: 74, t: 12, dur: 60, atraso: -51, giro: -10, ok: 0 }, { x: 86, t: 8, dur: 52, atraso: -14, giro: 14, ok: 1 },
  { x: 94, t: 9, dur: 57, atraso: -38, giro: -16, ok: 0 }, { x: 45, t: 5, dur: 66, atraso: -58, giro: 22, ok: 1 },
]

const pct = (n) => `${Math.min(100, Math.max(0, n)).toFixed(2)}%`

function Marca({ ok }) {
  return (ok ? VISTO : XIS).map(([x, y]) => <rect key={`${x}${y}`} x={x} y={y} width="1.05" height="1.05" fill={ok ? COR.ok : COR.erro} />)
}

function Folha({ ok, className, style }) {
  return (
    <svg className={className} style={style} viewBox="0 0 8 10" shapeRendering="crispEdges" aria-hidden="true">
      <rect width="8" height="10" fill={COR.folha} />
      <rect x="1" y="8" width="4" height="1" fill={COR.linha} />
      {ok !== undefined && <Marca ok={ok} />}
    </svg>
  )
}

function Pista({ pista, i, largura }) {
  const S = pista.px
  const inicio = -20 * S
  const fim = largura + 2 * S
  // u = quanto da corrida (em %) o boneco anda para avançar 1 "pixel" do desenho
  const u = (S / (fim - inicio)) * 100
  // Momento (em %) em que o carimbo (centro em x=14,5 do boneco) fica sobre a folha.
  // Nesse ponto o corpo (x 0..11) termina 0,5 pixel antes da folha: carimba sem encostar nela.
  const momentos = pista.docs.map(([p]) => ((p * largura + 3 * S - 14.5 * S - inicio) / (fim - inicio)) * 100)

  // Logo depois de carimbar, o boneco pula por cima da folha (ela tem 8 de largura e 10 de altura)
  const quadros = (lista) => `0%, 100% { transform: translateY(0); } ${lista.join(' ')}`
  const css = [
    `@keyframes fundo-correr-${i} { from { transform: translateX(${inicio}px); } to { transform: translateX(${fim}px); } }`,
    `@keyframes fundo-carimbar-${i} { ${quadros(momentos.map((m) =>
      `${pct(m - 2.5 * u)} { transform: translateY(0); } ${pct(m)} { transform: translateY(4px); } ${pct(m + 1.5 * u)} { transform: translateY(0); }`))} }`,
    `@keyframes fundo-pular-${i} { ${quadros(momentos.map((m) =>
      `${pct(m + 0.5 * u)} { transform: translateY(0); } ${pct(m + 10 * u)} { transform: translateY(-11px); } ${pct(m + 20 * u)} { transform: translateY(0); }`))} }`,
    ...momentos.map((m, d) => `@keyframes fundo-marca-${i}-${d} { 0%, ${pct(m - 0.2 * u)} { opacity: 0; } ${pct(m)}, 97% { opacity: 1; } 100% { opacity: 0; } }`),
  ].join('\n')

  const tempo = (nome) => ({ animation: `${nome} ${pista.duracao}s linear ${pista.atraso}s infinite` })

  return (
    <div className="fundo-pista" style={{ top: pista.topo, height: 16 * S }}>
      <style>{css}</style>
      <div className="fundo-chao" style={{ top: 15 * S }} />

      {pista.docs.map(([p, ok], d) => (
        <svg key={d} className="fundo-doc" style={{ left: p * largura, top: 6 * S, width: 8 * S, height: 10 * S }}
             viewBox="0 0 8 10" shapeRendering="crispEdges" aria-hidden="true">
          <rect width="8" height="10" fill={COR.folha} />
          <rect x="1" y="8" width="4" height="1" fill={COR.linha} />
          <g style={tempo(`fundo-marca-${i}-${d}`)}><Marca ok={ok} /></g>
        </svg>
      ))}

      <svg className="fundo-boneco" style={{ width: 17 * S, height: 16 * S, ...tempo(`fundo-correr-${i}`) }}
           viewBox="0 0 17 16" shapeRendering="crispEdges" aria-hidden="true">
        <g style={tempo(`fundo-pular-${i}`)}>
        <rect x="0" y="8" width="11" height="5" fill={COR.corpo} />
        <rect x="5" y="9" width="1" height="2" fill={COR.olho} />
        <rect x="8" y="9" width="1" height="2" fill={COR.olho} />
        <g className="fundo-passo-a" fill={COR.corpo}>
          <rect x="1" y="13" width="1" height="2" /><rect x="3" y="13" width="1" height="2" />
          <rect x="7" y="13" width="1" height="2" /><rect x="9" y="13" width="1" height="2" />
        </g>
        <g className="fundo-passo-b" fill={COR.corpo}>
          <rect x="0" y="13" width="1" height="2" /><rect x="4" y="13" width="1" height="2" />
          <rect x="6" y="13" width="1" height="2" /><rect x="10" y="13" width="1" height="2" />
        </g>
        <g style={tempo(`fundo-carimbar-${i}`)}>
          <rect x="11" y="7" width="1" height="1" fill={COR.corpo} />
          <rect x="12" y="6" width="1" height="1" fill={COR.corpo} />
          <rect x="12" y="5" width="1" height="1" fill={COR.corpo} />
          <rect x="13" y="0" width="3" height="1" fill={COR.cabo} />
          <rect x="14" y="1" width="1" height="2" fill={COR.cabo} />
          <rect x="12" y="3" width="5" height="2" fill={COR.tinta} />
        </g>
        </g>
      </svg>
    </div>
  )
}

export default function FundoAnimado({ variante = 'completa' }) {
  const [largura, setLargura] = useState(() => window.innerWidth)
  const [escondido, setEscondido] = useState(() => document.hidden)

  useEffect(() => {
    const aoMudarVisibilidade = () => setEscondido(document.hidden)
    document.addEventListener('visibilitychange', aoMudarVisibilidade)
    return () => document.removeEventListener('visibilitychange', aoMudarVisibilidade)
  }, [])

  const discreta = variante === 'discreta'
  const pistas = discreta ? PISTAS.slice(-1) : PISTAS
  const folhas = discreta ? FOLHAS.filter((_, i) => i % 3 === 0) : FOLHAS

  useEffect(() => {
    let espera
    const aoMudar = () => { clearTimeout(espera); espera = setTimeout(() => setLargura(window.innerWidth), 200) }
    window.addEventListener('resize', aoMudar)
    return () => { window.removeEventListener('resize', aoMudar); clearTimeout(espera) }
  }, [])

  return (
    <div className={`fundo fundo-${variante}${escondido ? ' fundo-pausado' : ''}`} aria-hidden="true">
      {folhas.map((f, i) => (
        <Folha key={i} ok={f.ok} className="fundo-folha"
               style={{ left: `${f.x}%`, width: f.t * 4, height: f.t * 5, '--giro': `${f.giro}deg`, animationDuration: `${f.dur}s`, animationDelay: `${f.atraso}s` }} />
      ))}

      {pistas.map((p, i) => <Pista key={p.topo} pista={p} i={i} largura={largura} />)}
    </div>
  )
}
