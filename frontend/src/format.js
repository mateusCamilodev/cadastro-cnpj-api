// Textos e formatos usados nas telas

export const STATUS = {
  RECEBIDO: { label: 'Recebido', tom: 'neutro' },
  AGUARDANDO_DOCUMENTO: { label: 'Aguardando documentos', tom: 'info' },
  EM_PROCESSAMENTO: { label: 'Em análise', tom: 'roxo' },
  APROVADO: { label: 'Aprovado', tom: 'sucesso' },
  REPROVADO: { label: 'Reprovado', tom: 'erro' },
  CANCELADO: { label: 'Cancelado', tom: 'apagado' },
  EXPIRADO: { label: 'Expirado', tom: 'apagado' },
}

export const FINAIS = ['APROVADO', 'REPROVADO', 'CANCELADO', 'EXPIRADO']

// A API devolve datas sem fuso (LocalDateTime) → o navegador lê como horário local
export function dataHora(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export function hora(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export function horaCurta(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

export function dataCurta(iso) {
  return iso ? new Date(iso).toLocaleDateString('pt-BR') : null
}

// "mm:ss" até o instante iso (00:00 se já passou)
export function tempoRestante(iso, agora) {
  const ms = new Date(iso).getTime() - agora
  if (ms <= 0) return '00:00'
  const s = Math.floor(ms / 1000)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

// Segundos que faltam até iso (negativo se já passou)
export function segundosAte(iso, agora) {
  return (new Date(iso).getTime() - agora) / 1000
}

// "agora mesmo", "há 5 min", "há 2 h", "há 3 d"
export function tempoAtras(iso, agora) {
  if (!iso) return '—'
  const s = Math.max(0, (agora - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'agora mesmo'
  if (s < 3600) return `há ${Math.floor(s / 60)} min`
  if (s < 86400) return `há ${Math.floor(s / 3600)} h`
  return `há ${Math.floor(s / 86400)} d`
}

// Urgência do prazo de envio: só usa cor de alerta quando falta pouco de verdade
export function urgenciaPrazo(segundos) {
  if (segundos <= 0) return 'vencido'
  if (segundos <= 30) return 'critico'
  if (segundos <= 120) return 'atencao'
  return 'normal'
}
