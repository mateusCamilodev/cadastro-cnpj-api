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

## Estrutura

```
src/main/java/.../
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
