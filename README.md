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

## Endpoints

| Ação | Método | URL |
|---|---|---|
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
| `cpf` | obrigatório, 11 dígitos numéricos, CPF válido |
| `email` | obrigatório, formato de e-mail válido |
| `telefone` | obrigatório, 10 ou 11 dígitos numéricos |
| `dataNascimento` | obrigatória, no passado |
| `cnpj` | obrigatório, 14 dígitos numéricos, CNPJ válido |
| `razaoSocial` | obrigatória, máx. 200 |
| `nomeFantasia` | **opcional**, máx. 200 |
| `dataAbertura` | obrigatória, hoje ou no passado |

Datas no formato `yyyy-MM-dd`.

**Regras:**

| Situação | Resultado |
|---|---|
| `ipr` novo e CPF+CNPJ sem solicitação ativa | **201**, header `Location: /solicitacoes/{id}` |
| mesmo `ipr` de uma solicitação ativa | **200**, devolve a existente (idempotência) |
| `ipr` novo, mas a **combinação** CPF+CNPJ já tem solicitação ativa | **409** |
| mesmo `ipr` de uma solicitação inativa (cancelada/expirada/reprovada) | cria **nova** solicitação com **novo id** (**201**), se CPF+CNPJ não tiverem outra ativa |

- O conflito é pela **combinação** CPF + CNPJ: mesmo CPF com outro CNPJ é permitido (e vice-versa).
- Resposta (201 ou 200): **somente** o id → `{ "id": "uuid" }`.

### GET `/solicitacoes/{id}`

```json
{
  "id": "uuid",
  "status": "AGUARDANDO_DOCUMENTO",
  "historico": [
    { "dataHora": "...", "status": "RECEBIDO", "descricao": "Solicitação criada" },
    { "dataHora": "...", "status": "AGUARDANDO_DOCUMENTO", "descricao": "Aguardando documentos até dd/MM/yyyy HH:mm:ss. Documentos declarados: 2. Enviados até o momento: 0 de 2" },
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
| PDF válido | **200**, conta como recebido (mesmo que o conteúdo divirja, a divergência é tratada na análise) |
| último PDF declarado recebido | **200**, status → `EM_PROCESSAMENTO` |

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
Data de Abertura: 10/03/2020
```

Para criar um PDF: escreva no Word e use **Salvar como → PDF** (PDF de imagem/escaneado não funciona).

### Análise (EM_PROCESSAMENTO → APROVADO / REPROVADO)

Cada campo de **cada** documento é comparado com o POST:

- Texto (ipr, nome, e-mail, razão social, nome fantasia): ignora maiúsculas/minúsculas e espaços extras.
- CPF, CNPJ, telefone: compara só os dígitos (aceita com ou sem máscara).
- Datas: aceita `dd/MM/yyyy` ou `yyyy-MM-dd`.
- `Nome Fantasia`: se não foi enviado no POST, o PDF também não pode ter.
- Campo ausente no PDF = reprovado.

Todos os campos de todos os documentos conferem → **APROVADO**. Qualquer campo divergente ou ausente → **REPROVADO**, com o histórico listando os campos (sem valores).

## Formato de erro

```json
{
  "timestamp": "...",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Um ou mais campos estão inválidos",
  "path": "/solicitacoes",
  "detalhes": [ { "campo": "cpf", "mensagem": "cpf inválido" } ]
}
```

- `detalhes[].campo` mostra só o nome final do campo (`pessoa.cpf` → `cpf`).
- Mensagens de erro **nunca** devolvem CPF ou CNPJ (LGPD).

| Código | Quando |
|---|---|
| 400 | validação, JSON mal formatado, id não-UUID, problemas no arquivo enviado |
| 404 | solicitação não encontrada, URL inexistente |
| 405 | método HTTP não suportado na URL |
| 409 | conflito de regra de negócio (duplicidade, cancelamento/envio não permitido) |
| 413 | arquivo maior que 5MB |
| 415 | Content-Type não suportado |
| 422 | arquivo não declarado na criação |
| 500 | erro inesperado |

## Dados de teste

- CPFs válidos: `12345678909`, `52998224725`
- CNPJs válidos: `11222333000181`, `11444777000161`
- PDFs: `docs/documento-valido.pdf` e `docs/documento-valido-2.pdf` (batem com o body acima), `docs/documento-divergente.pdf` (CPF e razão social diferentes, sem data de abertura).
