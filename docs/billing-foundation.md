# Billing service foundation

`anysale-billing-service` owns subscription state and payment-provider events. It uses `tenant_id`
only as an integration key, without a foreign key to Lead Service data. This lets billing move to a
separate database without changing the commercial domain.

The service exposes an ADMIN-only subscription status endpoint and a public Asaas webhook endpoint.
The webhook is protected with a dedicated `asaas-access-token`, records provider event ids uniquely,
and applies a payment event only to an existing subscription mapping.

Set the Asaas API key and webhook token only in the billing service environment. The next increment
will add the plan catalog and checkout that creates this mapping before any webhook can activate access.

## Initial launch plans

| Plan | Monthly price | Included limits |
| --- | ---: | --- |
| Essencial | R$ 149 | 3 users, 1,000 leads/month, 500 AI requests/month |
| Profissional | R$ 349 | 10 users, 5,000 leads/month, 3,000 AI requests/month |
| Enterprise | Custom | Limits defined in the contract |

All initial plans have 14 trial days and a 5-day payment grace period. The next increment uses this
catalog to create the provider subscription and later enforces its limits at the appropriate services.

## Hosted recurring checkout

`POST /v1/billing/checkout` is available only to tenant ADMINs. It accepts a plan code and returns
the hosted Asaas checkout URL. The payment page receives card data directly; AnySale never handles
card numbers. The checkout is created as `RECURRENT`, monthly, with the first charge due after the
plan trial period. Asaas callbacks only control navigation: `CHECKOUT_PAID` webhook is the event that
activates the tenant subscription.

## Sandbox local sem VPS

O Billing Service usa a porta `8084` localmente, deixando `8083` reservada ao Ingestion Gateway.
Para testar o Asaas antes de existir um domínio público, use os scripts em `infra/local`:

```powershell
.\infra\local\Start-BillingSandboxTunnels.ps1
```

O comando abre túneis HTTPS temporários para o Console (`5173`) e para o Billing Service (`8084`) e
mostra as duas URLs. Em seguida, configure as variáveis no terminal que iniciará o Billing Service:

```powershell
.\infra\local\Set-BillingSandboxEnvironment.ps1 -ConsolePublicUrl https://<console> -BillingPublicUrl https://<billing>
```

O script solicita a chave Sandbox sem imprimi-la, gera um token de webhook se necessário e não grava
segredos no repositório. Cadastre no Asaas o webhook `https://<billing>/v1/billing/webhooks/asaas` com
o token mostrado pelo script. Para acessar o Console pelo túnel, inclua sua URL temporária nos
**Valid redirect URIs** e **Web origins** do client `anysale-console` no Keycloak.
