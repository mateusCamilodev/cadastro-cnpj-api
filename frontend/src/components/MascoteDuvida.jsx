// Boneco em dúvida (pixel art): balança a cabeça dizendo "não" e tem um "?" flutuando em cima.
// Usado quando a busca/filtro da lista não encontra nada. Animação em CSS (classes duvida-*).

import { PIXEL } from '../pixel.js'

const COR = { corpo: PIXEL.corpo, olho: PIXEL.olho, interrogacao: PIXEL.tinta }

// "?" em pixels (5 de largura x 7 de altura), desenhado a partir de x=14, y=0
const INTERROGACAO = [
  [15, 0], [16, 0], [17, 0],
  [14, 1], [18, 1],
  [18, 2],
  [17, 3],
  [16, 4],
  [16, 6],
]

export default function MascoteDuvida({ tamanho = 84 }) {
  return (
    <svg className="duvida" width={Math.round(tamanho * 20 / 16)} height={tamanho} viewBox="0 0 20 16"
         shapeRendering="crispEdges" role="img" aria-label="Boneco balançando a cabeça, em dúvida">
      <g className="duvida-interrogacao" fill={COR.interrogacao}>
        {INTERROGACAO.map(([x, y]) => <rect key={`${x},${y}`} x={x} y={y} width="1.04" height="1.04" />)}
      </g>

      {/* pernas paradas; a cabeça (corpo + olhos + braços) balança */}
      <g fill={COR.corpo}>
        <rect x="3" y="13" width="1" height="2" /><rect x="5" y="13" width="1" height="2" />
        <rect x="9" y="13" width="1" height="2" /><rect x="11" y="13" width="1" height="2" />
      </g>
      <g className="duvida-cabeca">
        <rect x="2" y="8" width="11" height="5" fill={COR.corpo} />
        {/* braços erguidos, como quem diz "sei lá" */}
        <rect x="1" y="9" width="1" height="1" fill={COR.corpo} /><rect x="0" y="8" width="1" height="1" fill={COR.corpo} />
        <rect x="13" y="9" width="1" height="1" fill={COR.corpo} /><rect x="14" y="8" width="1" height="1" fill={COR.corpo} />
        <g className="duvida-olhos" fill={COR.olho}>
          <rect x="5" y="9" width="1" height="2" /><rect x="9" y="9" width="1" height="2" />
        </g>
      </g>
    </svg>
  )
}
