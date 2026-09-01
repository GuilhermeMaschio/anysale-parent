# Billing foundation

Billing belongs to the company (`tenant`), not to an individual user. The Lead Service stores the
subscription state independently from leads, tasks and Keycloak users.

## Asaas configuration

Set the following service environment variables only after creating the Asaas integration:

```text
ANYSALE_BILLING_ASAAS_ENABLED=true
ANYSALE_BILLING_ASAAS_API_KEY=<Asaas API key>
ANYSALE_BILLING_ASAAS_WEBHOOK_TOKEN=<dedicated random token>
```

The API key must never be sent to the Console. Configure the webhook in Asaas to call:

```text
POST https://<anysale-domain>/v1/billing/webhooks/asaas
Header: asaas-access-token: <dedicated random token>
```

Every provider event is persisted with a unique provider event id before its subscription status is
applied. Repeated delivery is therefore safe. A webhook cannot create a tenant or grant access to an
unknown subscription; the checkout flow must persist that relationship first.

The next billing increment adds the plan catalog and checkout that creates the Asaas customer and
subscription. It will use the provider subscription id to connect later payment events to the tenant.
