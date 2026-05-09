# n8n Contract

This document is the authoritative contract for the current WhatsApp + n8n MVP flow.

## Recommended Flow

1. Meta sends WhatsApp Cloud API webhooks directly to the gateway endpoint with the `messages` and `statuses` subscribed fields:
   `GET/POST http://localhost:8083/v1/whatsapp/webhook`
2. The gateway validates the webhook challenge/signature, extracts text messages, normalizes them, and forwards outbound status updates to the lead service.
3. The lead service creates or updates the lead, persists inbound and outbound interactions, and keeps delivery state attached to the same interaction via `externalMessageId`.
4. The lead service updates:
   `lastMessage`, `lastInteractionAt`, and `stage`.
5. n8n can still be used for local testing or later downstream automations through the normalized gateway endpoint:
   `POST http://localhost:8083/v1/messages/incoming`
6. n8n sends the AI result back to:
   `PATCH http://localhost:8080/v1/leads/{leadId}/enrichment`
7. n8n can fetch the latest lead state and the interaction history with:
   `GET http://localhost:8080/v1/leads/{leadId}`
   `GET http://localhost:8080/v1/leads/{leadId}/interactions`
8. AnySale can send a manual WhatsApp response through:
   `POST http://localhost:8081/v1/notifications/whatsapp/messages`

## Endpoint Summary

### 1. Direct WhatsApp Cloud API Webhook

Recommended external endpoint for Meta:

`GET /v1/whatsapp/webhook`
`POST /v1/whatsapp/webhook`

Service:
`anysale-ingestion-gateway`

Required runtime settings:
- `WHATSAPP_WEBHOOK_VERIFY_TOKEN`: the verify token configured in the Meta app.
- `WHATSAPP_APP_SECRET`: the Meta app secret used to validate `X-Hub-Signature-256`.

For local development, if `WHATSAPP_APP_SECRET` is blank, the gateway accepts unsigned POSTs. Production should always set the app secret.

Verification request from Meta:

```http
GET /v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token={token}&hub.challenge={challenge}
```

Expected verification response:
- `200 OK` with the raw challenge as the response body when the token matches.
- `403 Forbidden` when the token does not match.

Message webhook request from Meta:

```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "123456789",
      "changes": [
        {
          "field": "messages",
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "55 41 99999-9999",
              "phone_number_id": "987654321"
            },
            "contacts": [
              {
                "profile": {
                  "name": "Guilherme Maschio"
                },
                "wa_id": "5541999999999"
              }
            ],
            "messages": [
              {
                "from": "5541999999999",
                "id": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA",
                "timestamp": "1713575550",
                "text": {
                  "body": "Quero saber mais sobre cadeira ergonomica"
                },
                "type": "text"
              }
            ]
          }
        }
      ]
    }
  ]
}
```

Current behavior:
- Text messages are mapped to the internal incoming message contract.
- Status webhook payloads are mapped by `messageId` and forwarded to the lead service.
- Delivery events are persisted on the matching interaction as `deliveryStatus`, `deliveryStatusAt`, and optional failure details.
- Unsupported message types are ignored for now.
- Invalid signatures return `403 Forbidden`.
- Invalid JSON returns `400 Bad Request`.

### 2. Normalized Incoming Message

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
  "normalizedPhone": "5541999999999",
  "leadId": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
  "lead": {
    "id": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
    "name": "Contato 5541999999999",
    "email": null,
    "phone": "5541999999999",
    "source": "WHATSAPP",
    "desiredCategory": null,
    "desiredTags": [],
    "stage": "CONTACTED",
    "lastMessage": "Quero saber mais sobre cadeira ergonomica",
    "lastInteractionAt": "2026-04-20T01:12:30Z",
    "summary": null,
    "intent": null,
    "score": null,
    "nextAction": null
  }
}
```

### 3. Internal Inbound Endpoint

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

Response body:

```json
{
  "id": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
  "name": "Contato 5541999999999",
  "email": null,
  "phone": "5541999999999",
  "source": "WHATSAPP",
  "desiredCategory": null,
  "desiredTags": [],
  "stage": "CONTACTED",
  "lastMessage": "Quero saber mais sobre cadeira ergonomica",
  "lastInteractionAt": "2026-04-20T01:12:30Z",
  "summary": null,
  "intent": null,
  "score": null,
  "nextAction": null
}
```

### 4. Send WhatsApp Text Message

Manual outbound endpoint:

`POST /v1/notifications/whatsapp/messages`

Service:
`anysale-notification-service`

Required runtime settings:
- `WHATSAPP_ACCESS_TOKEN`: Meta access token with `whatsapp_business_messaging`.
- `WHATSAPP_PHONE_NUMBER_ID`: WhatsApp business phone number ID.
- `WHATSAPP_GRAPH_API_VERSION`: optional Graph API version. Defaults to `v20.0`.
- `WHATSAPP_GRAPH_API_BASE_URL`: optional Graph API base URL. Defaults to `https://graph.facebook.com`.
- `LEAD_SERVICE_BASE_URL`: optional lead service base URL. Defaults to `http://localhost:8080`.

Request body:

```json
{
  "leadId": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
  "to": "5541999999999",
  "message": "Oi, posso te ajudar com a cadeira ergonomica."
}
```

Response body:

```json
{
  "leadId": "3c04b6f5-91e2-4524-bf0f-1f2ce58d0d3b",
  "to": "5541999999999",
  "waId": "5541999999999",
  "messageId": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA",
  "status": "SENT"
}
```

Current behavior:
- Sends a WhatsApp text message with `preview_url=false`.
- Uses the Meta endpoint `POST /{version}/{phone-number-id}/messages`.
- If `leadId` is present, persists an `OUT` interaction in the lead service.
- The returned `messageId` becomes the correlation key used later by Meta status webhooks.

### 5. Record Outbound Interaction

Internal endpoint used by the notification service after a successful WhatsApp send:

`POST /v1/leads/{leadId}/interactions/outbound`

Service:
`anysale-lead-service`

Request body:

```json
{
  "message": "Oi, posso te ajudar com a cadeira ergonomica.",
  "channel": "WHATSAPP",
  "externalMessageId": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA"
}
```

Response body:

```json
{
  "id": "b0906265-b6f8-4e85-9c94-8743ef73c0a1",
  "message": "Oi, posso te ajudar com a cadeira ergonomica.",
  "channel": "WHATSAPP",
  "direction": "OUT",
  "externalMessageId": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA",
  "createdAt": "2026-04-21T16:00:00Z"
}
```

Current behavior:
- The lead service updates `lastMessage` and `lastInteractionAt` with the outbound message.
- `externalMessageId` is used as an idempotency key per channel.
- Repeated calls with the same `channel` and `externalMessageId` return the existing interaction.

### 6. Sync WhatsApp Delivery Status

Internal endpoint used by the gateway after receiving a Meta status webhook:

`POST /v1/leads/interactions/status`

Service:
`anysale-lead-service`

Request body:

```json
{
  "channel": "WHATSAPP",
  "externalMessageId": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA",
  "status": "delivered",
  "statusTimestamp": "2026-05-09T15:05:00Z",
  "recipientId": "5541999999999",
  "errorCode": null,
  "errorTitle": null,
  "errorMessage": null
}
```

Current behavior:
- Matches the existing interaction by `channel + externalMessageId`.
- Ignores unknown message IDs so the webhook can still return `200 OK` to Meta.
- Ignores stale status updates when the incoming timestamp is older than the current saved delivery timestamp.
- Persists `deliveryStatus`, `deliveryStatusAt`, `deliveryRecipientId`, and failure details when present.

### 7. Enrich Lead With AI Result

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

### 8. Get Lead

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

### 9. Get Interaction History

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
    "deliveryStatus": null,
    "deliveryStatusAt": null,
    "deliveryRecipientId": null,
    "deliveryErrorCode": null,
    "deliveryErrorTitle": null,
    "deliveryErrorMessage": null,
    "createdAt": "2026-04-18T13:15:30Z"
  },
  {
    "id": "0f8d6b88-0d4d-4f08-a778-e31f92aa81be",
    "message": "Oi, posso te ajudar com a cadeira ergonomica.",
    "channel": "WHATSAPP",
    "direction": "OUT",
    "externalMessageId": "wamid.002",
    "deliveryStatus": "READ",
    "deliveryStatusAt": "2026-05-09T15:05:00Z",
    "deliveryRecipientId": "5541999999999",
    "deliveryErrorCode": null,
    "deliveryErrorTitle": null,
    "deliveryErrorMessage": null,
    "createdAt": "2026-04-18T13:17:10Z"
  }
]
```

## Current Limitations

- The WhatsApp MVP still identifies leads by `phone`.
- Instagram support will require a channel identity such as `externalContactId`.
- The lead snapshot returned by inbound reflects the state right after persistence, before any later asynchronous enrichment or follow-up processing.
- Meta will only send status updates back if the app subscription includes the `statuses` field and the webhook URL is publicly reachable over HTTPS in the target environment.

## Postman

The local Postman collection was updated here:

`postman/collections/AnySale (Local).postman_collection.json`
