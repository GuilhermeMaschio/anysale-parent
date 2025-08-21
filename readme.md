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

Configuração por serviço

Dica: se rodar app em container, troque localhost pelos nomes de serviço do compose
(pg-anysale, mongo-anysale, kafka-anysale).

Lead Service (8080)

anysale-lead-service/src/main/resources/application.properties

server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/anysale
spring.datasource.username=anysale
spring.datasource.password=secret
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

spring.kafka.bootstrap-servers=localhost:9092
# Producer JSON
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.properties.spring.json.add.type.headers=true

# (se o serviço consumir algo)
spring.kafka.consumer.group-id=lead-service
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.contracts.events

management.endpoints.web.exposure.include=health,info,metrics

Catalog Service (8082)

anysale-catalog-service/src/main/resources/application.properties

server.port=8082
spring.data.mongodb.uri=mongodb://localhost:27017/anysale

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=catalog-service
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.contracts.events

lead-service.base-url=http://localhost:8080

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

Notification Service (8081)

anysale-notification-service/src/main/resources/application.properties

server.port=8081
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.contracts.events

management.endpoints.web.exposure.include=health,info

Ingestion Gateway (8083)

anysale-ingestion-gateway/src/main/resources/application.properties

server.port=8083
lead-service.base-url=http://localhost:8080
management.endpoints.web.exposure.include=health,info

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
Não edite migrations aplicadas. Crie uma nova V2__. Para ambiente local, flyway repair pode ser usado conscientemente.

Roadmap curto

Outbox para publicação idempotente

Persistir notificações (Postgres)

Autenticação/RBAC + multi-tenant

Observabilidade (Prometheus/Grafana/OTel)

Conectores (Facebook Lead Ads / Shopify / Woo)

Appendix A — Criação de tópicos Kafka via Spring

Crie esta classe no Lead Service para garantir os tópicos no start:

anysale-lead-service/src/main/java/com/anysale/lead/config/KafkaTopicsConfig.java

package com.anysale.lead.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
* Cria tópicos necessários no ambiente de dev.
* Em produção, prefira provisionar via IaC/DevOps.
  */
  @Configuration
  public class KafkaTopicsConfig {

  @Bean
  public NewTopic leadCreated() {
  return TopicBuilder.name("lead.created")
  .partitions(3)
  .replicas(1)
  .build();
  }

  @Bean
  public NewTopic leadUpdated() {
  return TopicBuilder.name("lead.updated")
  .partitions(3)
  .replicas(1)
  .build();
  }
  }


Se auto-create estiver desabilitado no broker, esses beans garantem os tópicos.
Para ambientes prod, use IaC (Terraform/Helm/Confluent).

Pronto! Se quiser, eu também te mando um README.pt-BR.md com esses mesmos conteúdos + um micro badge de status de build do Maven/GitHub Actions.

Você disse:
quero
O ChatGPT disse:
Pensou por 8s

perfeito! abaixo está o README.pt-BR.md prontinho (com TOC e badges).
Substitua seu-usuario/seu-repo no badge do GitHub Actions pelo nome do seu repositório.

# AnySale

Gerenciador de **leads** com **sugestões automáticas** e **notificações**.  
Arquitetura Hexagonal · Spring Boot · Kafka · Postgres (leads) · MongoDB (catálogo).

[![Build](https://github.com/seu-usuario/seu-repo/actions/workflows/ci.yml/badge.svg)](https://github.com/seu-usuario/seu-repo/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## Sumário
- [Visão geral](#visão-geral)
- [Módulos (Maven)](#módulos-maven)
- [Stack & Pré-requisitos](#stack--pré-requisitos)
- [Infra (Docker Compose)](#infra-docker-compose)
- [Configuração por serviço](#configuração-por-serviço)
    - [Lead Service (8080)](#lead-service-8080)
    - [Catalog Service (8082)](#catalog-service-8082)
    - [Notification Service (8081)](#notification-service-8081)
    - [Ingestion Gateway (8083)](#ingestion-gateway-8083)
- [Build & Run](#build--run)
- [Endpoints principais](#endpoints-principais)
- [Eventos (Kafka)](#eventos-kafka)
- [Modelo de dados (Postgres)](#modelo-de-dados-postgres)
- [Troubleshooting](#troubleshooting)
- [Roadmap curto](#roadmap-curto)
- [Apêndice A — Tópicos Kafka via Spring](#apêndice-a--tópicos-kafka-via-spring)

---

## Visão geral

Fluxo: `ingestion-gateway → lead-service → (Kafka) → catalog-service → lead-service → notification-service`  
Eventos: `lead.created` (novo lead) · `lead.updated` (mudanças/sugestões)

Arquitetura por serviço (**hexagonal**):


com.anysale.<service>
├─ domain/ # regras e modelos de domínio
├─ application/ # casos de uso
├─ adapters/
│ ├─ in/ # REST, Messaging (Kafka)
│ └─ out/ # Persistence (JPA/Mongo), HTTP (WebClient), Messaging (Kafka)
└─ config/ # beans infra (tópicos Kafka, etc.)


---

## Módulos (Maven)



anysale-parent/ # parent (packaging=pom)
├─ infra/docker-compose.yml # Postgres, Mongo, Kafka
├─ anysale-shared-kernel/ # VOs/util (sem Spring)
├─ anysale-contracts/ # DTOs de eventos (Kafka) compartilhados
├─ anysale-lead-service/ # JPA/Postgres + produz eventos
├─ anysale-catalog-service/ # Mongo + consome lead.created + sugere
├─ anysale-notification-service/ # consome lead.updated + histórico
└─ anysale-ingestion-gateway/ # WebFlux → proxy/normalizador de leads


---

## Stack & Pré-requisitos

- **Java 17**, **Maven 3.9+**, **Docker** + Docker Compose
- Spring Boot 3.5.x · Web/MVC · WebFlux (gateway) · Data JPA/Mongo · Kafka · Flyway · Actuator
- IntelliJ IDEA (habilite Lombok)

---

## Infra (Docker Compose)

Arquivo: `infra/docker-compose.yml`

Subir:
```bash
docker compose -f infra/docker-compose.yml up -d


Serviços (dev):

Postgres 16 (localhost:5432, db/user/pass anysale / anysale / secret)

Mongo 6 (localhost:27017, sem auth no dev)

Kafka 7.6.x (localhost:9092, auto-create de tópicos habilitado no dev)

Configuração por serviço

Dica: se rodar app em container, troque localhost pelos nomes de serviço do compose
(pg-anysale, mongo-anysale, kafka-anysale).

Lead Service (8080)

anysale-lead-service/src/main/resources/application.properties

server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/anysale
spring.datasource.username=anysale
spring.datasource.password=secret
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

spring.kafka.bootstrap-servers=localhost:9092
# Producer JSON
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.properties.spring.json.add.type.headers=true

# (se o serviço também consumir)
spring.kafka.consumer.group-id=lead-service
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.contracts.events

management.endpoints.web.exposure.include=health,info,metrics

Catalog Service (8082)

anysale-catalog-service/src/main/resources/application.properties

server.port=8082
spring.data.mongodb.uri=mongodb://localhost:27017/anysale

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=catalog-service
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.contracts.events

lead-service.base-url=http://localhost:8080

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

Notification Service (8081)

anysale-notification-service/src/main/resources/application.properties

server.port=8081
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.auto-offset-reset=latest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.anysale.contracts.events

management.endpoints.web.exposure.include=health,info

Ingestion Gateway (8083)

anysale-ingestion-gateway/src/main/resources/application.properties

server.port=8083
lead-service.base-url=http://localhost:8080
management.endpoints.web.exposure.include=health,info

Build & Run
# 0) infraestrutura
docker compose -f infra/docker-compose.yml up -d

# 1) build (raiz)
mvn -U -T 1C clean package

# 2) subir serviços (cada um em um terminal/IDE)
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

lead: id UUID PK, name, email, phone, source,
desired_category, desired_tags TEXT[], stage, created_at, updated_at

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
Não edite migrations aplicadas. Crie uma nova V2__. Em dev, flyway repair pode ser usado conscientemente.

Roadmap curto

Outbox para publicação idempotente

Persistir notificações (Postgres)

Autenticação/RBAC + multi-tenant

Observabilidade (Prometheus/Grafana/OTel)

Conectores (Facebook Lead Ads / Shopify / Woo)