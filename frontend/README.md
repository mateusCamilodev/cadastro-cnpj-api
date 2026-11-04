# Front-end — Carimbô

Portal web em React + Vite para a API `cadastro-cnpj-api`.

Mostra a lista de solicitações, o andamento e o resultado de cada uma, e permite **enviar os PDFs pendentes** (botão ou arrastar e soltar), usando o endpoint `POST /solicitacoes/{id}/documento`. Criar e cancelar solicitações continuam sendo feitos pela API.

## Como rodar

1. Suba a **API** (porta 8080) na raiz do projeto.
2. Nesta pasta (`frontend/`):

```bash
npm install     # só na primeira vez
npm run dev     # no PowerShell, se houver erro de "execução de scripts": npm.cmd run dev
```

3. Abra **http://localhost:5173** e entre com um usuário configurado na API.

O Vite repassa tudo que começa com `/api` para `http://localhost:8080` (veja `vite.config.js`), então não é preciso configurar CORS. Para apontar para outra API: `API_URL=http://localhost:8099 npm run dev`.

## Telas

| Tela | Rota | O que mostra |
|---|---|---|
| Login | `/login` | Gera o token; a sessão encerra sozinha quando o token vence. |
| Solicitações | `/solicitacoes` | Lista com abas de status, busca por número de solicitação e atualização automática. |
| Detalhe | `/solicitacoes/{id}` | Número e status, prazo, etapas, envio dos documentos pendentes, linha do tempo e o resultado da análise. |

## Estrutura

```
src/
  api.js          chamadas à API (token no header, erros em ApiError) — login, listar, buscar, enviarDocumento
  auth.jsx        login/logout e proteção das rotas
  format.js       textos de status e formatação de datas
  main.jsx        rotas
  styles.css      visual
  hooks.js        useAgora (relógio compartilhado)
  pixel.js        paleta única da pixel art (logo, mascotes, fundo)
  components/     Layout, Marca, FundoAnimado, Mascote, MascoteDuvida, Carimbo, StatusBadge, Icone,
                  Etapas, Documentos, Historico, Prazo, ProgressoDocumentos, Carregando
  pages/          Login, Lista, Detalhe
```
