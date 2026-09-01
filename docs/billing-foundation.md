# Billing service foundation

`anysale-billing-service` owns subscription state and payment-provider events. It uses `tenant_id`
only as an integration key, without a foreign key to Lead Service data. This lets billing move to a
separate database without changing the commercial domain.

The service exposes an ADMIN-only subscription status endpoint and a public Asaas webhook endpoint.
The webhook is protected with a dedicated `asaas-access-token`, records provider event ids uniquely,
and applies a payment event only to an existing subscription mapping.

Set the Asaas API key and webhook token only in the billing service environment. The next increment
will add the plan catalog and checkout that creates this mapping before any webhook can activate access.
