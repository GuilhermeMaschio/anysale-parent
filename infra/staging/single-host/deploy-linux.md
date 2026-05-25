# Linux Deployment Runbook

This runbook assumes a single Linux host using:

- `systemd`
- `nginx`
- local PostgreSQL access
- local Kafka access
- Java 17+

## 1. Suggested directories

```text
/opt/anysale/
  env/
    lead-service.env
    ingestion-gateway.env
    notification-service.env
  lead-service/
    anysale-lead-service-0.0.1-SNAPSHOT.jar
  ingestion-gateway/
    anysale-ingestion-gateway-1.0.0-SNAPSHOT.jar
  notification-service/
    anysale-notification-service-1.0.0-SNAPSHOT.jar
```

## 2. Build artifacts locally

From the repository root:

```bash
mvn -pl "anysale-lead-service,anysale-ingestion-gateway,anysale-notification-service" -am package -DskipTests
```

Artifacts produced:

- `anysale-lead-service/target/anysale-lead-service-0.0.1-SNAPSHOT.jar`
- `anysale-ingestion-gateway/target/anysale-ingestion-gateway-1.0.0-SNAPSHOT.jar`
- `anysale-notification-service/target/anysale-notification-service-1.0.0-SNAPSHOT.jar`

## 3. Copy files to the server

Copy:

- the 3 jar files
- the 3 local `.env` files
- `infra/staging/single-host/nginx-gateway.conf`
- the `systemd/*.service.example` files

Recommended destination:

- jars -> `/opt/anysale/<service>/`
- env files -> `/opt/anysale/env/`
- systemd files -> `/etc/systemd/system/`
- nginx file -> `/etc/nginx/sites-available/anysale-gateway.conf`

## 4. Prepare the service user

Example:

```bash
sudo useradd --system --home /opt/anysale --shell /usr/sbin/nologin anysale
sudo mkdir -p /opt/anysale/env /opt/anysale/lead-service /opt/anysale/ingestion-gateway /opt/anysale/notification-service
sudo chown -R anysale:anysale /opt/anysale
sudo chmod 600 /opt/anysale/env/*.env
```

## 5. Install systemd units

Rename the examples:

- `lead-service.service.example` -> `lead-service.service`
- `ingestion-gateway.service.example` -> `ingestion-gateway.service`
- `notification-service.service.example` -> `notification-service.service`

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable lead-service.service
sudo systemctl enable ingestion-gateway.service
sudo systemctl enable notification-service.service
sudo systemctl start lead-service.service
sudo systemctl start ingestion-gateway.service
sudo systemctl start notification-service.service
```

## 6. Check service health

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8083/actuator/health
curl http://127.0.0.1:8081/actuator/health
```

Inspect logs if needed:

```bash
sudo journalctl -u lead-service.service -f
sudo journalctl -u ingestion-gateway.service -f
sudo journalctl -u notification-service.service -f
```

## 7. Install nginx config

Example:

```bash
sudo cp /path/to/nginx-gateway.conf /etc/nginx/sites-available/anysale-gateway.conf
sudo ln -s /etc/nginx/sites-available/anysale-gateway.conf /etc/nginx/sites-enabled/anysale-gateway.conf
sudo nginx -t
sudo systemctl reload nginx
```

## 8. TLS certificate

The provided nginx file expects Let's Encrypt paths.

Example with Certbot:

```bash
sudo certbot --nginx -d gateway-staging.example.com
```

Replace the domain with the real staging domain before running this.

## 9. Meta webhook update

After nginx is live:

1. Set callback URL to `https://<your-domain>/v1/whatsapp/webhook`
2. Set the same verify token stored in `ingestion-gateway.env`
3. Subscribe:
   - `messages`
   - `statuses`

## 10. Smoke test

Run the release checklist:

- `docs/staging-whatsapp-checklist.md`
