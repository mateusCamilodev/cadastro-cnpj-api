import { Link } from 'react-router-dom'

// Cabeçalho padrão das páginas: trilha (breadcrumb), título, descrição e ações à direita
export default function Cabecalho({ trilha = [], titulo, descricao, children }) {
  return (
    <header className="cabecalho">
      {trilha.length > 0 && (
        <nav className="trilha" aria-label="Você está em">
          {trilha.map((t, i) => (
            <span key={i}>
              {t.para ? <Link to={t.para}>{t.label}</Link> : t.label}
              {i < trilha.length - 1 && <span className="trilha-sep">/</span>}
            </span>
          ))}
        </nav>
      )}
      <div className="cabecalho-linha">
        <div>
          <h1>{titulo}</h1>
          {descricao && <p className="cabecalho-descricao">{descricao}</p>}
        </div>
        {children && <div className="cabecalho-acoes">{children}</div>}
      </div>
    </header>
  )
}
