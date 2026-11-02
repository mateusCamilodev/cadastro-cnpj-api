# Fluxo da API — cadastro-cnpj-api

Guia do fluxo completo e de cada endpoint. Descreve o **comportamento esperado**: qualquer resposta diferente do que está aqui é bug.

## Visão geral

A API recebe uma **solicitação de cadastro de CNPJ** (identificada por um **IPR**), espera a pessoa enviar os **PDFs** que ela declarou e, no fim, **aprova** ou **reprova** comparando campo a campo os PDFs com os dados enviados na criação.

```
 1. POST /auth/token                       → pega o token (vale 15 min)
 2. POST /solicitacoes                     → cria a solicitação, declara os PDFs   → 201 { id }
 3. (10s depois, automático)               → status AGUARDANDO_DOCUMENTO (prazo de 5 min)
 4. POST /solicitacoes/{id}/documento      → envia cada PDF declarado (um por vez)
 5. (último PDF recebido)                  → status EM_PROCESSAMENTO
 6. (30s depois, automático)               → análise campo a campo → APROVADO ou REPROVADO
    GET /solicitacoes/{id}                 → acompanha status e histórico a qualquer momento
    PATCH /solicitacoes/{id}/cancelamento  → cancela enquanto está em andamento
```

## Status

```
RECEBIDO ──(10s)──► AGUARDANDO_DOCUMENTO ──(todos os PDFs recebidos)──► EM_PROCESSAMENTO ──(30s)──► APROVADO
                            │                                                                  └──► REPROVADO
                            └──(5 min sem todos os PDFs)──► EXPIRADO

CANCELADO ◄── PATCH de cancelamento (só em RECEBIDO, AGUARDANDO_DOCUMENTO ou EM_PROCESSAMENTO)
```

| Status | Significado | Ativa? |
|---|---|---|
| `RECEBIDO` | Acabou de ser criada | Sim |
| `AGUARDANDO_DOCUMENTO` | Esperando os PDFs declarados (prazo de 5 min) | Sim |
| `EM_PROCESSAMENTO` | Todos os PDFs chegaram; análise em 30s | Sim |
| `APROVADO` | Todos os campos de todos os PDFs bateram | Sim |
| `REPROVADO` | Algum campo divergiu ou faltou | **Não** |
| `CANCELADO` | Cancelada pelo PATCH | **Não** |
| `EXPIRADO` | Os PDFs não chegaram em 5 min | **Não** |

- As mudanças automáticas são feitas por um agendador que roda a cada **5s** (pode haver até 5s de atraso).
- Tempos em `src/main/resources/application.properties` (dá para diminuir para testar): `solicitacao.tempo-recebido=10s`, `solicitacao.prazo-documento=5m`, `solicitacao.tempo-processamento=30s`.
- **Ativa** importa para as regras de duplicidade: uma solicitação inativa libera o IPR e o CNPJ dela.

---

## 1. `POST /auth/token` — gerar token

Único endpoint aberto. Todos os outros exigem o header:

```
Authorization: Bearer <accessToken>
```

**Body**

```json
{ "email": "<seu-email>", "senha": "<sua-senha>" }
```

Usuários válidos: os configurados em `application-local.properties`.

**Respostas**

| Situação | Status | Body |
|---|---|---|
| E-mail e senha corretos | **200** | `{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900 }` |
| E-mail ou senha errados | **401** | `{ "mensagem": "E-mail ou senha inválidos" }` (mesma mensagem nos dois casos) |
| `email` ausente/inválido ou `senha` ausente | **400** | formato de erro com `detalhes` |

- O token **expira em 15 minutos** (`expiresIn: 900` segundos).
- O e-mail não diferencia maiúsculas/minúsculas; a senha diferencia.

**Token nas outras rotas**

| Situação | Status | Body |
|---|---|---|
| Sem o header `Authorization` | **401** | `{ "mensagem": "Token ausente" }` |
| Token adulterado, inválido ou expirado | **401** | `{ "mensagem": "Token inválido ou expirado" }` |

---

## 2. `POST /solicitacoes` — criar solicitação

**Body**

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

`arquivos` = os nomes **exatos** dos PDFs que serão enviados depois.

**Validações dos campos (400 se violadas)**

| Campo | Obrigatório | Regra |
|---|---|---|
| `ipr` | Sim | Máx. 50 caracteres; não pode ser vazio nem só espaços |
| `arquivos` | Sim | 1 a 10 nomes, sem repetidos; cada um termina em `.pdf`, máx. 255, sem caminho (sem `\ / : * ? " < > \|`) |
| `pessoa` | Sim | Objeto |
| `nome` | Sim | Máx. 150; não pode ser vazio nem só espaços |
| `cpf` | Sim | Exatamente 11 dígitos numéricos (não confere dígito verificador) |
| `email` | Sim | Formato de e-mail válido |
| `telefone` | Sim | 10 ou 11 dígitos numéricos |
| `dataNascimento` | Sim | Data no passado |
| `empresa` | Sim | Objeto |
| `cnpj` | Sim | Exatamente 14 dígitos numéricos (não confere dígito verificador) |
| `razaoSocial` | Sim | Máx. 200; não pode ser vazio nem só espaços |
| `nomeFantasia` | Não | Máx. 200 |
| `dataAbertura` | Não | Qualquer data |

Datas sempre em `yyyy-MM-dd`. Campo obrigatório ausente, `null`, `""` ou `"   "` → **400** com o campo em `detalhes`.

**Regras de duplicidade**

| Situação | Resultado |
|---|---|
| IPR e CNPJ sem solicitação ativa | **201** `{ "id": "uuid" }` + header `Location: /solicitacoes/{id}` |
| IPR já usado numa solicitação **ativa** | **409** `Já existe uma solicitação ativa com este IPR` |
| CNPJ já usado numa solicitação **ativa** (com outro IPR) | **409** `Já existe uma solicitação ativa para este CNPJ` |
| IPR e/ou CNPJ só usados em solicitações **inativas** | **201**, nova solicitação com **novo id** |

- IPR e CNPJ são conferidos **cada um por si**. O **CPF pode repetir** à vontade.
- A resposta de sucesso traz **somente** o `id`.

---

## 3. `GET /solicitacoes/{id}` — consultar

```json
{
  "id": "uuid",
  "status": "AGUARDANDO_DOCUMENTO",
  "historico": [
    { "dataHora": "...", "status": "RECEBIDO", "descricao": "Solicitação criada" },
    { "dataHora": "...", "status": "AGUARDANDO_DOCUMENTO", "descricao": "Aguardando documentos até 02/11/2026 10:05:00. Enviados até o momento: 0 de 2" },
    { "dataHora": "...", "status": "AGUARDANDO_DOCUMENTO", "descricao": "Documento recebido. Enviados até o momento: 1 de 2" }
  ]
}
```

| Situação | Status |
|---|---|
| Existe | **200** |
| id não existe | **404** |
| id não é um UUID | **400** |

**Histórico**

- Em **ordem cronológica** (mais antigo primeiro).
- "Enviados até o momento: X de N" conta os PDFs recebidos **incluindo** o do próprio evento.
- Descrições não mostram nomes de arquivo nem dados pessoais (CPF, CNPJ, e-mail...).

| Evento | Status | Descrição |
|---|---|---|
| Criação | `RECEBIDO` | `Solicitação criada` |
| 10s depois | `AGUARDANDO_DOCUMENTO` | `Aguardando documentos até <dd/MM/yyyy HH:mm:ss>. Enviados até o momento: 0 de N` |
| PDF recebido | `AGUARDANDO_DOCUMENTO` | `Documento recebido. Enviados até o momento: X de N` |
| PDF com nome não declarado | `AGUARDANDO_DOCUMENTO` | `Documento rejeitado: arquivo não declarado na criação. Enviados até o momento: X de N` |
| Último PDF recebido | `EM_PROCESSAMENTO` | `Todos os documentos recebidos. Processamento iniciado` |
| Análise OK | `APROVADO` | `Análise concluída: todos os campos conferem. Solicitação aprovada` |
| Análise com divergência | `REPROVADO` | `Análise concluída: solicitação reprovada. Campos com divergência: cpf, nome` |
| Prazo vencido | `EXPIRADO` | `Prazo para envio dos documentos expirou. Enviados: X de N` |
| Cancelamento | `CANCELADO` | `Solicitação cancelada pelo solicitante` |

---

## 4. `POST /solicitacoes/{id}/documento` — enviar um PDF

**Um arquivo por requisição.** Body → **form-data**, chave `arquivo`, tipo **File**.

```bash
curl -X POST "http://localhost:8080/solicitacoes/{id}/documento" \
  -H "Authorization: Bearer <token>" \
  -F "arquivo=@documento-valido.pdf"
```

| Situação | Status | Body |
|---|---|---|
| PDF válido e declarado | **200** | `{ "mensagem": "Arquivo <nome declarado> enviado" }` |
| Status diferente de `AGUARDANDO_DOCUMENTO`, ou prazo vencido | **409** | |
| Arquivo declarado que **já foi recebido** | **409** | `O arquivo <nome> já foi recebido` |
| Nome fora da lista `arquivos` (comparação **exata**, diferencia maiúsculas/minúsculas) | **422** | `detalhes`: `nomeArquivo` |
| Sem a chave `arquivo`, arquivo vazio, não é PDF, corrompido ou sem texto (escaneado) | **400** | |
| Arquivo maior que 5MB | **413** | |

- O upload **não** compara o conteúdo: um PDF com dados errados é aceito (200) e a divergência aparece na análise.
- Quando o **último** PDF declarado chega, a solicitação vai para `EM_PROCESSAMENTO` (veja no GET).

**Layout do PDF** — uma linha por campo, `Rótulo: valor`. Linhas sem `:` são ignoradas (pode ter título). Rótulos não diferenciam maiúsculas, acentos nem hífen.

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

Para criar um PDF: escreva no Word e use **Salvar como → PDF**, com o nome exato declarado no POST.

---

## 5. Análise (automática, 30s após `EM_PROCESSAMENTO`)

Cada campo de **cada** PDF é comparado com o POST. Todos batem → **APROVADO**. Qualquer campo divergente ou ausente, em qualquer PDF → **REPROVADO**.

| Rótulo no PDF | Comparado com | Como compara |
|---|---|---|
| IPR | `ipr` | Texto: ignora maiúsculas e espaços extras |
| Nome | `pessoa.nome` | Texto |
| CPF | `pessoa.cpf` | Só os dígitos (aceita máscara) |
| E-mail | `pessoa.email` | Texto |
| Telefone | `pessoa.telefone` | Só os dígitos (aceita máscara) |
| Data de Nascimento | `pessoa.dataNascimento` | Data: `dd/MM/yyyy` ou `yyyy-MM-dd` |
| CNPJ | `empresa.cnpj` | Só os dígitos (aceita máscara) |
| Razão Social | `empresa.razaoSocial` | Texto |
| Nome Fantasia | `empresa.nomeFantasia` | Texto. Se não veio no POST, o PDF também não pode ter |

- **Data de Abertura não é comparada.**
- Campo que não aparece no PDF conta como reprovado.
- Na reprovação, o histórico lista **só os nomes** dos campos divergentes, sem valores.

---

## 6. `PATCH /solicitacoes/{id}/cancelamento` — cancelar

Sem body.

| Status atual | Resultado |
|---|---|
| `RECEBIDO`, `AGUARDANDO_DOCUMENTO`, `EM_PROCESSAMENTO` | **200**, vira `CANCELADO` (mesmo formato do GET) |
| `APROVADO`, `REPROVADO`, `CANCELADO`, `EXPIRADO` | **409** |

---

## Formato de erro

```json
{ "timestamp": "...", "status": 409, "mensagem": "Já existe uma solicitação ativa com este IPR" }
```

Erros de validação (400) e de arquivo não declarado (422) trazem também `detalhes`:

```json
{
  "timestamp": "...",
  "status": 400,
  "mensagem": "Um ou mais campos estão inválidos",
  "detalhes": [ { "campo": "cpf", "mensagem": "cpf deve conter 11 dígitos numéricos" } ]
}
```

- `detalhes` **só aparece quando tem itens**.
- `detalhes[].campo` mostra só o nome final (`pessoa.cpf` → `cpf`).
- Os **401** devolvem só `{ "mensagem": "..." }`.
- **LGPD:** nenhuma resposta, header ou histórico pode trazer CPF, CNPJ ou e-mail.

| Código | Quando |
|---|---|
| 200 | Token gerado, consulta, cancelamento, PDF enviado |
| 201 | Solicitação criada |
| 400 | Validação, JSON mal formado, data fora de `yyyy-MM-dd`, id não-UUID, problema no arquivo |
| 401 | Login inválido; token ausente, inválido ou expirado |
| 404 | Solicitação não encontrada, URL inexistente |
| 405 | Método HTTP não suportado na URL |
| 409 | IPR/CNPJ duplicado, cancelamento ou envio não permitido, arquivo já recebido |
| 413 | Arquivo maior que 5MB |
| 415 | Content-Type não suportado (criação exige `application/json`) |
| 422 | Arquivo não declarado na criação |
| 500 | Erro inesperado — **nenhuma requisição do cliente deveria provocar 500** |

---

## Como rodar e dados de teste

Requer JDK 21+.

```bash
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run        # Linux/Mac
```

- API: `http://localhost:8080`
- Banco H2 em memória (zera ao reiniciar): `http://localhost:8080/h2-console` · JDBC URL `jdbc:h2:mem:cadastrodb`

| Dado | Valor |
|---|---|
| Usuários do token | configurados em `application-local.properties` |
| CPF / CNPJ | Qualquer número com 11 / 14 dígitos |
| PDFs que batem com o body de exemplo | `docs/documento-valido.pdf`, `docs/documento-valido-2.pdf` (CPF `12345678909`, CNPJ `11222333000181`) |
| PDF divergente | `docs/documento-divergente.pdf` (CPF `52998224725`, razão social `Outra Empresa LTDA`, sem data de abertura) |
