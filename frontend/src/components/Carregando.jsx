// Indicador de carregamento único do portal (anel girando + texto opcional)
export default function Carregando({ texto = 'Carregando…', pequeno = false }) {
  return (
    <span className={`carregando${pequeno ? ' carregando-pequeno' : ''}`} role="status">
      <span className="carregando-anel" aria-hidden="true" />
      {texto && <span>{texto}</span>}
    </span>
  )
}
