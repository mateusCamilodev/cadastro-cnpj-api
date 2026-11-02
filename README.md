# cadastro-cnpj-api

API REST de **solicitação de cadastro de CNPJ**: a pessoa cria uma solicitação (IPR), declara os documentos PDF que vai enviar, envia os PDFs e a API valida campo a campo se o conteúdo dos documentos bate com os dados informados. No fim, a solicitação é **APROVADA** ou **REPROVADA**.

> Este README é a **especificação** da API: descreve o comportamento esperado.

## Como rodar

Requisitos: **JDK 21+**.

```bash
# Windows
mvnw.cmd spring-boot:run
# Linux/Mac
./mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Banco H2 **em memória** (os dados somem ao reiniciar): `http://localhost:8080/h2-console`
  - JDBC URL `jdbc:h2:mem:cadastrodb`
- PDFs de exemplo em [`docs/`](docs/).

## Fluxo de status

```
RECEBIDO ─(10s)─► AGUARDANDO_DOCUMENTO ─(todos os PDFs recebidos)─► EM_PROCESSAMENTO ─(30s, análise)─► APROVADO
                         │                                                                          └► REPROVADO
                         └─(5 min sem todos os PDFs)─► EXPIRADO

CANCELADO: via PATCH, permitido em RECEBIDO, AGUARDANDO_DOCUMENTO ou EM_PROCESSAMENTO
```

- As transições automáticas são feitas por um agendador que roda a cada **5s** (pode haver até 5s de atraso).
- Tempos configuráveis em `application.properties`: `solicitacao.tempo-recebido=10s`, `solicitacao.prazo-documento=5m`, `solicitacao.tempo-processamento=30s`.
- **Solicitação ativa** = qualquer status exceto `CANCELADO`, `EXPIRADO` e `REPROVADO`.

## Autenticação

Todos os endpoints de `/solicitacoes` exigem um **token JWT** no header `Authorization: Bearer <token>`. O token é gerado em `POST /auth/token` e **expira em 15 minutos**.

```json
POST /auth/token
{ "email": "<seu-email>", "senha": "<sua-senha>" }
```

Resposta **200**:

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 900 }
```

| Situação | Resultado |
|---|---|
| E-mail e senha corretos | **200** com o token |
| E-mail ou senha errados | **401** `{ "mensagem": "E-mail ou senha inválidos" }` (não diz qual dos dois) |
| `email` ausente/inválido ou `senha` ausente | **400** (formato de erro padrão) |
| Rota protegida sem o header `Authorization` | **401** `{ "mensagem": "Token ausente" }` + header `WWW-Authenticate: Bearer` |
| Token adulterado, assinado com outro segredo ou expirado | **401** `{ "mensagem": "Token inválido ou expirado" }` + header `WWW-Authenticate: Bearer` |

Os **401** devolvem só `{ "mensagem": "..." }`, sem o formato de erro completo.

- Usuários definidos em `application-local.properties` (fora do Git; modelo em `application-local.example.properties`).
- O e-mail não diferencia maiúsculas/minúsculas; a senha diferencia.
- Abertos sem token: `POST /auth/token` e o H2 Console.

## Endpoints

| Ação | Método | URL |
|---|---|---|
| Gerar token | POST | `/auth/token` (sem token) |
| Criar solicitação | POST | `/solicitacoes` |
| Consultar | GET | `/solicitacoes/{id}` |
| Cancelar | PATCH | `/solicitacoes/{id}/cancelamento` (sem body) |
| Enviar documento | POST | `/solicitacoes/{id}/documento` (form-data, campo `arquivo` = PDF) |

### POST `/solicitacoes`

```json
{
  "ipr": "IPR-0001",
  "arquivos": ["documento-valido.pdf", "documento-valido-2.pdf"],
  "pessoa": {
    "nome": "Mateus Rodrigues",
    "cpf": "12345678909",
    "email": "mateus@email.com",
    "telefone": "19999998888",
    "dataNascimento": "2000-01-15"
  },
  "empresa": {
    "cnpj": "11222333000181",
    "razaoSocial": "Empresa Teste LTDA",
    "nomeFantasia": "Empresa Teste",
    "dataAbertura": "2020-03-10"
  }
}
```

**Validações (400 se violadas):**

| Campo | Regra |
|---|---|
| `ipr` | obrigatório, máx. 50 caracteres |
| `arquivos` | obrigatório, 1 a 10 itens, sem nomes repetidos; cada item terminando em `.pdf`, máx. 255, sem caminho (sem `\ / : * ? " < > \|`) |
| `pessoa`, `empresa` | obrigatórios |
| `nome` | obrigatório, máx. 150 |
| `cpf` | obrigatório, exatamente 11 dígitos numéricos (não confere dígito verificador) |
| `email` | obrigatório, formato de e-mail válido |
| `telefone` | obrigatório, 10 ou 11 dígitos numéricos |
| `dataNascimento` | obrigatória, no passado |
| `cnpj` | obrigatório, exatamente 14 dígitos numéricos (não confere dígito verificador) |
| `razaoSocial` | obrigatória, máx. 200 |
| `nomeFantasia` | **opcional**, máx. 200 |
| `dataAbertura` | **opcional**, sem validação de data |

Datas no formato `yyyy-MM-dd`.

**Regras:**

| Situação | Resultado |
|---|---|
| `ipr` e `cnpj` sem solicitação ativa | **201**, header `Location: /solicitacoes/{id}` |
| `ipr` já usado numa solicitação ativa | **409** `Já existe uma solicitação ativa com este IPR` |
| `cnpj` já usado numa solicitação ativa (com outro `ipr`) | **409** `Já existe uma solicitação ativa para este CNPJ` |
| `ipr`/`cnpj` de uma solicitação inativa (cancelada/expirada/reprovada) | **201**, cria **nova** solicitação com **novo id** |

- `ipr` e `cnpj` são conferidos **cada um por si**. O **CPF pode repetir** à vontade.
- Se os dois estiverem em uso, a mensagem é a do IPR (conferido primeiro).
- Resposta (201): **somente** o id → `{ "id": "uuid" }`.

### GET `/solicitacoes/{id}`

```json
{
  "id": "uuid",
  "status": "AGUARDANDO_DOCUMENTO",
  "historico": [
    { "dataHora": "...", "status": "RECEBIDO", "descricao": "Solicitação criada" },
    { "dataHora": "...", "status": "AGUARDANDO_DOCUMENTO", "descricao": "Aguardando documentos até dd/MM/yyyy HH:mm:ss. Enviados até o momento: 0 de 2" },
    { "dataHora": "...", "status": "AGUARDANDO_DOCUMENTO", "descricao": "Documento recebido. Enviados até o momento: 1 de 2" }
  ]
}
```

- `historico` em **ordem cronológica** (mais antigo primeiro).
- "Enviados até o momento: X de N" mostra quantos documentos já foram recebidos **incluindo** o do evento.
- Descrições não mostram nomes de arquivo nem dados pessoais.
- id inexistente → **404** · id que não é UUID → **400**.

### PATCH `/solicitacoes/{id}/cancelamento`

- Permitido em `RECEBIDO`, `AGUARDANDO_DOCUMENTO` ou `EM_PROCESSAMENTO` → **200**, status `CANCELADO`.
- Em `APROVADO`, `REPROVADO`, `CANCELADO` ou `EXPIRADO` → **409**.

### POST `/solicitacoes/{id}/documento`

Um arquivo por requisição: **Body → form-data**, chave `arquivo` do tipo **File**.

| Situação | Resultado |
|---|---|
| status diferente de `AGUARDANDO_DOCUMENTO` (ou prazo vencido) | **409** |
| nome do arquivo não está na lista `arquivos` do POST (comparação **exata**, diferencia maiúsculas/minúsculas) | **422**, detalhe `nomeArquivo` |
| arquivo declarado que **já foi recebido** | **409** |
| sem o campo `arquivo`, arquivo vazio, não é PDF, PDF corrompido ou sem texto (escaneado) | **400** |
| arquivo maior que 5MB | **413** |
| PDF válido | **200** `{ "mensagem": "Arquivo <nome declarado> enviado" }`; conta como recebido (mesmo que o conteúdo divirja, a divergência é tratada na análise) |
| último PDF declarado recebido | **200** com a mesma mensagem; status → `EM_PROCESSAMENTO` (veja no GET) |

### Layout do PDF

Uma linha por campo, `Rótulo: valor`. Linhas sem `:` são ignoradas (títulos).

```
IPR: IPR-0001
Nome: Mateus Rodrigues
CPF: 123.456.789-09
E-mail: mateus@email.com
Telefone: (19) 99999-8888
Data de Nascimento: 15/01/2000
CNPJ: 11.222.333/0001-81
Razão Social: Empresa Teste LTDA
Nome Fantasia: Empresa Teste
```

`Data de Abertura` pode estar no PDF, mas não é comparada.

Para criar um PDF: escreva no Word e use **Salvar como → PDF** (PDF de imagem/escaneado não funciona).

### Análise (EM_PROCESSAMENTO → APROVADO / REPROVADO)

Cada campo de **cada** documento é comparado com o POST:

- Texto (ipr, nome, e-mail, razão social, nome fantasia): ignora maiúsculas/minúsculas e espaços extras.
- CPF, CNPJ, telefone: compara só os dígitos (aceita com ou sem máscara).
- Data de nascimento: aceita `dd/MM/yyyy` ou `yyyy-MM-dd`.
- Data de abertura: **não é comparada**.
- `Nome Fantasia`: se não foi enviado no POST, o PDF também não pode ter.
- Campo ausente no PDF = reprovado.

Todos os campos de todos os documentos conferem → **APROVADO**. Qualquer campo divergente ou ausente → **REPROVADO**, com o histórico listando os campos (sem valores).

## Formato de erro

```json
{ "timestamp": "...", "status": 409, "mensagem": "Já existe uma solicitação ativa com este IPR" }
```

Em erro de validação (400), vem também `detalhes` com cada campo inválido:

```json
{
  "timestamp": "...",
  "status": 400,
  "mensagem": "Um ou mais campos estão inválidos",
  "detalhes": [ { "campo": "cpf", "mensagem": "cpf deve conter 11 dígitos numéricos" } ]
}
```

- `detalhes` só aparece quando tem itens (400 de validação e 422).
- `detalhes[].campo` mostra só o nome final do campo (`pessoa.cpf` → `cpf`).
- Os **401** devolvem só `{ "mensagem": "..." }`.
- Mensagens de erro **nunca** devolvem CPF ou CNPJ (LGPD).

| Código | Quando |
|---|---|
| 400 | validação, JSON mal formatado, id não-UUID, problemas no arquivo enviado |
| 401 | credenciais inválidas no `/auth/token`; token ausente, inválido ou expirado |
| 404 | solicitação não encontrada, URL inexistente |
| 405 | método HTTP não suportado na URL |
| 409 | conflito de regra de negócio (duplicidade, cancelamento/envio não permitido) |
| 413 | arquivo maior que 5MB |
| 415 | Content-Type não suportado |
| 422 | arquivo não declarado na criação |
| 500 | erro inesperado |

## Dados de teste

- CPF e CNPJ: qualquer número com 11 e 14 dígitos (ex.: CPF `11111111111`, CNPJ `12345678901234`). Os PDFs de exemplo usam CPF `12345678909` e CNPJ `11222333000181`.
- PDFs: `docs/documento-valido.pdf` e `docs/documento-valido-2.pdf` (batem com o body acima), `docs/documento-divergente.pdf` (CPF e razão social diferentes, sem data de abertura).
