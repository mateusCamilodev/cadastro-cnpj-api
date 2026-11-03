// Logotipo animado em pixel art: o boneco carimba uma folha.
// A cada ciclo de 6s ele carimba duas vezes: primeiro APROVA (✓ verde), depois REPROVA (✗ vermelho).
// A animação é toda em CSS (classes logo-*, em styles.css) e para em "prefers-reduced-motion".

const COR = {
  corpo: '#d97757',
  olho: '#141414',
  folha: '#e8f4f8',
  linha: '#9aabb3',
  cabo: '#c4cdd3',
  tinta: '#4d8dff',
  ok: '#5ee08f',
  erro: '#ff6b7a',
}

// [x, y] de cada "pixel" (1 unidade do viewBox)
const CORPO = [
  ...Array.from({ length: 11 * 5 }, (_, i) => [i % 11, 8 + Math.floor(i / 11)]),   // corpo 11x5
  [1, 13], [1, 14], [3, 13], [3, 14], [7, 13], [7, 14], [9, 13], [9, 14],   // pernas, em pares
]
const OLHOS = [[3, 9], [3, 10], [7, 9], [7, 10]]
const BRACO = [[10, 7], [11, 7], [11, 6], [12, 6], [12, 5], [13, 5], [13, 4], [14, 4], [14, 3], [15, 3]]   // braço grosso até o carimbo
const BASE = Array.from({ length: 6 * 2 }, (_, i) => [15 + (i % 6), 1 + Math.floor(i / 6)])
const CABO = [[17, -1], [18, -1], [17, 0], [18, 0]]
const VISTO = [[15, 10], [16, 11], [17, 12], [18, 11], [19, 10], [20, 9]]
const XIS = [[15, 8], [16, 9], [17, 10], [18, 11], [19, 12], [19, 8], [18, 9], [16, 11], [15, 12]]

function Pixels({ lista, cor }) {
  return lista.map(([x, y]) => <rect key={`${x},${y}`} x={x} y={y} width="1.04" height="1.04" fill={cor} />)
}

export function SimboloMarca({ tamanho = 32 }) {
  return (
    <svg className="logo" width={Math.round(tamanho * 23 / 16)} height={tamanho} viewBox="0 -1 23 16"
         shapeRendering="crispEdges" aria-hidden="true">
      {/* folha */}
      <rect x="14" y="5" width="8" height="10" fill={COR.folha} />
      <Pixels lista={[[15, 14], [16, 14], [17, 14], [18, 14]]} cor={COR.linha} />

      {/* marcas deixadas pelo carimbo */}
      <g className="logo-visto"><Pixels lista={VISTO} cor={COR.ok} /></g>
      <g className="logo-xis"><Pixels lista={XIS} cor={COR.erro} /></g>

      {/* boneco */}
      <Pixels lista={CORPO} cor={COR.corpo} />
      <Pixels lista={OLHOS} cor={COR.olho} />

      {/* braço + carimbo: descem juntos até a folha */}
      <g className="logo-carimbo">
        <Pixels lista={BRACO} cor={COR.corpo} />
        <Pixels lista={CABO} cor={COR.cabo} />
        <Pixels lista={BASE} cor={COR.tinta} />
      </g>
    </svg>
  )
}

// Símbolo + nome; o subtítulo fica embaixo do nome, os dois alinhados ao centro do boneco
export default function Marca({ tamanho = 44 }) {
  return (
    <div className="marca" aria-label="Carimbô — Portal de solicitações">
      <SimboloMarca tamanho={tamanho} />
      <span className="marca-texto">
        <span className="marca-nome">
          Carimbô
          <svg className="marca-check" viewBox="0 0 24 24" aria-hidden="true"><path d="M4.5 12.5l5 5L19.5 7" /></svg>
        </span>
        <span className="marca-sub">Portal de solicitações</span>
      </span>
    </div>
  )
}
