# n8n Contract

This document is the authoritative contract for the current WhatsApp + n8n MVP flow.

## Recommended Flow

1. n8n receives the WhatsApp webhook from Meta.
2. n8n normalizes the provider payload and calls the gateway endpoint:
   `POST http://localhost:8083/v1/messages/incoming`
3. The gateway forwards the message to the lead service.
4. The lead service creates or updates the lead, persists the interaction, and updates:
   `lastMessage`, `lastInteractionAt`, and `stage`.
5. n8n runs AI classification and enrichment.
6. n8n sends the AI result back to:
   `PATCH http://localhost:8080/v1/leads/{leadId}/enrichment`
7. n8n can fetch the latest lead state and the interaction history with:
   `GET http://localhost:8080/v1/leads/{leadId}`
   `GET http://localhost:8080/v1/leads/{leadId}/interactions`

## Endpoint Summary

### 1. Receive Incoming Message

Recommended external endpoint for n8n:

`POST /v1/messages/incoming`

Service:
`anysale-ingestion-gateway`

Request body:

```json
{
  "phone": "+55 (41) 99999-9999",
  "leadName": "Guilherme Maschio",
  "message": "Quero saber mais sobre cadeira ergonomica",
  "channel": "WHATSAPP",
  "externalMessageId": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4RkU5QzQ5QzQ1RTk3AA=="
}
```

Field rules:
- `phone`: required, used as the current lead identity for the WhatsApp MVP.
- `leadName`: optional. If blank, the lead service falls back to `Contato {normalizedPhone}`.
- `message`: required.
- `channel`: required. Recommended values for this MVP: `WHATSAPP`, `INSTAGRAM`.
- `externalMessageId`: optional, but strongly recommended for idempotency and deduplication.

Response body:

```json
{
  "status": "RECEIVED",
  "normalizedPhone": "5541999999999"
}
```

### 2. Internal Inbound Endpoint

Internal endpoint used by the gateway:

`POST /v1/leads/incoming-message`

Service:
`anysale-lead-service`

Request body:

```json
{
  "phone": "5541999999999",
  "leadName": "Guilherme Maschio",
  "message": "Quero saber mais sobre cadeira ergonomica",
  "channel": "WHATSAPP",
  "externalMessageId": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4RkU5QzQ5QzQ1RTk3AA=="
}
```

Response:
- `200 OK`
- empty body

### 3. Enrich Lead With AI Result

`PATCH /v1/leads/{leadId}/enrichment`

Service:
`anysale-lead-service`

Request body:

```json
{
  "summary": "Lead com alta intencao de compra buscando cadeira ergonomica para home office.",
  "intent": "BUYING",
  "desiredCategory": "home-office",
  "desiredTags": [
    "cadeira",
    "ergonomica",
    "home-office"
  ],
  "score": 92,
  "nextAction": "Enviar catalogo de cadeiras e oferecer atendimento humano."
}
```

Field rules:
- All fields are optional for partial updates.
- String fields sent as blank are normalized to `null`.
- `desiredTags` is replaced when present.
- `score` is an integer in the current MVP.

Response body:

```json
{
  "id": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
  "name": "Contato 5541999999999",
  "email": null,
  "phone": "5541999999999",
  "source": "WHATSAPP",
  "desiredCategory": "home-office",
  "desiredTags": [
    "cadeira",
    "ergonomica",
    "home-office"
  ],
  "stage": "CONTACTED",
  "lastMessage": "Quero saber mais sobre cadeira ergonomica",
  "lastInteractionAt": "2026-04-18T13:15:30Z",
  "summary": "Lead com alta intencao de compra buscando cadeira ergonomica para home office.",
  "intent": "BUYING",
  "score": 92,
  "nextAction": "Enviar catalogo de cadeiras e oferecer atendimento humano."
}
```

### 4. Get Lead

`GET /v1/leads/{leadId}`

Service:
`anysale-lead-service`

Example response:

```json
{
  "id": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
  "name": "Contato 5541999999999",
  "email": null,
  "phone": "5541999999999",
  "source": "WHATSAPP",
  "desiredCategory": "home-office",
  "desiredTags": [
    "cadeira",
    "ergonomica",
    "home-office"
  ],
  "stage": "CONTACTED",
  "lastMessage": "Quero saber mais sobre cadeira ergonomica",
  "lastInteractionAt": "2026-04-18T13:15:30Z",
  "summary": "Lead com alta intencao de compra buscando cadeira ergonomica para home office.",
  "intent": "BUYING",
  "score": 92,
  "nextAction": "Enviar catalogo de cadeiras e oferecer atendimento humano."
}
```

### 5. Get Interaction History

`GET /v1/leads/{leadId}/interactions`

Service:
`anysale-lead-service`

Example response:

```json
[
  {
    "id": "85b65288-3c45-4af1-a31d-471028d7cc53",
    "message": "Ola, quero saber mais sobre cadeira ergonomica",
    "channel": "WHATSAPP",
    "direction": "IN",
    "externalMessageId": "wamid.001",
    "createdAt": "2026-04-18T13:15:30Z"
  },
  {
    "id": "0f8d6b88-0d4d-4f08-a778-e31f92aa81be",
    "message": "Pode me mandar o catalogo?",
    "channel": "WHATSAPP",
    "direction": "IN",
    "externalMessageId": "wamid.002",
    "createdAt": "2026-04-18T13:17:10Z"
  }
]
```

## Current Limitations

- The WhatsApp MVP still identifies leads by `phone`.
- Instagram support will require a channel identity such as `externalContactId`.
- The inbound gateway response does not return `leadId` yet.
- If n8n needs the lead identifier immediately after inbound, it currently needs an orchestration strategy on top of the existing APIs.

## Postman

The local Postman collection was updated here:

`postman/collections/AnySale (Local).postman_collection.json`
