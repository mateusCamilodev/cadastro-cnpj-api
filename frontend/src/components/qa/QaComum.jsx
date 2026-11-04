import { useEffect, useRef } from 'react'
import Icone from '../Icone.jsx'

// Peças compartilhadas do QA Center (mesmos tokens e linguagem visual do resto do Carimbô)

// ===== Estados =====

export const STATUS_TESTE = {
  PENDING: { label: 'Na fila', tom: 'neutro' },
  RUNNING: { label: 'Executando', tom: 'info' },
  PASSED: { label: 'Aprovado', tom: 'sucesso' },
  FAILED: { label: 'Reprovado', tom: 'erro' },
  SKIPPED: { label: 'Bloqueado', tom: 'roxo' },
  ERROR: { label: 'Erro', tom: 'alerta' },
  CANCELLED: { label: 'Cancelado', tom: 'apagado' },
}

export const STATUS_EXECUCAO = {
  PENDING: { label: 'Na fila', tom: 'neutro' },
  RUNNING: { label: 'Em execução', tom: 'info' },
  COMPLETED: { label: 'Concluída', tom: 'sucesso' },
  CANCELLED: { label: 'Cancelada', tom: 'apagado' },
  ERROR: { label: 'Falhou', tom: 'erro' },
}

// Selo de status; "rodando" mostra o anel girando (só enquanto há trabalho de verdade)
export function QaStatus({ status, tipo = 'teste' }) {
  const mapa = tipo === 'execucao' ? STATUS_EXECUCAO : STATUS_TESTE
  const info = mapa[status] || { label: status, tom: 'neutro' }
  const rodando = status === 'RUNNING'
  return (
    <span className={`status status-${info.tom}`}>
      {rodando ? <span className="carregando-anel qa-anel" aria-hidden="true" /> : <span className="status-marcador" />}
      {info.label}
    </span>
  )
}

export function MetodoBadge({ metodo }) {
  return <span className={`metodo metodo-${(metodo || '').toLowerCase()}`}>{metodo}</span>
}

// ===== Formatação =====

export function duracao(ms) {
  if (ms === null || ms === undefined) return '—'
  if (ms < 1000) return `${ms} ms`
  const s = ms / 1000
  if (s < 60) return `${s.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} s`
  return `${Math.floor(s / 60)} min ${Math.round(s % 60)} s`
}

export function dataHoraCurta(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// ===== Estrutura das páginas =====

export function CabecalhoQa({ titulo, descricao, children }) {
  return (
    <header className="qa-cabecalho">
      <div>
        <p className="qa-sobretitulo">QA Center</p>
        <h1>{titulo}</h1>
        {descricao && <p className="qa-descricao">{descricao}</p>}
      </div>
      {children && <div className="qa-acoes">{children}</div>}
    </header>
  )
}

// Esqueleto de carregamento (linhas pulsando no formato do conteúdo)
export function Esqueleto({ linhas = 4, altura = 18 }) {
  return (
    <div className="esqueleto" role="status" aria-label="Carregando">
      {Array.from({ length: linhas }, (_, i) => (
        <span key={i} className="esqueleto-linha" style={{ height: altura, width: `${92 - (i % 3) * 14}%` }} />
      ))}
    </div>
  )
}

// Opções de seleção usadas em mais de uma tela
export const CRITICIDADE = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' }

export function Kpi({ rotulo, valor, detalhe, destaque }) {
  return (
    <div className={`kpi${destaque ? ` kpi-${destaque}` : ''}`}>
      <span className="kpi-rotulo">{rotulo}</span>
      <strong className="kpi-valor tabular">{valor}</strong>
      {detalhe && <span className="kpi-detalhe">{detalhe}</span>}
    </div>
  )
}

export function ErroCarga({ erro, tentarDeNovo }) {
  return (
    <div className="mensagem mensagem-erro" role="alert">
      <Icone nome="alerta" />
      <span>{erro}</span>
      {tentarDeNovo && <button type="button" className="botao botao-secundario botao-compacto qa-tentar" onClick={tentarDeNovo}>Tentar de novo</button>}
    </div>
  )
}

// ===== Diálogo e painel lateral (fecham com Esc; foco vai para dentro ao abrir) =====

function useFecharComEsc(aberto, fechar) {
  useEffect(() => {
    if (!aberto) return
    const aoTeclar = (e) => { if (e.key === 'Escape') fechar() }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [aberto, fechar])
}

export function Dialogo({ aberto, titulo, children, aoFechar, acoes }) {
  const ref = useRef(null)
  useFecharComEsc(aberto, aoFechar)
  useEffect(() => { if (aberto) ref.current?.focus() }, [aberto])
  if (!aberto) return null
  return (
    <div className="sobreposicao" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <div className="dialogo" role="dialog" aria-modal="true" aria-labelledby="dialogo-titulo" tabIndex={-1} ref={ref}>
        <h2 id="dialogo-titulo">{titulo}</h2>
        <div className="dialogo-corpo">{children}</div>
        <div className="dialogo-acoes">{acoes}</div>
      </div>
    </div>
  )
}

export function PainelLateral({ aberto, titulo, subtitulo, children, aoFechar, largura = 560 }) {
  const ref = useRef(null)
  useFecharComEsc(aberto, aoFechar)
  useEffect(() => { if (aberto) ref.current?.focus() }, [aberto])
  if (!aberto) return null
  return (
    <div className="sobreposicao sobreposicao-lateral" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <aside className="painel-lateral" style={{ width: `min(${largura}px, 100%)` }} role="dialog" aria-modal="true"
             aria-labelledby="painel-lateral-titulo" tabIndex={-1} ref={ref}>
        <header className="painel-lateral-topo">
          <div>
            <h2 id="painel-lateral-titulo">{titulo}</h2>
            {subtitulo && <p>{subtitulo}</p>}
          </div>
          <button type="button" className="botao-icone-qa" onClick={aoFechar} aria-label="Fechar"><Icone nome="x" tamanho={18} /></button>
        </header>
        <div className="painel-lateral-corpo">{children}</div>
      </aside>
    </div>
  )
}
