import { STATUS } from '../format.js'

// Etiqueta de status discreta: marcador quadrado + texto
export default function StatusBadge({ status }) {
  const info = STATUS[status] || { label: status, tom: 'neutro' }
  return (
    <span className={`status status-${info.tom}`}>
      <span className="status-marcador" />
      {info.label}
    </span>
  )
}
