AnySale

Sistema para gerenciar leads e acelerar fechamento de vendas de qualquer produto, com sugestões automáticas e notificações.
Arquitetura hexagonal, serviços Spring Boot, mensageria Kafka, dados em PostgreSQL (leads) e MongoDB (catálogo).

Sumário

Arquitetura (visão rápida)

Tecnologias

Módulos (Maven)

Pré-requisitos

Infraestrutura (Docker Compose)

Configuração por serviço (application.properties)

Build & Run

Fluxo de eventos (Kafka)

API (endpoints principais)

Modelo de dados (lead + tags)

Testes (opcional)

Troubleshooting

Roadmap

Arquitetura (visão rápida)

Hexagonal por serviço (ports & adapters):

com.anysale.<service>
├─ domain/               # regras e modelos de domínio
├─ application/          # casos de uso (opcional, pode crescer aqui)
├─ adapters/
│   ├─ in/               # REST, Messaging (Kafka)
│   └─ out/              # Persistence (JPA/Mongo), HTTP (WebClient), Messaging (Kafka)
└─ config/               # beans infra (tópicos Kafka, etc.)


Mensageria:

lead.created → publicado pelo lead-service, consumido pelo catalog-service

lead.updated → publicado pelo lead-service, consumido pelo notification-service

Tecnologias

Java 17, Maven 3.9+, Spring Boot 3.5.4

Spring Web (MVC), Spring WebFlux/WebClient (gateway / chamadas internas)

Spring Data JPA (PostgreSQL), Spring Data MongoDB (catálogo)

Apache Kafka (spring-kafka), Resilience4j (retry/circuit breaker) onde aplicável

Flyway (migrations DB), Actuator, Jakarta Validation

Lombok (DTOs simples, menos boilerplate)

Módulos (Maven)

anysale-parent (pom) – agregador/parent, Java 17, Boot 3.5.4, versões/plugins; lista os <modules>.

anysale-shared-kernel (jar) – VOs/util (ex.: Email, Phone, Result, DomainException). Sem Spring.

anysale-contracts (jar) – DTOs de eventos Kafka (ex.: LeadCreated, LeadUpdated) e DTOs compartilhados. Sem Spring.

anysale-lead-service (jar) – CRUD de Lead/Suggestion via JPA + PostgreSQL; produz eventos lead.created e lead.updated.

anysale-catalog-service (jar) – MongoDB para produtos; consome lead.created, calcula top3 e chama PATCH no lead-service.

anysale-notification-service (jar) – consome lead.updated e expõe endpoint de histórico (simulado in-memory).

anysale-ingestion-gateway (jar) – WebFlux: recebe payload bruto (/v1/ingest/lead) e repassa pro lead-service via HTTP; pode usar Resilience4j.

Estrutura do repo:

<repo-raiz>/
├─ pom.xml                          # parent (packaging=pom)
├─ infra/
│  └─ docker-compose.yml            # Postgres, Mongo, Kafka/ZooKeeper
├─ anysale-shared-kernel/
├─ anysale-contracts/
├─ anysale-lead-service/
├─ anysale-catalog-service/
├─ anysale-notification-service/
└─ anysale-ingestion-gateway/

Pré-requisitos

JDK 17 (java -version)

Maven 3.9+ (mvn -v)

Docker + Docker Compose

IntelliJ IDEA (recomendado) + plugin Lombok habilitado

Dica IntelliJ: Settings → Build Tools → Maven → JDK for importer = 17 e Runner = 17.

Infraestrutura (Docker Compose)

Arquivo: infra/docker-compose.yml (na raiz do repo)

version: "3.9"
services:
postgres:
image: postgres:16
container_name: pg-anysale
environment:
POSTGRES_DB: anysale
POSTGRES_USER: anysale
POSTGRES_PASSWORD: secret
ports: ["5432:5432"]
volumes: ["pgdata:/var/lib/postgresql/data"]

mongo:
image: mongo:6
container_name: mongo-anysale
ports: ["27017:27017"]

zookeeper:
image: confluentinc/cp-zookeeper:7.6.1
container_name: zk-anysale
environment:
ZOOKEEPER_CLIENT_PORT: 2181
ports: ["2181:2181"]

kafka:
image: confluentinc/cp-kafka:7.6.1
container_name: kafka-anysale
depends_on: [zookeeper]
ports: ["9092:9092"]
environment:
KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
volumes:
pgdata:


Subir a infra:

docker compose -f infra/docker-compose.yml up -d


Ver logs do Kafka (opcional):

docker logs -f kafka-anysale

Configuração por serviço (application.properties)
anysale-lead-service/src/main/resources/application.properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/anysale
spring.datasource.username=anysale
spring.datasource.password=secret
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.group-id=lead-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.*
spring.kafka.properties.spring.json.add.type.headers=true

management.endpoints.web.exposure.include=health,info,metrics

anysale-catalog-service/src/main/resources/application.properties
server.port=8082
spring.data.mongodb.uri=mongodb://localhost:27017/anysale

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=catalog-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.*

lead-service.base-url=http://localhost:8080
management.endpoints.web.exposure.include=health,info

anysale-notification-service/src/main/resources/application.properties
server.port=8081
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.*
management.endpoints.web.exposure.include=health,info

anysale-ingestion-gateway/src/main/resources/application.properties
server.port=8083
lead-service.base-url=http://localhost:8080

# se usar Resilience4j (@Retry/@CircuitBreaker) no gateway:
resilience4j.retry.instances.leadClient.max-attempts=3
resilience4j.retry.instances.leadClient.wait-duration=300ms
resilience4j.circuitbreaker.instances.leadClient.sliding-window-size=10
resilience4j.circuitbreaker.instances.leadClient.failure-rate-threshold=50

management.endpoints.web.exposure.include=health,info


Override por variável de ambiente: qualquer propriedade pode ser sobrescrita (ex.: SPRING_DATASOURCE_URL).

Build & Run
# 0) subir infra
docker compose -f infra/docker-compose.yml up -d

# 1) build (raiz do repo)
mvn -U -T 1C clean package

# 2) subir serviços (cada um em um terminal/Run do IntelliJ)
mvn -f anysale-lead-service spring-boot:run
mvn -f anysale-catalog-service spring-boot:run
mvn -f anysale-notification-service spring-boot:run
mvn -f anysale-ingestion-gateway spring-boot:run

# health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

Fluxo de eventos (Kafka)

Gateway recebe lead bruto e chama Lead Service (POST /v1/leads).

Lead Service cria o lead e publica lead.created.

Catalog Service consome lead.created, escolhe top3 produtos e chama PATCH /v1/leads/{id}/suggestions.

Lead Service anexa sugestões e publica lead.updated.

Notification Service consome lead.updated e registra/expõe o histórico.

Tópicos:

lead.created (produzido pelo lead-service; consumido pelo catalog-service)

lead.updated (produzido pelo lead-service; consumido pelo notification-service)

Dica: crie os tópicos via @Bean NewTopic (já incluso no lead-service) para não depender de auto-create em produção.

API (endpoints principais)
Lead Service (8080)

POST /v1/leads
Cria um lead e publica lead.created.
Body:

{
"name": "João",
"email": "joao@ex.com",
"phone": "+55...",
"source": "MarketplaceX",
"desiredCategory": "home-office",
"desiredTags": ["cadeira", "ergonômica"]
}


200 OK (JSON do lead)

PATCH /v1/leads/{id}/stage
Muda estágio; publica lead.updated.
Body: {"stage":"WON"}

PATCH /v1/leads/{id}/suggestions
Anexa sugestões; publica lead.updated.
Body:

{
"suggestions": [
{"productId":"p1","title":"Cadeira Ergo A","price":520,"currency":"BRL","vendor":"LojaX"},
{"productId":"p2","title":"Mesa B","price":399,"currency":"BRL","vendor":"LojaX"}
]
}


GET /v1/leads/{id}
Retorna o lead.

Catalog Service (8082)

POST /v1/products (seed/bulk)

GET /v1/products

Notification Service (8081)

GET /v1/notifications/{leadId}
Lista histórico de notificações (em memória).

Ingestion Gateway (8083)

POST /v1/ingest/lead
Recebe lead bruto (campos como full_name, desired_category, etc.) e repassa ao Lead Service.

Modelo de dados (lead + tags)

Tabela lead: dados principais (id, nome, email, stage, etc.).

Tabela lead_desired_tag: uma linha por tag do lead (lead_id, tag) via @ElementCollection.

Tabela lead_suggestion: sugestões anexadas.

Flyway V1 já cria tudo; arquivo:
anysale-lead-service/src/main/resources/db/migration/V1__init.sql

Testes (opcional)

Testcontainers para Postgres/Mongo/Kafka em integração.

Para começar rápido, você pode:

Desativar o teste gerado do Initializr ou

Usar mvn -DskipTests clean package durante o bootstrap.

Se quiser, adiciono um exemplo de teste de contexto com Postgres via Testcontainers.

Troubleshooting

“Dependency requires at least JVM 17 / This build uses a Java 11 JVM”
Troque o JAVA_HOME, IntelliJ Importer/Runner e mvn -v para Java 17.

Parent não encontra módulo / “Cannot resolve symbol <module>”
Garanta <modules> corretos, pastas sob o parent, packaging=pom no parent e jar nos filhos. Recarregue o Maven na IDE.

Falha ao subir DB/Kafka
Verifique docker compose -f infra/docker-compose.yml up -d, portas livres, e URLs em application.properties.

Kafka bootstrap
Logs do container (docker logs -f kafka-anysale). Para host/IDE, KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092.

Erro JPA: 'Basic' attribute type should not be 'String[]'
Resolvido usando @ElementCollection List<String> em vez de String[].

Roadmap

Outbox/CDC para publicação idempotente

Autenticação/autorização (API Key no gateway)

Observabilidade (Prometheus/Grafana, OpenTelemetry)

Notificadores reais (e-mail/SMS) + DLQ/retentativas

Estratégias de ranking de catálogo mais sofisticadas