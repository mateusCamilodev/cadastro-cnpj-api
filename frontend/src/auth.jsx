import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { api, definirAoPerderSessao, lerSessao, salvarSessao } from './api.js'

// Guarda quem está logado (token, e-mail e quando o token expira) para o app inteiro
const AuthContext = createContext(null)

// Sessão salva ainda dentro do prazo; se estiver vencida, já apaga do navegador
function sessaoValida() {
  const s = lerSessao()
  if (s && s.expiraEm > Date.now()) return s
  if (s) salvarSessao(null)
  return null
}

export function AuthProvider({ children }) {
  const [sessao, setSessao] = useState(sessaoValida)
  const [expirou, setExpirou] = useState(false)

  const sair = useCallback((porExpiracao = false) => {
    salvarSessao(null)
    setSessao(null)
    setExpirou(porExpiracao)
  }, [])

  const entrar = useCallback(async (email, senha) => {
    const r = await api.login(email, senha)
    const nova = { token: r.accessToken, email: email.trim().toLowerCase(), expiraEm: Date.now() + r.expiresIn * 1000 }
    salvarSessao(nova)
    setSessao(nova)
    setExpirou(false)
  }, [])

  // A API respondeu 401 em alguma tela → token inválido/expirado
  useEffect(() => definirAoPerderSessao(() => sair(true)), [sair])

  // Desloga sozinho quando o token vence
  useEffect(() => {
    if (!sessao) return
    const t = setTimeout(() => sair(true), sessao.expiraEm - Date.now())
    return () => clearTimeout(t)
  }, [sessao, sair])

  return (
    <AuthContext.Provider value={{ sessao, entrar, sair, expirou }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}

// Envolve as páginas que exigem login
export function SoLogado({ children }) {
  const { sessao } = useAuth()
  if (!sessao) return <Navigate to="/login" replace />
  return children
}
