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
  // POST /solicitacoes/{id}/documento (form-data, campo "arquivo") — o endpoint que a API já tem
  enviarDocumento: (id, arquivo) => {
    const form = new FormData()
    form.append('arquivo', arquivo)
    return requisitar(`/solicitacoes/${id}/documento`, { metodo: 'POST', form })
  },
}

// ===== QA Center =====
function consulta(params) {
  const q = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.set(k, v) })
  const s = q.toString()
  return s ? `?${s}` : ''
}

export const qa = {
  endpoints: () => requisitar('/qa/endpoints'),
  cenarios: () => requisitar('/qa/cenarios'),
  categorias: () => requisitar('/qa/categorias'),
  ambientes: () => requisitar('/qa/ambientes'),
  suites: () => requisitar('/qa/suites'),
  criarSuite: (suite) => requisitar('/qa/suites', { metodo: 'POST', json: suite }),
  alterarSuite: (id, suite) => requisitar(`/qa/suites/${id}`, { metodo: 'PUT', json: suite }),
  excluirSuite: (id) => requisitar(`/qa/suites/${id}`, { metodo: 'DELETE' }),
  configuracoes: () => requisitar('/qa/configuracoes'),
  alterarConfiguracoes: (valores) => requisitar('/qa/configuracoes', { metodo: 'PUT', json: valores }),
  auditoria: (quantidade = 100) => requisitar(`/qa/auditoria?quantidade=${quantidade}`),
  criarExecucao: (pedido) => requisitar('/qa/execucoes', { metodo: 'POST', json: pedido }),
  execucoes: (filtros) => requisitar('/qa/execucoes' + consulta(filtros)),
  execucao: (id) => requisitar(`/qa/execucoes/${id}`),
  resultados: (id) => requisitar(`/qa/execucoes/${id}/resultados`),
  cancelar: (id) => requisitar(`/qa/execucoes/${id}/cancelar`, { metodo: 'POST' }),
  reexecutar: (id) => requisitar(`/qa/execucoes/${id}/reexecutar`, { metodo: 'POST' }),
  resumo: (filtros) => requisitar('/qa/relatorios/resumo' + consulta(filtros)),
  tendencias: (filtros) => requisitar('/qa/relatorios/tendencias' + consulta(filtros)),
  falhas: (filtros) => requisitar('/qa/relatorios/falhas' + consulta(filtros)),
  comparacao: (a, b) => requisitar('/qa/relatorios/comparacao' + consulta({ a, b })),

  // Baixa o CSV com o token no header e entrega ao navegador como arquivo
  async baixarCsv(filtros) {
    const token = lerSessao()?.token
    const resposta = await fetch(BASE + '/qa/relatorios/exportar.csv' + consulta(filtros), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (resposta.status === 401) aoPerderSessao()
    if (!resposta.ok) throw new ApiError(resposta.status, { mensagem: `Não foi possível exportar (HTTP ${resposta.status})` })
    const nome = /filename="([^"]+)"/.exec(resposta.headers.get('Content-Disposition') || '')?.[1] || 'qa-relatorio.csv'
    const url = URL.createObjectURL(await resposta.blob())
    const a = document.createElement('a')
    a.href = url
    a.download = nome
    a.click()
    URL.revokeObjectURL(url)
  },

  // Tempo real (SSE) via fetch: mantém o header Authorization (o EventSource do navegador não envia headers,
  // e token na URL ficaria em logs). Reconecta sozinho se a conexão cair antes do fim.
  // Devolve uma função para parar de ouvir.
  ouvirEventos(id, aoReceber, aoFalhar) {
    let ativo = true
    let controle = null
    async function conectar(tentativa = 0) {
      controle = new AbortController()
      const token = lerSessao()?.token
      try {
        const resposta = await fetch(`${BASE}/qa/execucoes/${id}/eventos`, {
          headers: { Accept: 'text/event-stream', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
          signal: controle.signal,
        })
        if (resposta.status === 401) { aoPerderSessao(); return }
        if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`)
        const leitor = resposta.body.getReader()
        const decodificador = new TextDecoder()
        let buffer = ''
        let terminou = false
        for (;;) {
          const { value, done } = await leitor.read()
          if (done) break
          buffer += decodificador.decode(value, { stream: true })
          let fim
          while ((fim = buffer.indexOf('\n\n')) >= 0) {
            const bloco = buffer.slice(0, fim)
            buffer = buffer.slice(fim + 2)
            let tipo = 'message'
            const dados = []
            for (const linha of bloco.split('\n')) {
              if (linha.startsWith('event:')) tipo = linha.slice(6).trim()
              else if (linha.startsWith('data:')) dados.push(linha.slice(5).replace(/^ /, ''))
            }
            if (!dados.length) continue
            let conteudo = null
            try { conteudo = JSON.parse(dados.join('\n')) } catch { conteudo = dados.join('\n') }
            if (tipo === 'fim') terminou = true
            if (ativo) aoReceber(tipo, conteudo)
          }
        }
        if (ativo && !terminou) setTimeout(() => ativo && conectar(0), 1000)
      } catch (e) {
        if (!ativo || e.name === 'AbortError') return
        if (aoFalhar) aoFalhar(e)
        setTimeout(() => ativo && conectar(tentativa + 1), Math.min(10000, 1000 * 2 ** tentativa))
      }
    }
    conectar()
    return () => { ativo = false; controle?.abort() }
  },
}
