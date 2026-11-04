# cadastro-cnpj-api

API REST para **solicitações de cadastro de CNPJ**, com um portal web (React) para acompanhar cada solicitação.

A solicitação é criada pela API, os documentos PDF são enviados e a API confere o conteúdo deles com os dados informados, aprovando ou reprovando a solicitação.

## Tecnologias

- **Backend:** Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, H2 (em memória), Apache PDFBox
- **Front-end:** React, Vite, React Router

## Como rodar

### 1. Configuração local

Usuários, senhas e o segredo do JWT **não ficam no repositório**. Crie o arquivo de configuração local a partir do modelo:

```bash
cp src/main/resources/application-local.example.properties src/main/resources/application-local.properties
```

e preencha os valores. O arquivo `application-local.properties` é ignorado pelo Git.

### 2. API (porta 8080)

Requisito: **JDK 21+**.

```bash
# Windows
mvnw.cmd spring-boot:run
# Linux/Mac
./mvnw spring-boot:run
```

O banco H2 é **em memória**: os dados somem ao reiniciar a API.

### 3. Front-end (porta 5173)

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. Detalhes em [`frontend/README.md`](frontend/README.md).

## Endpoints

Todos os endpoints de `/solicitacoes` exigem o header `Authorization: Bearer <token>`.

| Ação | Método | URL |
|---|---|---|
| Gerar token | POST | `/auth/token` |
| Criar solicitação | POST | `/solicitacoes` |
| Listar | GET | `/solicitacoes` (filtro opcional `?status=`) |
| Consultar | GET | `/solicitacoes/{id}` |
| Cancelar | PATCH | `/solicitacoes/{id}/cancelamento` |
| Enviar documento | POST | `/solicitacoes/{id}/documento` (form-data, campo `arquivo` = PDF) |

## Formato de erro

```json
{
  "timestamp": "2026-01-01T10:00:00",
  "status": 400,
  "mensagem": "Um ou mais campos estão inválidos",
  "detalhes": [{ "campo": "solicitacao", "mensagem": "solicitação é obrigatória" }]
}
```

## QA Center

Módulo para configurar, executar, acompanhar ao vivo e analisar testes automatizados da própria API (menu **QA Center** no front).
Os testes chamam a API **de verdade** em segundo plano; nenhum resultado é simulado.

**Permissões** (claim `scope` do JWT, definidas por usuário em `auth.usuarios[N].permissoes` no arquivo local). Sem elas, o backend responde 403:

| Permissão | Libera |
|---|---|
| `qa.view` | catálogo, execuções e detalhes (`GET /qa/**`) |
| `qa.execute` | criar e reexecutar execuções |
| `qa.cancel` | cancelar execução |
| `qa.reports.view` | relatórios, comparação e CSV |

O executor usa uma conta de teste própria (`qa.conta-teste.*` no arquivo local) e só chama os endpoints do catálogo, na URL base do
ambiente escolhido. Nenhuma URL vem do navegador. Ambientes: **Desenvolvimento** (a própria API) e **Homologação** (`qa.ambientes.homologacao.url`,
aparece como indisponível se estiver desligada). Não existe opção de produção. As solicitações criadas pelos testes começam com `QA-`.

| Ação | Método | URL |
|---|---|---|
| Catálogo | GET | `/qa/endpoints`, `/qa/cenarios`, `/qa/categorias`, `/qa/ambientes` |
| Nova execução (202) | POST | `/qa/execucoes` `{ "ambiente", "endpointIds", "categorias" }` ou `{ "ambiente", "cenarioIds" }` |
| Histórico | GET | `/qa/execucoes?status=&ambiente=&usuario=&de=&ate=&busca=&pagina=&tamanho=` |
| Detalhe / resultados (com evidências mascaradas) | GET | `/qa/execucoes/{id}`, `/qa/execucoes/{id}/resultados` |
| Tempo real (SSE) | GET | `/qa/execucoes/{id}/eventos` |
| Cancelar / reexecutar | POST | `/qa/execucoes/{id}/cancelar`, `/qa/execucoes/{id}/reexecutar` |
| Relatórios | GET | `/qa/relatorios/resumo`, `tendencias`, `falhas`, `comparacao?a=&b=`, `exportar.csv` |

Limites: 1 execução ativa por usuário e 2 no total (429), a mesma seleção em andamento é recusada (409) e cada requisição e cada
execução têm tempo máximo (`qa.limites.*`). Tokens, senhas, CPF, CNPJ, e-mails e telefones são mascarados antes de serem gravados.
O histórico fica no H2 em memória.

Testes do módulo: `mvnw.cmd test` (sobem a aplicação numa porta aleatória e executam cenários reais contra ela).

## Estrutura

```
src/main/java/.../
  qa/           QA Center (catálogo, cenários, executor, tempo real, relatórios)
  config/       segurança e propriedades
  controller/   endpoints REST
  dto/          corpos de requisição e resposta
  exception/    erros e tratamento global
  model/        entidades JPA
  repository/   acesso ao banco
  service/      regras de negócio, leitura de PDF e análise
frontend/       portal web (React + Vite)
docs/           PDFs de exemplo
```
