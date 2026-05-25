# WhatsApp Staging Checklist

This checklist is the minimum package required to move the current WhatsApp CRM flow from local development to a stable staging environment.

Supporting files are available in:

- `infra/staging/README.md`
- `infra/staging/single-host/README.md`
- `infra/staging/lead-service.env.example`
- `infra/staging/ingestion-gateway.env.example`
- `infra/staging/notification-service.env.example`

## 1. Required Services

The current staging flow depends on:

- `anysale-lead-service`
- `anysale-ingestion-gateway`
- `anysale-notification-service`
- PostgreSQL
- Kafka
- A public HTTPS URL for the Meta webhook

## 2. Required Environment Variables

### `anysale-lead-service`

```env
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://<postgres-host>:5432/anysale
SPRING_DATASOURCE_USERNAME=anysale
SPRING_DATASOURCE_PASSWORD=<strong-password>
SPRING_FLYWAY_ENABLED=true
SPRING_KAFKA_BOOTSTRAP_SERVERS=<kafka-host>:9092
SPRING_KAFKA_CONSUMER_GROUP_ID=lead-service
SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET=earliest
```

### `anysale-ingestion-gateway`

```env
SERVER_PORT=8083
LEAD_SERVICE_BASE_URL=https://<lead-service-host>
WHATSAPP_WEBHOOK_VERIFY_TOKEN=<meta-verify-token>
WHATSAPP_APP_SECRET=<meta-app-secret>
LEAD_CLIENT_RETRY_MAX_ATTEMPTS=3
LEAD_CLIENT_RETRY_WAIT_DURATION=300ms
LEAD_CLIENT_CB_SLIDING_WINDOW_SIZE=10
LEAD_CLIENT_CB_FAILURE_RATE_THRESHOLD=50
```

### `anysale-notification-service`

```env
SERVER_PORT=8081
LEAD_SERVICE_BASE_URL=https://<lead-service-host>
SPRING_KAFKA_BOOTSTRAP_SERVERS=<kafka-host>:9092
SPRING_KAFKA_CONSUMER_GROUP_ID=notification-service
SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET=earliest
WHATSAPP_GRAPH_API_BASE_URL=https://graph.facebook.com
WHATSAPP_GRAPH_API_VERSION=v20.0
WHATSAPP_PHONE_NUMBER_ID=<meta-phone-number-id>
WHATSAPP_ACCESS_TOKEN=<rotated-access-token>
```

## 3. Meta Configuration

Before the first staging smoke test:

1. Rotate any token that was previously exposed in chat, logs, or screenshots.
2. Configure the webhook callback URL to the public staging endpoint:
   `https://<gateway-public-host>/v1/whatsapp/webhook`
3. Configure the same verify token used in `WHATSAPP_WEBHOOK_VERIFY_TOKEN`.
4. Subscribe the app to both webhook fields:
   - `messages`
   - `statuses`
5. Confirm that the WABA phone number is no longer using only the Meta `Test Number` if this staging environment is meant to reflect production behavior.

## 4. Deployment Order

Deploy in this order:

1. PostgreSQL
2. Kafka
3. `anysale-lead-service`
4. `anysale-ingestion-gateway`
5. `anysale-notification-service`
6. Meta webhook callback update

## 5. Smoke Test

Run the smoke test on staging in this order:

1. `GET /actuator/health` on services `8080`, `8081`, and `8083`
2. Meta webhook challenge:
   `GET /v1/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=<token>&hub.challenge=challenge-123`
3. Send a WhatsApp inbound text message to the connected number
4. Confirm a lead was created or updated
5. Confirm an `IN` interaction was saved with the inbound `wamid`
6. Send an outbound message through:
   `POST /v1/notifications/whatsapp/messages`
7. Confirm an `OUT` interaction was saved with the outbound `wamid`
8. Wait for Meta `statuses` callbacks
9. Confirm the same outbound interaction was updated with:
   - `deliveryStatus`
   - `deliveryStatusAt`
   - `deliveryRecipientId`
10. Re-send the same inbound payload and confirm no duplicate interaction is created

## 6. Database Validation

Recommended validation queries:

```sql
select id, name, phone, source, stage, last_message, last_interaction_at
from lead
order by updated_at desc
limit 20;

select
    lead_id,
    direction,
    channel,
    external_message_id,
    delivery_status,
    delivery_status_at,
    delivery_recipient_id,
    delivery_error_code,
    delivery_error_title,
    message
from interaction
order by created_at desc
limit 50;
```

## 7. Release Gate

Do not call staging ready until all items below are true:

- Secrets are stored outside the repository
- Meta webhook challenge succeeds
- Inbound messages create or update leads
- Outbound messages create `OUT` interactions
- `delivered` and `read` statuses reach the CRM
- Duplicate webhook retries do not duplicate interactions
- Failures can be traced by `messageId` / `externalMessageId`
