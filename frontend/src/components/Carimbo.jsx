// Carimbo de borracha (APROVADO / REPROVADO), com tinta "gasta" feita por filtro SVG.
// Aparece uma vez, "batendo" na página, quando a solicitação recebe o resultado.
export default function Carimbo({ aprovado, data }) {
  return (
    <div className={`carimbo ${aprovado ? 'carimbo-aprovado' : 'carimbo-reprovado'}`}
         role="img" aria-label={`Carimbo: ${aprovado ? 'aprovado' : 'reprovado'}${data ? ` em ${data}` : ''}`}>
      <svg width="0" height="0" aria-hidden="true" style={{ position: 'absolute' }}>
        <filter id="tinta-gasta">
          <feTurbulence type="fractalNoise" baseFrequency="0.7" numOctaves="2" seed="7" result="ruido" />
          <feColorMatrix in="ruido" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 -1.1 1.45" result="falhas" />
          <feComposite in="SourceGraphic" in2="falhas" operator="in" result="gasto" />
          <feDisplacementMap in="gasto" in2="ruido" scale="1.4" />
        </filter>
      </svg>
      <div className="carimbo-tinta">
        <span className="carimbo-palavra">{aprovado ? 'Aprovado' : 'Reprovado'}</span>
        {data && <span className="carimbo-data">{data}</span>}
      </div>
    </div>
  )
}
