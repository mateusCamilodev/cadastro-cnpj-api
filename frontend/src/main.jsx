import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, SoLogado } from './auth.jsx'
import Layout from './components/Layout.jsx'
import FundoAnimado from './components/FundoAnimado.jsx'
import Login from './pages/Login.jsx'
import Lista from './pages/Lista.jsx'
import Detalhe from './pages/Detalhe.jsx'
import './styles.css'

// Ao abrir o front ele sempre começa na lista (mesmo que o navegador tenha guardado uma solicitação aberta)
if (location.pathname !== '/solicitacoes' && location.pathname !== '/login') {
  history.replaceState(null, '', '/solicitacoes')
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <FundoAnimado />
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<SoLogado><Layout /></SoLogado>}>
            <Route path="/" element={<Navigate to="/solicitacoes" replace />} />
            <Route path="/solicitacoes" element={<Lista />} />
            <Route path="/solicitacoes/:id" element={<Detalhe />} />
          </Route>
          <Route path="*" element={<Navigate to="/solicitacoes" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
