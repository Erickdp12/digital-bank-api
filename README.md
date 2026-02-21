# 🏦 Digital Bank API

Uma API REST simples em Java utilizando Spring Boot que simula operações de um banco digital.

Este projeto permite:

✅ Criar contas  
✅ Consultar saldo  
✅ Depositar  
✅ (Você pode adicionar mais funcionalidades depois)

---

## 🚀 Tecnologias

Este projeto foi desenvolvido com:

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- H2 Database (em memória)
- Lombok
- Maven

---

## 📦 Estrutura de Endpoints

POST /accounts


Body JSON:

json
{
  "holderName": "Erick"
}
--------------------------------------------------------------------------------------------
Listar Contas
GET /accounts
--------------------------------------------------------------------------------------------
Consultar Saldo
GET /accounts/{id}/balance
--------------------------------------------------------------------------------------------
Depositar
POST /accounts/{id}/deposit?amount=100
--------------------------------------------------------------------------------------------
Banco de Dados

Este projeto utiliza o H2 em memória.

Você pode acessar o console H2:

http://localhost:8080/h2-console

JDBC URL:

jdbc:h2:mem:testdb


### 🆕 Criar Conta
