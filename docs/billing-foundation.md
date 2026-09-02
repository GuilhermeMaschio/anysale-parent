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
Para o ambiente local do AnySale, prefira o túnel nomeado `anysale-sandbox`. Ele mantém os endereços
estáveis `https://sandbox-console.anysale.com.br` e `https://sandbox-billing.anysale.com.br`,
respectivamente encaminhados para o Console (`5173`) e o Billing Service (`8084`). O Console inicia
esse túnel automaticamente com `npm run dev` quando ele já estiver configurado no Cloudflare.

No mesmo terminal em que o Billing Service será iniciado, configure as variáveis:

```powershell
.\infra\local\Set-BillingSandboxEnvironment.ps1 `
  -ConsolePublicUrl https://sandbox-console.anysale.com.br `
  -BillingPublicUrl https://sandbox-billing.anysale.com.br

mvn -f anysale-billing-service\pom.xml spring-boot:run
```

O script solicita a chave Sandbox sem imprimi-la, gera um token de webhook se necessário e não grava
segredos no repositório. Cadastre no Asaas o webhook
`https://sandbox-billing.anysale.com.br/v1/billing/webhooks/asaas` com o token mostrado pelo script.
O client `anysale-console` também deve autorizar `https://sandbox-console.anysale.com.br/*` nos
**Valid redirect URIs** e `https://sandbox-console.anysale.com.br` em **Web origins** do Keycloak.

O script `Start-BillingSandboxTunnels.ps1` continua disponível apenas como alternativa de URL
temporária quando não houver domínio gerenciado pelo Cloudflare.

### Eventos do webhook

Para manter a assinatura e o checkout sincronizados, selecione no Asaas os eventos `CHECKOUT_PAID`,
`CHECKOUT_CANCELED`, `CHECKOUT_EXPIRED`, `SUBSCRIPTION_CREATED`, `PAYMENT_RECEIVED`,
`PAYMENT_CONFIRMED`, `PAYMENT_OVERDUE`, `PAYMENT_DUNNING_REQUESTED`,
`SUBSCRIPTION_INACTIVATED` e `SUBSCRIPTION_DELETED`.
