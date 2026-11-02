# Desafio de QA 🐞

Esta branch (`qa`) tem bugs. Sua missão: encontrar o máximo possível testando a API.

## Regras do jogo

- O [`FLUXO.md`](FLUXO.md) explica como a API **deveria** funcionar: o fluxo completo e cada endpoint. Qualquer comportamento diferente do que está lá é bug.
- Teste pela API (Postman, curl, RestAssured...). Vale olhar o banco no H2 Console.
- **Não vale** olhar o código-fonte nem comparar com a branch `main`.

## Antes de começar

1. Copie `src/main/resources/application-local.example.properties` para `application-local.properties` (mesma pasta) e preencha com o usuário, a senha e o segredo que você recebeu.
2. Suba a API (veja "Como rodar" no `FLUXO.md`).
3. Gere um token em `POST /auth/token` com esse usuário.
4. Mande `Authorization: Bearer <accessToken>` nas requests de `/solicitacoes`.
5. Para testar mais rápido, dá para diminuir os tempos em `src/main/resources/application.properties`.

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
