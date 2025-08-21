AnySale

Gerenciador de leads com sugestões automáticas e notificações.
Arquitetura hexagonal · Spring Boot · Kafka · Postgres (leads) · MongoDB (catálogo).

Sumário

Visão geral
Módulos (Maven)
Stack & Pré-requisitos
Infra (Docker Compose)
Configuração por serviço
Lead Service (8080)
Catalog Service (8082)
Notification Service (8081)
Ingestion Gateway (8083)
Build & Run
Endpoints principais
Eventos (Kafka)
Modelo de dados (Postgres)
Troubleshooting
Roadmap curto
Appendix A — Criação de tópicos Kafka via Spring

Visão geral
Fluxo: ingestion-gateway → lead-service → (Kafka) → catalog-service → lead-service → notification-service
Eventos: lead.created (novo lead) · lead.updated (mudanças/sugestões)

Arquitetura por serviço (hexagonal):
com.anysale.<service>
├─ domain/            # regras e modelos de domínio
├─ application/       # casos de uso
├─ adapters/
│   ├─ in/            # REST, Messaging (Kafka)
│   └─ out/           # Persistence (JPA/Mongo), HTTP (WebClient), Messaging (Kafka)
└─ config/            # beans infra (tópicos Kafka, etc.)

Módulos (Maven)
anysale-parent/                 # parent (packaging=pom)
├─ infra/docker-compose.yml   # Postgres, Mongo, Kafka
├─ anysale-shared-kernel/     # VOs/util (sem Spring)
├─ anysale-contracts/         # DTOs de eventos (Kafka) compartilhados
├─ anysale-lead-service/      # JPA/Postgres + produz eventos
├─ anysale-catalog-service/   # Mongo + consome lead.created + sugere
├─ anysale-notification-service/ # consome lead.updated + histórico
└─ anysale-ingestion-gateway/ # WebFlux → proxy/normalizador de leads

Stack & Pré-requisitos
Java 17, Maven 3.9+, Docker + Docker Compose
Spring Boot 3.5.x · Web/MVC · WebFlux (gateway) · Data JPA/Mongo · Kafka · Flyway · Actuator
IntelliJ IDEA (habilite Lombok)
Infra (Docker Compose)
Arquivo: infra/docker-compose.yml

Subir:
docker compose -f infra/docker-compose.yml up -d

Resumo dos serviços:
Postgres 16 (localhost:5432, db/user/pass anysale / anysale / secret)
Mongo 6 (localhost:27017, sem auth no dev)
Kafka 7.6.x (localhost:9092, auto-create de tópicos habilitado no dev)

1) anysale-ingestion-gateway (WebFlux)
Função: porta de entrada “universal” para receber leads brutos vindos de formulários, ads, parceiros etc.
Trabalho principal: normaliza o payload (mapeia campos como full_name, desired_category, desired_tags…) e repassa para o lead-service via HTTP.
Por que existe: desacopla integrações externas do core; aqui você coloca retries/circuit breakers (Resilience4j) e regras específicas de parceiros.
Interface: POST /v1/ingest/lead.

2) anysale-lead-service (Core/CRUD + eventos)
Função: fonte de verdade dos Leads. Faz CRUD, regras de negócio e publica eventos.
Dados que possui (Postgres): lead, lead_suggestion (e, no futuro, activity).
Eventos Kafka:
Produz: lead.created (quando cria) e lead.updated (mudanças de estágio, sugestões anexadas).
Chamadas de entrada (REST):

POST /v1/leads — cria lead (idempotência opcional).
PATCH /v1/leads/{id}/stage — muda estágio (valida transições).
PATCH /v1/leads/{id}/suggestions — anexa sugestões.
GET /v1/leads/{id} — detalhe.
Por que existe: concentra regra de negócio do funil e contratos de evento; 
outros serviços não alteram lead direto no DB — pedem via API.

3) anysale-catalog-service (Sugestões)
Função: cuidar do catálogo de produtos (MongoDB) e gerar sugestões para um lead recém-criado.
Fluxo:
Consome lead.created.
Calcula top 3 itens (regras/heurísticas) e chama PATCH /v1/leads/{id}/suggestions no lead-service.
O lead-service anexa e então publica lead.updated.
Chamadas de entrada (REST): POST /v1/products (seed/bulk), GET /v1/products.
Por que existe: separar o domínio “catálogo & recomendação” do “funil de leads”;
pode evoluir para IA sem tocar o core.

4) anysale-notification-service (Notificações)
Função: reagir às mudanças do lead e disparar/registrar notificações para o time (hoje: armazenado em memória; próximo passo: persistir).
Fluxo:
Consome lead.updated.
Gera entradas de histórico e (futuramente) envia e-mail/SMS/WhatsApp, webhooks, etc.
Chamadas de entrada (REST): GET /v1/notifications/{leadId} para consultar histórico.
Por que existe: orquestração de follow-up e comunicação assíncrona, sem poluir o serviço de lead.

5) anysale-contracts (Contratos compartilhados)
Função: DTOs de evento e (se necessário) DTOs REST compartilhados.
Conteúdo: LeadCreatedEvent, LeadUpdatedEvent (POJOs), com versão de evento.
Por que existe: garantir compatibilidade entre produtores/consumidores (todos dependem deste módulo).

6) anysale-shared-kernel (Kernel compartilhado)
Função: utilidades e Value Objects (ex.: Email, Phone, Result, DomainException).
Sem Spring, apenas Java puro.
Por que existe: evitar duplicação de conceitos básicos de domínio em vários serviços.

7) anysale-parent (Maven parent)
Função: centraliza versões (Java 17, Spring Boot, plugins), e agrega os módulos com <modules>.
Por que existe: padroniza build e simplifica manutenção de dependências.

Build & Run
# 0) infraestrutura
docker compose -f infra/docker-compose.yml up -d
# 1) build (raiz do repo)
mvn -U -T 1C clean package
# 2) subir serviços (cada um em um terminal ou pela IDE)
mvn -f anysale-lead-service spring-boot:run
mvn -f anysale-catalog-service spring-boot:run
mvn -f anysale-notification-service spring-boot:run
mvn -f anysale-ingestion-gateway spring-boot:run

# 3) health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

Endpoints principais

Lead Service (8080)

POST /v1/leads

{
"name":"João","email":"joao@ex.com","phone":"+55...",
"source":"MarketplaceX","desiredCategory":"home-office",
"desiredTags":["cadeira","ergonômica"]
}


PATCH /v1/leads/{id}/stage → {"stage":"WON"}
PATCH /v1/leads/{id}/suggestions

{ "suggestions":[
{"productId":"p1","title":"Cadeira Ergo A","price":520,"currency":"BRL","vendor":"LojaX"}
]}


GET /v1/leads/{id}
Catalog Service (8082)
POST /v1/products (seed) · GET /v1/products
Notification Service (8081)
GET /v1/notifications/{leadId}
Ingestion Gateway (8083)
POST /v1/ingest/lead (payload bruto → normalizado e enviado ao Lead Service)

Eventos (Kafka)
lead.created — produzido pelo lead-service, consumido pelo catalog-service
lead.updated — produzido pelo lead-service, consumido pelo notification-service

Contratos (módulo anysale-contracts)
com.anysale.contracts.events.LeadCreatedEvent
com.anysale.contracts.events.LeadUpdatedEvent

Modelo de dados (Postgres)
lead: id UUID PK, name, email, phone, source, desired_category, desired_tags TEXT[], stage, created_at, updated_at
lead_suggestion: id, lead_id FK, product_id, title, price, currency, vendor, created_at
Migrations via Flyway: anysale-lead-service/src/main/resources/db/migration/V1__init.sql

Troubleshooting
Kafka deserialização (ClassNotFound)
Garanta spring.kafka.*.trusted.packages=com.anysale.contracts.events em todos os consumidores e que os produtores enviem DTOs do módulo contracts.
Se já houver mensagens antigas incompatíveis, troque o group-id ou limpe o tópico.

Transaction synchronization is not active
Use @org.springframework.transaction.annotation.Transactional (Spring) e publique eventos após o commit (ou adote Outbox).

Mongo/DB “connection refused”
App em container deve usar mongo-anysale / pg-anysale (não localhost).

Flyway checksum mismatch
Não edite migrations aplicadas. 
Crie uma nova V2__. Para ambiente local, flyway repair pode ser usado conscientemente.

Roadmap curto
Outbox para publicação idempotente
Persistir notificações (Postgres)
Autenticação/RBAC + multi-tenant
Observabilidade (Prometheus/Grafana/OTel)
Conectores (Facebook Lead Ads / Shopify / Woo)
Appendix A — Criação de tópicos Kafka via Spring
Crie esta classe no Lead Service para garantir os tópicos no start:
anysale-lead-service/src/main/java/com/anysale/lead/config/KafkaTopicsConfig.java



