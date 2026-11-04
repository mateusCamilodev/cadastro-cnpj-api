import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { useAgora } from '../hooks.js'
import { tempoRestante } from '../format.js'
import Icone from './Icone.jsx'
import Marca from './Marca.jsx'

// Seções do portal (barra superior). "permissao" esconde a seção de quem não pode vê-la
// (o backend também barra com 403 — esconder o menu é só conforto).
const SECOES = [
  { para: '/solicitacoes', icone: 'lista', label: 'Solicitações' },
  { para: '/qa', icone: 'escudo', label: 'QA Center', permissao: 'qa.view' },
]

// Tempo de sessão: em minutos na maior parte do tempo; nos 2 minutos finais vira mm:ss em destaque.
// O relógio fica só aqui, então só este texto redesenha a cada segundo.
function SessaoRestante({ expiraEm }) {
  const agora = useAgora(1000)
  const segundos = (expiraEm - agora) / 1000
  const acabando = segundos <= 120
  return (
    <span className={`sessao${acabando ? ' sessao-acabando' : ''}`}>
      {acabando
        ? <>Sessão expira em <span className="tabular">{tempoRestante(new Date(expiraEm).toISOString(), agora)}</span></>
        : <>Sessão expira em {Math.ceil(segundos / 60)} min</>}
    </span>
  )
}

export default function Layout() {
  const { sessao, sair, pode } = useAuth()
  const navegar = useNavigate()
  const inicial = sessao.email.charAt(0).toUpperCase()

  return (
    <div className="app">
      <header className="barra-topo">
        <div className="barra-topo-conteudo">
          <NavLink to="/solicitacoes" className="marca-link" aria-label="Carimbô — ir para Solicitações">
            <Marca tamanho={42} />
          </NavLink>

          <nav className="secoes" aria-label="Seções do portal">
            {SECOES.filter((s) => !s.permissao || pode(s.permissao)).map((s) => (
              <NavLink key={s.para} to={s.para} className="secao">
                <Icone nome={s.icone} tamanho={16} />{s.label}
              </NavLink>
            ))}
          </nav>

          <div className="conta">
            <span className="conta-avatar" aria-hidden="true">{inicial}</span>
            <div className="conta-dados">
              <strong title={sessao.email}>{sessao.email}</strong>
              <SessaoRestante expiraEm={sessao.expiraEm} />
            </div>
            <button className="botao botao-fantasma botao-sair" onClick={() => { sair(); navegar('/login') }} aria-label="Sair">
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
