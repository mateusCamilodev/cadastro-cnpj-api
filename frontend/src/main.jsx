import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { AuthProvider, ComPermissao, SoLogado } from './auth.jsx'
import Layout from './components/Layout.jsx'
import FundoAnimado from './components/FundoAnimado.jsx'
import Login from './pages/Login.jsx'
import Lista from './pages/Lista.jsx'
import Detalhe from './pages/Detalhe.jsx'
import QaPainel from './pages/qa/QaPainel.jsx'
import QaEndpoints from './pages/qa/QaEndpoints.jsx'
import QaCenarios from './pages/qa/QaCenarios.jsx'
import QaExecucoes from './pages/qa/QaExecucoes.jsx'
import QaExecucao from './pages/qa/QaExecucao.jsx'
import QaRelatorios from './pages/qa/QaRelatorios.jsx'
import QaSuites from './pages/qa/QaSuites.jsx'
import QaNovaExecucao from './pages/qa/QaNovaExecucao.jsx'
import QaAndamento from './pages/qa/QaAndamento.jsx'
import QaConfiguracoes from './pages/qa/QaConfiguracoes.jsx'
import QaLayout from './components/qa/QaLayout.jsx'
import './styles.css'
import './qa.css'

// Ao abrir o front ele começa na lista de solicitações (mesmo que o navegador tenha guardado uma solicitação
// aberta). As páginas do QA Center são preservadas: um F5 numa execução continua nela.
const caminho = location.pathname
if (caminho !== '/solicitacoes' && caminho !== '/login' && !caminho.startsWith('/qa')) {
  history.replaceState(null, '', '/solicitacoes')
}

// Fundo completo no login; discreto nas telas internas (não compete com o conteúdo)
function Fundo() {
  const { pathname } = useLocation()
  return <FundoAnimado variante={pathname === '/login' ? 'completa' : 'discreta'} />
}

const qaVer = (pagina) => <ComPermissao permissao="qa.view">{pagina}</ComPermissao>

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <Fundo />
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<SoLogado><Layout /></SoLogado>}>
            <Route path="/" element={<Navigate to="/solicitacoes" replace />} />
            <Route path="/solicitacoes" element={<Lista />} />
            <Route path="/solicitacoes/:id" element={<Detalhe />} />
            {/* QA Center: área técnica separada, com menu lateral próprio. Exige qa.view (e o backend confere tudo de novo). */}
            <Route path="/qa" element={qaVer(<QaLayout />)}>
              <Route index element={<QaPainel />} />
              <Route path="endpoints" element={<QaEndpoints />} />
              <Route path="cenarios" element={<QaCenarios />} />
              <Route path="suites" element={<QaSuites />} />
              <Route path="nova" element={<ComPermissao permissao="qa.execute"><QaNovaExecucao /></ComPermissao>} />
              <Route path="andamento" element={<QaAndamento />} />
              <Route path="execucoes" element={<QaExecucoes />} />
              <Route path="execucoes/:id" element={<QaExecucao />} />
              <Route path="relatorios" element={<ComPermissao permissao="qa.reports.view"><QaRelatorios /></ComPermissao>} />
              <Route path="configuracoes" element={<QaConfiguracoes />} />
              <Route path="*" element={<Navigate to="/qa" replace />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/solicitacoes" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
