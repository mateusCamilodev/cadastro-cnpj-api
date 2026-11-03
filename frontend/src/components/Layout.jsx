import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../auth.jsx'
import { tempoRestante } from '../format.js'
import Icone from './Icone.jsx'
import Marca from './Marca.jsx'

// Seções do portal (barra superior). Para incluir uma nova, é só adicionar um item aqui.
const SECOES = [
  { para: '/solicitacoes', icone: 'lista', label: 'Solicitações' },
]

export default function Layout() {
  const { sessao, sair } = useAuth()
  const navegar = useNavigate()
  const [agora, setAgora] = useState(Date.now())

  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])

  const inicial = sessao.email.charAt(0).toUpperCase()

  return (
    <div className="app">
      <header className="barra-topo">
        <div className="barra-topo-conteudo">
          <Marca tamanho={34} />

          <nav className="secoes" aria-label="Seções do portal">
            {SECOES.map((s) => (
              <NavLink key={s.para} to={s.para} className="secao">
                <Icone nome={s.icone} tamanho={16} />{s.label}
              </NavLink>
            ))}
          </nav>

          <div className="conta">
            <span className="conta-avatar" aria-hidden="true">{inicial}</span>
            <div className="conta-dados">
              <strong title={sessao.email}>{sessao.email}</strong>
              <span>Sessão expira em <span className="tabular">{tempoRestante(new Date(sessao.expiraEm).toISOString(), agora)}</span></span>
            </div>
            <button className="botao botao-fantasma" onClick={() => { sair(); navegar('/login') }} title="Sair">
              <Icone nome="sair" tamanho={16} /><span className="conta-sair">Sair</span>
            </button>
          </div>
        </div>
      </header>

      <main className="conteudo">
        <Outlet />
      </main>
    </div>
  )
}
