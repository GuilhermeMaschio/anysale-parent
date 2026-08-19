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
