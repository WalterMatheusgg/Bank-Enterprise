<div align="center">

<img src="docs/assets/banner.svg" alt="NexusBank Enterprise | Sistema Bancário Empresarial" width="100%">

# 🏦 Sistema Bancário Empresarial
### NexusBank Enterprise · Java 21 · API REST + Painel Web

**Gerencie contas fictícias, realize créditos demonstrativos, execute transferências transacionais e consulte o histórico em uma interface web conectada ao Spring Boot.**

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)

[![Java CI](https://github.com/WalterMatheusgg/Sistema-Bancario-Empresarial/actions/workflows/ci.yml/badge.svg)](https://github.com/WalterMatheusgg/Sistema-Bancario-Empresarial/actions/workflows/ci.yml)
[![MIT](https://img.shields.io/badge/License-MIT-36D3B4)](LICENSE)

[Interface](#-interface-web) · [Funcionalidades](#-funcionalidades) · [Instalação](#-como-executar) · [API](#-endpoints) · [Arquitetura](#-arquitetura)

</div>

---

## ✨ Sobre o projeto

O **NexusBank Enterprise** é um projeto de portfólio com backend **Java 21, Spring Boot 3, PostgreSQL e Flyway**. Além da API original, agora possui um painel bancário responsivo com fluxos reais integrados ao servidor: abertura de contas, crédito fictício, transferências e histórico.

A aplicação explora **transações ACID, locks pessimistas, idempotência, API REST, persistência com JPA e testes automatizados**.

> [!IMPORTANT]
> **Projeto educacional.** Não é um banco real e não faz pagamentos ou transferências financeiras externas. Os créditos adicionados são somente valores simulados para testar a aplicação.

## 🖥️ Interface web

<div align="center">

<img width="100%" src="docs/assets/dashboard-real.png" alt="Captura real do dashboard NexusBank: saldos, contas bancárias e movimentações de demonstração">


</div>



## 🚀 Funcionalidades

| Área | Funcionalidades |
|---|---|
| 💳 Contas | Abertura, consulta, saldos e listagem paginada |
| 🪙 Crédito de demonstração | Saldo fictício com auditoria própria e chave de idempotência |
| 🔁 Transferências | Débito e crédito atômicos com saldo não negativo |
| 🔒 Concorrência | Bloqueios pessimistas em ordem determinística entre contas |
| 🔑 Idempotência | Repetir a mesma requisição não repete a operação |
| 📒 Ledger | Duas entradas por transferência: débito e crédito |
| 🧾 Histórico | Registros de depósitos simulados e lançamentos de transferências |
| 🛡️ Segurança | HTTP Basic, BCrypt e validação de entradas |
| 📚 Documentação | Swagger UI e OpenAPI |
| 🧪 Testes | JUnit 5, MockMvc, H2, Maven e GitHub Actions |
| 🐳 Infraestrutura | Docker Compose e PostgreSQL com volume persistente |

## 🧰 Tecnologias

**Backend:** Java 21 · Spring Boot 3.5.6 · Spring Web · Spring Security · Spring Data JPA · Hibernate · Jakarta Validation

**Banco:** PostgreSQL 16 · H2 em memória · Flyway · SQL

**Web:** HTML5 · CSS3 · JavaScript ES2022 · Fetch API

**Ferramentas:** Maven · JUnit · Spring Security Test · Swagger/OpenAPI · Docker · GitHub Actions · Playwright (captura real)

## 🏗️ Arquitetura

~~~mermaid
flowchart TB
    USER["Painel web / Swagger / curl"] --> AUTH["Spring Security HTTP Basic"]
    AUTH --> API["Controllers /api/v1"]
    API --> ACCOUNT["Contas"]
    API --> DEPOSIT["Créditos fictícios"]
    API --> TRANSFER["TransferService @Transactional"]
    TRANSFER --> LEDGER["Ledger: DEBIT + CREDIT"]
    ACCOUNT --> DB[("PostgreSQL ou H2")]
    DEPOSIT --> DB
    TRANSFER --> DB
    LEDGER --> DB
    FLYWAY["Migrações Flyway V1 e V2"] --> DB
~~~

~~~text
src/main/
├── java/dev/nexusbank/
│   ├── account/             # Contas e listagem
│   ├── deposit/             # Créditos fictícios auditados
│   ├── transfer/            # Transferências transacionais
│   ├── ledger/              # Registros contábeis de transferências
│   └── config/              # Autenticação e tratamento de erros
└── resources/
    ├── application.yml
    ├── db/migration/        # V1 e V2 (sem sobrescrever versões)
    └── static/              # Painel real: HTML, CSS e JavaScript
src/test/java/dev/nexusbank/   # Testes de API e de integração
docs/capture-dashboard.mjs     # Captura com servidor e navegador reais
~~~

## ⚡ Como executar

### Alternativa 1 — Java e Maven (H2 local)

Requisitos: **JDK 21** e **Maven 3.9+**.

~~~bash
git clone https://github.com/WalterMatheusgg/Sistema-Bancario-Empresarial.git
cd Sistema-Bancario-Empresarial
mvn clean verify
mvn spring-boot:run
~~~

Abra **http://localhost:8080** para o painel ou **http://localhost:8080/swagger-ui/index.html** para o Swagger.

Somente em ambiente local, as credenciais de exemplo são:

- **Usuário:** admin
- **Senha:** dev-only-change-me

**Importante:** H2 é um banco em memória; ao reiniciar a aplicação, os dados de demonstração são descartados. Substitua a senha padrão se acessar a aplicação fora de sua máquina.

### Alternativa 2 — Docker Compose + PostgreSQL

~~~bash
cp .env.example .env
# Edite o .env e defina senhas fortes.
docker compose up --build -d
docker compose ps
~~~

Acesse **http://localhost:8080**. A API fica exposta apenas em <code>127.0.0.1:8080</code>, e os dados do PostgreSQL persistem no volume <code>postgres_data</code>.

~~~bash
docker compose down
~~~

> [!CAUTION]
> Não publique seu arquivo <code>.env</code> e não use <code>docker compose down -v</code> se quiser preservar os dados.

## 🔌 Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | <code>/actuator/health</code> | Saúde da aplicação (público) |
| POST | <code>/api/v1/accounts</code> | Criar conta |
| GET | <code>/api/v1/accounts?page=0&size=50</code> | Listar contas (até 100 por página) |
| GET | <code>/api/v1/accounts/{id}</code> | Consultar saldo |
| POST | <code>/api/v1/accounts/{id}/deposits</code> | Adicionar crédito fictício |
| GET | <code>/api/v1/accounts/{id}/deposits</code> | Consultar créditos fictícios |
| POST | <code>/api/v1/transfers</code> | Transferir entre contas |
| GET | <code>/api/v1/transfers/{id}</code> | Consultar transferência |
| GET | <code>/api/v1/accounts/{id}/ledger</code> | Consultar registros de transferências |

As operações de negócio exigem autenticação. Créditos e transferências utilizam o cabeçalho <code>Idempotency-Key</code>.

### Exemplo: criar uma conta

~~~bash
curl -u admin:dev-only-change-me \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Empresa Exemplo"}' \
  http://localhost:8080/api/v1/accounts
~~~

### Exemplo: adicionar saldo fictício

Substitua o identificador pelo UUID retornado ao criar a conta.

~~~bash
curl -u admin:dev-only-change-me \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: teste-credito-001" \
  -d '{"amount":250.00}' \
  http://localhost:8080/api/v1/accounts/UUID_DA_CONTA/deposits
~~~

### Exemplo: transferir entre duas contas

~~~bash
curl -u admin:dev-only-change-me \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: teste-transferencia-001" \
  -d '{"fromAccountId":"UUID_ORIGEM","toAccountId":"UUID_DESTINO","amount":50.00}' \
  http://localhost:8080/api/v1/transfers
~~~

**Use uma chave inédita por operação nova.** A repetição da mesma chave com os mesmos dados recupera a operação anterior, sem duplicar o saldo.

## 🧪 Testes e qualidade

~~~bash
mvn clean verify
node --check src/main/resources/static/assets/app.js
~~~

Os testes verificam crédito fictício, idempotência, conflito de chave, fluxo crédito → transferência, lançamentos do ledger, saldo insuficiente, autenticação da API e recursos do frontend.

O [workflow Java CI](.github/workflows/ci.yml) valida Java e JavaScript. O [workflow de screenshot](.github/workflows/dashboard-screenshot.yml) inicia a aplicação com H2, cria transações demonstrativas e captura o painel em Chromium.

## 🔐 Segurança e limitações

Este projeto **não está pronto para produção bancária**:

- Autenticação demonstrativa por **uma conta administrativa HTTP Basic**; ainda não há perfis de usuários, clientes separados nem OAuth2.
- Os créditos fictícios possuem auditoria própria, mas **o ledger em partidas de débito e crédito cobre somente as transferências**.
- A exclusividade da chave de idempotência é garantida no banco; ainda são necessários tratamentos extras para algumas disputas simultâneas entre contas distintas.
- O dashboard mostra a primeira página de até 50 contas e histórico limitado por conta.
- Antes de um cenário produtivo seriam necessários **TLS, controle de acesso individual, proteção CSRF compatível com autenticação de navegador, gestão de segredos, reconciliação contábil, backup e testes concorrentes com PostgreSQL**.

## 🗺️ Próximas evoluções

- [ ] Identidades e permissões por usuário
- [ ] Ledger contábil completo para todas as movimentações
- [ ] Testes de concorrência com PostgreSQL/Testcontainers
- [ ] Pesquisa, filtros e paginação no painel
- [ ] Auditoria operacional, métricas e deploy seguro

---

<div align="center">

### 👨‍💻 Walter Matheus
Desenvolvedor Full Stack · Java · Spring Boot · Angular

[![GitHub](https://img.shields.io/badge/GitHub-WalterMatheusgg-181717?style=for-the-badge&logo=github)](https://github.com/WalterMatheusgg)
[![Projeto NEXUS CRM](https://img.shields.io/badge/Outro_Projeto-NEXUS_CRM-4C81E7?style=for-the-badge)](https://github.com/WalterMatheusgg/NEXUS)

Distribuído sob licença [MIT](LICENSE).

</div>
