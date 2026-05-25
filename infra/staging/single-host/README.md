# Single-Host Staging Preset

This preset assumes:

- `lead-service`, `ingestion-gateway`, and `notification-service` run on the same host
- PostgreSQL is available on `127.0.0.1:5435`
- Kafka is available on `127.0.0.1:9092`
- only the gateway webhook is exposed publicly
- Meta calls the public HTTPS URL that terminates in Nginx and forwards to `127.0.0.1:8083`

## Files

- `lead-service.env.example`
- `ingestion-gateway.env.example`
- `notification-service.env.example`
- `nginx-gateway.conf.example`

## Recommended public URL

Point Meta to:

`https://gateway-staging.example.com/v1/whatsapp/webhook`

Replace `gateway-staging.example.com` with the real DNS name of your staging host.

## Boot sequence

1. Copy the three `.env.example` files to `.env`
2. Fill in secrets:
   - database password
   - `WHATSAPP_WEBHOOK_VERIFY_TOKEN`
   - `WHATSAPP_APP_SECRET`
   - `WHATSAPP_PHONE_NUMBER_ID`
   - `WHATSAPP_ACCESS_TOKEN`
3. Build:

```powershell
mvn -pl "anysale-lead-service,anysale-ingestion-gateway,anysale-notification-service" -am package -DskipTests
```

4. Load env vars in one terminal per service:

```powershell
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\single-host\lead-service.env
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\single-host\ingestion-gateway.env
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\single-host\notification-service.env
```

5. Start the jars:

```powershell
java -jar .\anysale-lead-service\target\anysale-lead-service-0.0.1-SNAPSHOT.jar
java -jar .\anysale-ingestion-gateway\target\anysale-ingestion-gateway-1.0.0-SNAPSHOT.jar
java -jar .\anysale-notification-service\target\anysale-notification-service-1.0.0-SNAPSHOT.jar
```

6. Apply the reverse proxy based on `nginx-gateway.conf.example`
7. Update the Meta webhook callback URL and subscribe:
   - `messages`
   - `statuses`

## Validation

After boot, use:

- `../../docs/staging-whatsapp-checklist.md`

This preset is intentionally simple so the team can validate staging before investing in containers, orchestration, or service discovery.
