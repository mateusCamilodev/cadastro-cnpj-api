import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import Marca from '../components/Marca.jsx'
import Icone from '../components/Icone.jsx'

export default function Login() {
  const { sessao, entrar, expirou } = useAuth()
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [verSenha, setVerSenha] = useState(false)

  if (sessao) return <Navigate to="/solicitacoes" replace />

  async function enviar(e) {
    e.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await entrar(email, senha)
      navegar('/solicitacoes', { replace: true })
    } catch (err) {
      setErro(err.detalhes.length ? err.detalhes.map((d) => d.mensagem).join('. ') : err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="login">
      <div className="login-cartao">
        {/* Lado esquerdo: apresentação do portal */}
        <aside className="login-vitrine">
          <Marca tamanho={52} />
          <div className="login-vitrine-texto">
            <h2>Do envio ao carimbo, cada solicitação em um só lugar.</h2>
            <ul className="login-recursos">
              <li><Icone nome="relogio" tamanho={18} />Status atualizado em tempo real</li>
              <li><Icone nome="arquivo" tamanho={18} />Histórico completo de cada etapa</li>
              <li><Icone nome="check" tamanho={18} />Resultado com os campos que divergiram</li>
            </ul>
          </div>
          <p className="login-vitrine-rodape"><Icone nome="escudo" tamanho={16} />Acesso protegido por token JWT</p>
        </aside>

        {/* Lado direito: formulário */}
        <form className="login-form" onSubmit={enviar} noValidate>
          <h1>Entrar</h1>
          <p className="texto-suave login-sub">Use seu e-mail e senha para acessar o portal.</p>

          {expirou && <div className="mensagem mensagem-alerta"><Icone nome="alerta" />Sua sessão expirou. Entre novamente.</div>}
          {erro && <div className="mensagem mensagem-erro"><Icone nome="alerta" />{erro}</div>}

          <label className="campo">
            <span className="campo-rotulo">E-mail</span>
            <span className="campo-com-icone">
              <Icone nome="email" tamanho={18} />
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                     placeholder="voce@empresa.com" autoComplete="username" autoFocus />
            </span>
          </label>
          <label className="campo">
            <span className="campo-rotulo">Senha</span>
            <span className="campo-com-icone">
              <Icone nome="cadeado" tamanho={18} />
              <input type={verSenha ? 'text' : 'password'} value={senha} onChange={(e) => setSenha(e.target.value)}
                     placeholder="Sua senha" autoComplete="current-password" />
              <button type="button" className="campo-acao" onClick={() => setVerSenha((v) => !v)}
                      aria-label={verSenha ? 'Esconder senha' : 'Mostrar senha'} title={verSenha ? 'Esconder senha' : 'Mostrar senha'}>
                <Icone nome={verSenha ? 'olho-fechado' : 'olho'} tamanho={18} />
              </button>
            </span>
          </label>

          <button className="botao botao-primario botao-largo login-entrar" disabled={enviando}>
            {enviando ? 'Entrando…' : <>Entrar<Icone nome="seta" tamanho={16} /></>}
          </button>
        </form>
      </div>
    </div>
  )
}
