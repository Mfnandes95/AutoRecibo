# AutoRecibo — Emissão Rápida de Notas e Ordens de Serviço

Sistema SaaS para microempreendedores (MEI) e autônomos emitirem notas comerciais e ordens de serviço em poucos cliques, com geração de PDF pronto para impressão e assinatura — sem burocracia fiscal.

> ⚠️ Este projeto **não emite documentos fiscais eletrônicos** (NF-e/NFS-e). É um emissor de recibos/ordens de serviço para controle interno do prestador. Veja [Limites do Escopo](docs/scope-statement.md#2-limites-do-escopo-in--out) para mais detalhes.

---

## 📋 Sobre o Projeto

Microempreendedores e autônomos costumam formalizar vendas e serviços com recibos manuscritos ou planilhas improvisadas, gerando retrabalho e erros de cálculo. O **NotaCerta** resolve isso com um fluxo de emissão em **tela única**, cálculo de totais em tempo real e exportação em PDF — do cadastro do item à emissão final em menos de um minuto.

📄 Documentação completa do escopo: [`docs/scope-statement.md`](docs/scope-statement.md)

---

## ✨ Funcionalidades (MVP)

- 🔐 Cadastro e autenticação via Email, CPF ou CNPJ (senha com hash BCrypt)
- 📥 Importação em lote de produtos/serviços via planilha `.xlsx`
- 🧾 Emissão de notas/OS em fluxo de tela única, com cálculo em tempo real
- 💸 Aplicação de descontos pré-definidos por item (5%, 10%, 15%, 30%)
- 👁️ Etapa de revisão (*preview*) antes da emissão final
- 📄 Geração de PDF com campos de assinatura do contratante e do emissor

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0, Gradle |
| Banco de Dados | PostgreSQL |
| Frontend | HTML5, JavaScript Vanilla, Tailwind CSS |
| Importação de planilhas | Apache POI |
| Geração de PDF | Thymeleaf + OpenHTMLToPDF |

---

## 🏗️ Arquitetura

Arquitetura monolítica modular: o backend expõe uma API REST consumida pelo frontend (views server-side via Thymeleaf ou SPA desacoplada em JS Vanilla, conforme o módulo).

```
Frontend (HTML/JS/Tailwind)  ──HTTPS/JSON──▶  Backend (Spring Boot)  ──JDBC──▶  PostgreSQL
                              ◀────────────    Controllers → Services
                                                → Repositories (JPA)
```

Todo cálculo exibido em tempo real na tela de emissão é apenas UX — o backend **revalida e recalcula** todos os valores antes de persistir ou gerar o PDF, garantindo integridade mesmo diante de manipulação no cliente.

Detalhes completos de fluxo de dados: [`docs/scope-statement.md`](docs/scope-statement.md#3-arquitetura-e-fluxo-de-dados)

---

## 📦 Estrutura do Projeto

```
notacerta/
├── src/
│   ├── main/
│   │   ├── java/com/notacerta/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   └── dto/
│   │   └── resources/
│   │       ├── templates/       # Templates Thymeleaf (PDF, views)
│   │       ├── static/          # JS Vanilla, CSS/Tailwind
│   │       └── application.yml
│   └── test/
├── docs/
│   └── scope-statement.md       # Documento de Escopo do Projeto
├── build.gradle
└── README.md
```

---

## 🚀 Como Executar

### Pré-requisitos

- Java 21+
- PostgreSQL 14+
- Gradle (wrapper incluso)

### Passos

1. Clone o repositório:
   ```bash
   git clone https://github.com/seu-usuario/notacerta.git
   cd notacerta
   ```

2. Configure o banco de dados em `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/notacerta
       username: seu_usuario
       password: sua_senha
   ```

3. Execute as migrações e suba a aplicação:
   ```bash
   ./gradlew bootRun
   ```

4. Acesse em `http://localhost:8080`

---

## 🔌 Principais Endpoints da API

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/auth/registro` | Cadastro de usuário |
| `POST` | `/api/auth/login` | Autenticação (JWT) |
| `POST` | `/api/catalogo/importar` | Importação de planilha `.xlsx` |
| `GET` | `/api/catalogo` | Lista produtos/serviços do usuário |
| `POST` | `/api/documentos/preview` | Gera espelho de revisão do documento |
| `POST` | `/api/documentos` | Persiste e emite o documento |
| `GET` | `/api/documentos/{id}/pdf` | Download do PDF gerado |

---

## 🗺️ Roadmap (fora do MVP)

Funcionalidades avaliadas para versões futuras, fora do escopo atual:

- Integração fiscal (NF-e/NFS-e via SEFAZ)
- Controle de estoque avançado
- Emissão de boletos/cobrança
- Múltiplos usuários por conta (equipes e permissões)

Veja a justificativa completa em [Limites do Escopo](docs/scope-statement.md#2-limites-do-escopo-in--out).

---

## 📄 Licença

Defina aqui a licença do projeto (ex.: MIT, Apache 2.0) antes da publicação pública do repositório.
