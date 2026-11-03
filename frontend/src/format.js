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

export function tempoRestante(iso, agora) {
  const ms = new Date(iso).getTime() - agora
  if (ms <= 0) return '00:00'
  const s = Math.floor(ms / 1000)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}
