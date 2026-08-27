# LeadHunter API

Backend REST API para uma plataforma de prospecção comercial.

O objetivo da plataforma é permitir que freelancers, vendedores e agências encontrem potenciais clientes, organizem seus leads, registrem contatos e acompanhem o processo de venda.

## 🚀 Tecnologias

* Java 21
* Spring Boot
* Spring Security
* JWT
* Spring Data JPA
* Hibernate
* PostgreSQL
* Maven
* Docker
* Swagger / OpenAPI

## 📌 Funcionalidades

### Autenticação

* Cadastro
* Login
* Autenticação JWT
* Consulta do usuário autenticado
* Atualização de perfil
* Alteração de senha
* Exclusão de conta

### Leads

* Criar lead
* Listar leads
* Buscar lead por ID
* Atualizar lead
* Excluir lead
* Alterar status
* Favoritar lead
* Filtrar leads
* Paginar resultados
* Ordenar resultados
* Calcular oportunidade do lead

### CRM

Os leads possuem os seguintes estados:

```text
NEW
CONTACTED
INTERESTED
NEGOTIATING
CUSTOMER
LOST
```

### Notas

Cada lead pode possuir várias notas.

### Interações

O sistema registra contatos realizados com os leads.

Tipos de interação:

```text
WHATSAPP
PHONE
EMAIL
INSTAGRAM
MEETING
OTHER
```

### Dashboard

O dashboard apresenta:

* Total de leads
* Leads novos
* Leads contatados
* Leads interessados
* Leads em negociação
* Clientes
* Leads perdidos
* Taxa de conversão

### Busca de leads

A plataforma terá integração com fontes externas para encontrar potenciais clientes.

O usuário poderá pesquisar utilizando critérios como:

* Categoria
* Cidade
* Estado
* Possui site
* Possui WhatsApp
* Possui Instagram

Os resultados poderão ser importados para o CRM.

---

# 🔐 Autenticação

A API utiliza JWT.

Após realizar login, o cliente deve enviar:

```http
Authorization: Bearer {token}
```

Exemplo:

```http
Authorization: Bearer eyJhbGciOiJIUzI1Ni...
```

---

# 📚 API

## Auth

### Registrar usuário

```http
POST /api/auth/register
```

Request:

```json
{
  "name": "William",
  "email": "william@email.com",
  "password": "123456"
}
```

### Login

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "william@email.com",
  "password": "123456"
}
```

---

# 👤 Usuário

### Consultar perfil

```http
GET /api/users/me
```

### Atualizar perfil

```http
PUT /api/users/me
```

### Alterar senha

```http
PATCH /api/users/me/password
```

### Excluir conta

```http
DELETE /api/users/me
```

---

# 🎯 Leads

### Criar lead

```http
POST /api/leads
```

### Listar leads

```http
GET /api/leads
```

Filtros disponíveis:

```text
search
category
city
state
status
source
favorite
hasWebsite
hasWhatsapp
```

Exemplo:

```http
GET /api/leads?city=Salvador&status=NEW&page=0&size=20
```

### Buscar lead

```http
GET /api/leads/{id}
```

### Atualizar lead

```http
PUT /api/leads/{id}
```

### Excluir lead

```http
DELETE /api/leads/{id}
```

### Alterar status

```http
PATCH /api/leads/{id}/status
```

Request:

```json
{
  "status": "CONTACTED"
}
```

### Favoritar

```http
POST /api/leads/{id}/favorite
```

### Desfavoritar

```http
DELETE /api/leads/{id}/favorite
```

---

# 📝 Notas

### Criar nota

```http
POST /api/leads/{leadId}/notes
```

### Listar notas

```http
GET /api/leads/{leadId}/notes
```

### Atualizar nota

```http
PUT /api/notes/{id}
```

### Excluir nota

```http
DELETE /api/notes/{id}
```

---

# 📞 Interações

### Criar interação

```http
POST /api/leads/{leadId}/interactions
```

Request:

```json
{
  "type": "WHATSAPP",
  "description": "Enviei uma proposta comercial."
}
```

### Histórico

```http
GET /api/leads/{leadId}/interactions
```

### Excluir interação

```http
DELETE /api/interactions/{id}
```

---

# 📊 Dashboard

### Resumo

```http
GET /api/dashboard/summary
```

### Leads por status

```http
GET /api/dashboard/leads-by-status
```

### Interações

```http
GET /api/dashboard/interactions
```

---

# 🔎 Busca

### Pesquisar leads

```http
POST /api/search/leads
```

Request:

```json
{
  "category": "Restaurante",
  "city": "Salvador",
  "state": "BA"
}
```

### Histórico de pesquisas

```http
GET /api/search/history
```

### Importar resultado

```http
POST /api/search/{id}/import
```

---

# 🗄️ Banco de dados

Principais entidades:

```text
User
Lead
Note
Interaction
```

Relacionamentos:

```text
User 1 ──── N Lead

Lead 1 ──── N Note

Lead 1 ──── N Interaction
```

---

# 🧪 Testes

O projeto deve possuir testes para:

* Services
* Controllers
* Repositories
* Autenticação
* Autorização
* Validação dos DTOs

Executar:

```bash
./mvnw test
```

---

# 🐳 Docker

Subir os serviços:

```bash
docker compose up -d
```

Parar:

```bash
docker compose down
```

---

# 📖 Documentação da API

A documentação será disponibilizada através do Swagger/OpenAPI.

Após iniciar a aplicação:

```text
/swagger-ui/index.html
```

---

# 🛠️ Status do projeto

### MVP

* [ ] Configuração do projeto
* [ ] Banco de dados
* [ ] User
* [ ] Authentication
* [ ] JWT
* [ ] Lead
* [ ] Filtros
* [ ] Notes
* [ ] Interactions
* [ ] Dashboard
* [ ] Swagger
* [ ] Testes

### Versões futuras

* [ ] Busca automática de empresas
* [ ] Integração com fontes externas
* [ ] Score de oportunidade
* [ ] IA para qualificação de leads
* [ ] Geração de mensagens
* [ ] Automação de follow-up
* [ ] Planos de assinatura
* [ ] Pagamentos
* [ ] Equipes
* [ ] Analytics avançado
