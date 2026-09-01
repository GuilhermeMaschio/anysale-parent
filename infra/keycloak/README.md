# Keycloak local

Start the local identity provider with the existing development stack:

```powershell
docker compose -f infra/docker-compose.yml up -d keycloak
```

Open `http://localhost:8180/admin/` and sign in with the development-only
bootstrap credentials `admin` / `admin`, unless you overrode
`KEYCLOAK_ADMIN_USERNAME` and `KEYCLOAK_ADMIN_PASSWORD` in your shell.

The first start imports `anysale-realm`, the public SPA client
`anysale-console` (authorization code + PKCE), and the roles `SALES_AGENT`,
`SALES_MANAGER`, and `ADMIN`. Create operator users in the Keycloak admin
console and assign exactly the roles they need.

For the local lead-service, use:

```env
ANYSALE_SECURITY_ENABLED=true
KEYCLOAK_ISSUER=http://localhost:8180/realms/anysale-realm
ANYSALE_SECURITY_KEYCLOAK_CLIENT_ID=anysale-console
```

Start the lead-service with the Keycloak profile:

```powershell
mvn -f .\anysale-lead-service\pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev-keycloak"
```

Copy `anysale-console/.env.example` to `anysale-console/.env.local`, then run
the Console with `npm run dev` from `anysale-console`. The Console redirects to
Keycloak only after the operator selects **Entrar com AnySale** and sends the
access token in the `Authorization: Bearer` header for every API call.

The bootstrap password is only a local default. Set unique secrets outside the
repository for staging and production; do not use `start-dev` there.

## Painel de usuários do AnySale

O Console nunca recebe uma credencial administrativa do Keycloak. Para liberar
a tela **Usuários** para administradores do AnySale, crie no realm
`anysale-realm` um client confidencial de serviço, por exemplo
`anysale-user-admin`:

1. Desative **Standard flow** e **Direct access grants**; ative **Service accounts roles**.
2. Copie o segredo gerado na aba **Credentials** para o ambiente do Lead Service.
3. Em **Service account roles**, no client `realm-management`, atribua
   `query-users`, `view-users` e `manage-users`.

Configure somente no processo do Lead Service:

```env
ANYSALE_KEYCLOAK_ADMIN_ENABLED=true
ANYSALE_KEYCLOAK_ADMIN_CLIENT_ID=anysale-user-admin
ANYSALE_KEYCLOAK_ADMIN_CLIENT_SECRET=<segredo-do-client>
```

O endpoint `/v1/admin/users` exige a role de realm `ADMIN`, além da credencial
de serviço acima. Não use variáveis `VITE_*` para essas configurações.
