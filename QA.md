# Desafio de QA 🐞

Esta branch (`qa`) tem **bugs plantados de propósito**. Sua missão: encontrar o máximo possível testando a API como caixa-preta.

## Regras do jogo

- A **especificação** é o [`README.md`](README.md). Qualquer comportamento diferente do README é bug.
- Teste pela API (Postman, curl, RestAssured...). Vale olhar o banco no H2 Console.
- **Não vale** olhar o código-fonte nem comparar com a branch `main` (`git diff`) — aí perde a graça 😉.
- Existem bugs de **validação de campos**, de **status code** e de **regra de negócio**, do fácil ao sutil.

## Dicas de onde estressar

- Campos obrigatórios: ausente, `null`, `""`, só espaços `"   "`.
- Limites: tamanho máximo, mínimo e exatamente no limite; datas de hoje, ontem e amanhã.
- Formatos: JSON mal formado, Content-Type errado, tipos errados.
- Status codes: compare **cada** resposta com a tabela do README (201 x 200, 400 x 404 x 409 x 422 x 500...).
- Regras: idempotência do `ipr`, conflito CPF+CNPJ, cancelamento em cada status, recriação após cancelado/expirado/reprovado.
- Upload: nome exato do arquivo, reenvio, arquivo inválido, sem arquivo.
- Histórico: ordem, contagens, textos.
- Análise: monte PDFs com um campo diferente por vez e veja se reprova.
- Headers e LGPD: leia **todas** as mensagens de erro e os headers da resposta.

> Os tempos (10s / 5min / 30s) ficam em `src/main/resources/application.properties` — pode diminuir para testar mais rápido.

## Modelo de reporte

```
ID: BUG-01
Título: [Endpoint] comportamento observado
Severidade: Crítica | Alta | Média | Baixa
Pré-condição:
Passos:
  1.
  2.
Resultado esperado: (cite a regra do README)
Resultado obtido: (status code + body)
Evidência: (print / curl / body)
```

Boa caçada!
