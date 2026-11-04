// Mascote em pixel art.
// humor="feliz"  → braços pra cima, bochechas e brilhos (aprovado)
// humor="triste" → braços caídos, olhos baixos, lágrima e nuvem de chuva (reprovado)

import { PIXEL } from '../pixel.js'

const CORES = { o: PIXEL.corpo, k: PIXEL.olho, r: PIXEL.bochecha, t: PIXEL.lagrima }
const PX = 10            // tamanho de cada "pixel" no desenho
const X0 = 30, Y0 = 44   // onde o boneco começa dentro do quadro

function pixels(humor) {
  const p = []
  const add = (x, y, c) => p.push([x, y, c])
  for (let y = 2; y <= 6; y++) for (let x = 2; x <= 12; x++) add(x, y, 'o')   // corpo
  for (const x of [3, 5, 9, 11]) { add(x, 7, 'o'); add(x, 8, 'o') }          // pernas, em pares

  if (humor === 'feliz') {
    add(0, 0, 'o'); add(1, 1, 'o'); add(14, 0, 'o'); add(13, 1, 'o')         // braços pra cima
    for (const x of [5, 9]) { add(x, 3, 'k'); add(x, 4, 'k') }               // olhos
    add(4, 5, 'r'); add(10, 5, 'r')                                           // bochechas
  } else {
    add(1, 5, 'o'); add(0, 6, 'o'); add(13, 5, 'o'); add(14, 6, 'o')         // braços caídos
    for (const x of [5, 9]) add(x, 4, 'k')                                    // olhos baixos
    add(6, 3, 'k'); add(8, 3, 'k')                                            // sobrancelhas tristes (pontas de dentro pra cima)
    add(5, 5, 't'); add(5, 6, 't')                                            // lágrima
  }
  return p
}

function Brilho({ x, y, cor }) {
  return (
    <g className="mascote-brilho" fill={cor}>
      <rect x={x} y={y} width="6" height="6" />
      <rect x={x - 6} y={y} width="6" height="6" opacity=".45" />
      <rect x={x + 6} y={y} width="6" height="6" opacity=".45" />
      <rect x={x} y={y - 6} width="6" height="6" opacity=".45" />
      <rect x={x} y={y + 6} width="6" height="6" opacity=".45" />
    </g>
  )
}

export default function Mascote({ humor }) {
  const feliz = humor === 'feliz'
  return (
    <svg viewBox="0 0 210 150" className={`mascote mascote-${feliz ? 'feliz' : 'triste'}`} shapeRendering="crispEdges"
         role="img" aria-label={feliz ? 'Mascote comemorando' : 'Mascote triste'}>
      {feliz ? (
        <>
          <Brilho x={14} y={22} cor={PIXEL.tinta} />
          <Brilho x={186} y={14} cor={PIXEL.brilho} />
          <Brilho x={192} y={70} cor={PIXEL.tinta} />
        </>
      ) : (
        <g className="mascote-nuvem">
          <g fill={PIXEL.nuvem}><rect x="78" y="10" width="52" height="10" /><rect x="88" y="0" width="32" height="10" /><rect x="68" y="20" width="72" height="10" /></g>
          <g fill={PIXEL.lagrima}><rect className="gota" x="82" y="36" width="4" height="8" /><rect className="gota gota-2" x="102" y="34" width="4" height="8" /><rect className="gota gota-3" x="122" y="36" width="4" height="8" /></g>
        </g>
      )}
      <g className="mascote-corpo">
        {pixels(humor).map(([x, y, c], i) => (
          <rect key={i} x={X0 + x * PX} y={Y0 + y * PX} width={PX + 0.4} height={PX + 0.4} fill={CORES[c]} />
        ))}
      </g>
    </svg>
  )
}
