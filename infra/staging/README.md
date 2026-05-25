# Staging Package

This folder contains the minimum runtime package to start the current WhatsApp CRM flow in staging without hardcoding secrets in the repository.

## Files

- `lead-service.env.example`
- `ingestion-gateway.env.example`
- `notification-service.env.example`
- `Import-EnvFile.ps1`
- `single-host/`

## Presets

### Generic examples

Use the files in this folder when your staging topology is still undecided and you want placeholders.

### Single-host examples

Use `infra/staging/single-host/` when all three Java services run on the same host and only the gateway webhook needs to be public over HTTPS.

## 1. Create real env files

Copy each example file and remove the `.example` suffix:

- `infra/staging/lead-service.env`
- `infra/staging/ingestion-gateway.env`
- `infra/staging/notification-service.env`

Do not commit these `.env` files. They are ignored by `.gitignore`.

## 2. Fill the required secrets

At minimum, replace:

- database host and password for `lead-service`
- Kafka bootstrap host for `lead-service` and `notification-service`
- `LEAD_SERVICE_BASE_URL` for `ingestion-gateway` and `notification-service`
- `WHATSAPP_WEBHOOK_VERIFY_TOKEN`
- `WHATSAPP_APP_SECRET`
- `WHATSAPP_PHONE_NUMBER_ID`
- `WHATSAPP_ACCESS_TOKEN`

## 3. Build the services

From the repository root:

```powershell
mvn -pl "anysale-lead-service,anysale-ingestion-gateway,anysale-notification-service" -am package -DskipTests
```

## 4. Load env vars into a PowerShell session

Run one command per service before starting it:

```powershell
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\lead-service.env
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\ingestion-gateway.env
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\notification-service.env
```

If you prefer, load each env file in the terminal where that service will run.

## 5. Start the services

### Lead Service

```powershell
java -jar .\anysale-lead-service\target\anysale-lead-service-0.0.1-SNAPSHOT.jar
```

### Ingestion Gateway

```powershell
java -jar .\anysale-ingestion-gateway\target\anysale-ingestion-gateway-1.0.0-SNAPSHOT.jar
```

### Notification Service

```powershell
java -jar .\anysale-notification-service\target\anysale-notification-service-1.0.0-SNAPSHOT.jar
```

## 6. Validate staging

Use this checklist after boot:

- [staging-whatsapp-checklist.md](../../docs/staging-whatsapp-checklist.md)

The release gate is only complete after inbound, outbound, and Meta `statuses` are all confirmed in the CRM timeline.
