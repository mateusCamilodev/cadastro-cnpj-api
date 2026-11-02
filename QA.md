# Desafio de QA 🐞

Esta branch (`qa`) tem **bugs plantados de propósito**. Sua missão: encontrar o máximo possível testando a API como caixa-preta.

## Regras do jogo

- A **especificação** é o [`FLUXO.md`](FLUXO.md) (fluxo completo e cada endpoint). O [`README.md`](README.md) tem o mesmo conteúdo, em resumo. Qualquer comportamento diferente do que está lá é bug.
- Teste pela API (Postman, curl, RestAssured...). Vale olhar o banco no H2 Console.
- **Não vale** olhar o código-fonte nem comparar com a branch `main` (`git diff`) — aí perde a graça 😉.
- Há bugs de **validação de campos**, de **status code**, de **regra de negócio**, de **token** e de **LGPD/formato de resposta**, do fácil ao sutil.

## Antes de começar

1. Gere um token em `POST /auth/token` com o e-mail e a senha configurados em `application-local.properties`.
2. Mande `Authorization: Bearer <accessToken>` em todas as requests de `/solicitacoes`.
3. Para testar mais rápido, diminua os tempos em `src/main/resources/application.properties` (`solicitacao.tempo-recebido`, `solicitacao.prazo-documento`, `solicitacao.tempo-processamento`).

## Dicas de onde estressar

- **Campos obrigatórios:** ausente, `null`, `""`, só espaços `"   "`. Vale para os objetos `pessoa` e `empresa` também.
- **Limites:** tamanho mínimo/máximo e exatamente no limite; datas de ontem, hoje e amanhã; listas com 10 e 11 itens.
- **Formato:** JSON mal formado, Content-Type errado, id que não é UUID.
- **Status codes:** compare **cada** resposta com as tabelas do FLUXO.md (200 x 201, 400 x 404 x 409 x 422 x 500). Nenhuma requisição deveria dar 500.
- **Token:** login certo e errado (e-mail e senha separadamente), body incompleto, quanto tempo o token dura de verdade (dica: o token é um JWT — cole em jwt.io).
- **Regras de criação:** IPR repetido, CNPJ repetido, CPF repetido; e depois de cancelar/expirar/reprovar.
- **Upload:** nome exato do arquivo, reenvio, arquivo inválido, sem arquivo, ordem de envio diferente da declarada. Declare **3 arquivos** e acompanhe o status a cada envio.
- **Histórico:** ordem dos eventos, contagens "X de N", textos.
- **Análise:** monte PDFs mudando **um campo por vez** (nome, CPF, nome fantasia...) e veja se reprova. Teste com 2 ou mais PDFs em que só o segundo diverge.
- **LGPD e formato:** leia **todas** as mensagens, o histórico e os campos de cada resposta de erro.

Para criar PDFs: escreva no Word, uma linha por campo (`Rótulo: valor`, veja o FLUXO.md), e use **Salvar como → PDF** com o nome exato declarado.

## Modelo de reporte

```
ID: BUG-01
Título: [Endpoint] comportamento observado
Severidade: Crítica | Alta | Média | Baixa
Pré-condição:
Passos:
  1.
  2.
Resultado esperado: (cite a regra do FLUXO.md)
Resultado obtido: (status code + body)
Evidência: (print / curl / body)
```

Boa caçada!
