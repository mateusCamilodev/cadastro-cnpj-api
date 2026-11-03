// Cliente da API. Todas as chamadas passam por aqui:
// coloca o token no header, transforma a resposta em JSON e lança ApiError quando der erro.

const BASE = '/api'
const CHAVE_AUTH = 'cadastro-cnpj-auth'

let aoPerderSessao = () => {}
export function definirAoPerderSessao(fn) {
  aoPerderSessao = fn
}

export function lerSessao() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE_AUTH))
  } catch {
    return null
  }
}

export function salvarSessao(sessao) {
  try {
    if (sessao) localStorage.setItem(CHAVE_AUTH, JSON.stringify(sessao))
    else localStorage.removeItem(CHAVE_AUTH)
  } catch {
    // navegador sem localStorage: a sessão vale só enquanto a página estiver aberta
  }
}

export class ApiError extends Error {
  constructor(status, corpo) {
    super(corpo?.mensagem || `Erro ${status}`)
    this.status = status
    this.corpo = corpo
    this.detalhes = corpo?.detalhes || []
  }
}

async function requisitar(caminho, { metodo = 'GET', json, form, semToken = false } = {}) {
  const headers = {}
  // O login NUNCA leva token: se um token velho/vencido for junto, o Spring recusa com 401
  // ("Token inválido ou expirado") antes mesmo de olhar e-mail e senha.
  const token = semToken ? null : lerSessao()?.token
  if (token) headers.Authorization = `Bearer ${token}`

  let corpo
  if (form) {
    corpo = form // o navegador monta o Content-Type multipart sozinho
  } else if (json !== undefined) {
    headers['Content-Type'] = 'application/json'
    corpo = JSON.stringify(json)
  }

  let resposta
  try {
    resposta = await fetch(BASE + caminho, { method: metodo, headers, body: corpo })
  } catch {
    throw new ApiError(0, { mensagem: 'Não foi possível falar com a API. Ela está rodando em localhost:8080?' })
  }

  const texto = await resposta.text()
  let dados = null
  try {
    dados = texto ? JSON.parse(texto) : null
  } catch {
    dados = { mensagem: texto }
  }

  if (resposta.status === 401 && caminho !== '/auth/token') aoPerderSessao()
  if (!resposta.ok) throw new ApiError(resposta.status, dados)
  return dados
}

export const api = {
  login: (email, senha) => requisitar('/auth/token', { metodo: 'POST', json: { email, senha }, semToken: true }),
  listar: () => requisitar('/solicitacoes'),
  buscar: (id) => requisitar(`/solicitacoes/${id}`),
}
