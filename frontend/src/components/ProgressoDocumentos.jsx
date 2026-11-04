// Barrinha "x de y documentos" — azul enquanto falta documento, verde quando completo
export default function ProgressoDocumentos({ recebidos, esperados, rotulo = false }) {
  const pct = esperados ? (recebidos / esperados) * 100 : 0
  const completo = esperados > 0 && recebidos === esperados
  return (
    <span className="progresso-docs" role="img" aria-label={`${recebidos} de ${esperados} documentos recebidos`}>
      <span className="progresso-docs-barra"><span className={completo ? 'completo' : ''} style={{ width: `${pct}%` }} /></span>
      <span className="tabular">{recebidos}/{esperados}{rotulo && <span className="progresso-docs-rotulo"> {esperados === 1 ? 'documento' : 'documentos'}</span>}</span>
    </span>
  )
}
