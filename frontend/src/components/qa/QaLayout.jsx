import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { qa } from '../../api.js'
import { useAuth } from '../../auth.jsx'
import Icone from '../Icone.jsx'

// Estrutura do QA Center: menu lateral próprio (separado do portal de solicitações) + conteúdo.
// Cada item aparece conforme a permissão; o backend barra de qualquer forma (403).
const GRUPOS = [
  { titulo: null, itens: [{ para: '/qa', label: 'Dashboard', icone: 'painel', fim: true }] },
  {
    titulo: 'Catálogo',
    itens: [
      { para: '/qa/endpoints', label: 'Endpoints', icone: 'lista' },
      { para: '/qa/cenarios', label: 'Cenários', icone: 'arquivo' },
      { para: '/qa/suites', label: 'Test Suites', icone: 'pastas' },
    ],
  },
  {
    titulo: 'Execução',
    itens: [
      { para: '/qa/nova', label: 'Nova execução', icone: 'play', permissao: 'qa.execute' },
      { para: '/qa/andamento', label: 'Em andamento', icone: 'atividade', contador: true },
      { para: '/qa/execucoes', label: 'Histórico', icone: 'relogio' },
    ],
  },
  {
    titulo: 'Análise',
    itens: [{ para: '/qa/relatorios', label: 'Relatórios', icone: 'grafico', permissao: 'qa.reports.view' }],
  },
  { titulo: 'Administração', itens: [{ para: '/qa/configuracoes', label: 'Configurações', icone: 'engrenagem' }] },
]

// Quantas execuções estão rodando agora (para o selo do menu). Atualiza a cada 5 s e ao trocar de página.
function useEmAndamento() {
  const [total, setTotal] = useState(0)
  const { pathname } = useLocation()
  useEffect(() => {
    let ativo = true
    const ler = () => qa.execucoes({ ativas: true, tamanho: 1 }).then((p) => ativo && setTotal(p.total)).catch(() => {})
    ler()
    const t = setInterval(ler, 5000)
    return () => { ativo = false; clearInterval(t) }
  }, [pathname])
  return total
}

export default function QaLayout() {
  const { pode } = useAuth()
  const emAndamento = useEmAndamento()

  return (
    <div className="qa-shell">
      <aside className="qa-lateral" aria-label="Menu do QA Center">
        <div className="qa-lateral-marca">
          <Icone nome="escudo" tamanho={18} />
          <div>
            <strong>QA Center</strong>
            <span>Qualidade da API</span>
          </div>
        </div>
        <nav>
          {GRUPOS.map((g, i) => {
            const itens = g.itens.filter((it) => !it.permissao || pode(it.permissao))
            if (!itens.length) return null
            return (
              <div key={i} className="qa-lateral-grupo">
                {g.titulo && <span className="qa-lateral-titulo">{g.titulo}</span>}
                {itens.map((it) => (
                  <NavLink key={it.para} to={it.para} end={it.fim} className="qa-lateral-item">
                    <Icone nome={it.icone} tamanho={16} />
                    <span>{it.label}</span>
                    {it.contador && emAndamento > 0 && (
                      <span className="qa-lateral-selo tabular" aria-label={`${emAndamento} em andamento`}>{emAndamento}</span>
                    )}
                  </NavLink>
                ))}
              </div>
            )
          })}
        </nav>
      </aside>
      <div className="qa-principal">
        <Outlet />
      </div>
    </div>
  )
}
