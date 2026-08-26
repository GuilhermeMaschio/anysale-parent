# Onboarding de desenvolvimento

Este guia sobe o ambiente local completo do AnySale sem gravar credenciais no repositório.

## Repositórios e pré-requisitos

| Parte | Caminho local | Tecnologia |
| --- | --- | --- |
| Backend | `C:\Development\backend\java\workspace_mm\anysale-parent` | Java 17+, Maven 3.9+, Spring Boot |
| Console | `C:\Development\frontend\anysale-console` | Node.js LTS, npm, React e Vite |

Instale também Docker Desktop com Docker Compose v2. Na IntelliJ, habilite o plugin Lombok e importe os módulos Maven.

## 1. Subir a infraestrutura Docker

No diretório do backend:

```powershell
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
```

O compose inicia PostgreSQL (`5435`), MongoDB (`27017`), Kafka (`9092`), n8n (`5678`) e Keycloak (`8180`). Os serviços Spring Boot e o Console são executados pela IDE ou pelo terminal, não dentro desse compose.

Para uma instalação nova e apenas local, o Keycloak usa as credenciais bootstrap documentadas em `infra/keycloak/README.md`. Elas podem ser substituídas pelos valores de ambiente `KEYCLOAK_ADMIN_USERNAME` e `KEYCLOAK_ADMIN_PASSWORD`; use valores próprios fora do desenvolvimento local.

Para parar somente a infraestrutura:

```powershell
docker compose -f infra/docker-compose.yml down
```

Use `down -v` somente se quiser apagar os bancos e o realm local.

## 2. Configurar Keycloak local

Abra `http://localhost:8180`. O compose importa `anysale-realm` na primeira inicialização e disponibiliza o client público `anysale-console`.

O tema de login está em `infra/keycloak/themes/anysale`. Se o realm já existir, selecione manualmente **Realm settings → Themes → Login theme → anysale**. Para detalhes, consulte [infra/keycloak/README.md](../infra/keycloak/README.md).

Para executar APIs protegidas, inicie o Lead Service com o perfil Keycloak e configure os nomes abaixo no ambiente do processo:

```text
ANYSALE_SECURITY_ENABLED=true
KEYCLOAK_ISSUER=http://localhost:8180/realms/anysale-realm
ANYSALE_SECURITY_KEYCLOAK_CLIENT_ID=anysale-console
```

Nunca coloque senha administrativa do Keycloak, tokens Meta ou chave OpenAI em arquivos versionados.

## 3. Variáveis de ambiente

Os modelos sem segredo estão em `infra/staging/*.env.example` e `infra/staging/single-host/*.env.example`.

Para criar arquivos locais de staging, copie os exemplos sem os adicionar ao Git. Para carregar um arquivo em uma sessão PowerShell:

```powershell
. .\infra\staging\Import-EnvFile.ps1 -Path .\infra\staging\single-host\lead-service.env
```

| Serviço | Variáveis principais |
| --- | --- |
| Lead | `SPRING_DATASOURCE_*`, `SPRING_KAFKA_*`, `ANYSALE_INTERNAL_TOKEN`, `NOTIFICATION_SERVICE_BASE_URL`, configurações Keycloak e `OPENAI_*` |
| Gateway | `LEAD_SERVICE_BASE_URL`, `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET` |
| Notification | `LEAD_SERVICE_BASE_URL`, `ANYSALE_INTERNAL_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_ACCESS_TOKEN` |
| Console | `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID`, opcionalmente `VITE_LEAD_SERVICE_URL` |

### Matriz de configuração por módulo

Comece pelos arquivos em `infra/staging/*.env.example`. Eles são modelos: copie cada um para um arquivo local não versionado e preencha somente o que se aplica ao ambiente.

| Módulo | Configuração local recomendada (há defaults para o Docker local) | Necessário para integrar recursos externos |
| --- | --- | --- |
| Lead Service (`8080`) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `NOTIFICATION_SERVICE_BASE_URL`, `ANYSALE_INTERNAL_TOKEN` | `ANYSALE_SECURITY_ENABLED`, `KEYCLOAK_ISSUER`, `ANYSALE_SECURITY_KEYCLOAK_CLIENT_ID`; para IA, `ANYSALE_AI_OPENAI_ENABLED`, `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_ALLOWED_MODELS` e, se necessário, `OPENAI_BASE_URL` |
| Notification Service (`8081`) | `LEAD_SERVICE_BASE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `ANYSALE_INTERNAL_TOKEN` | `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_ACCESS_TOKEN`; opcionalmente `WHATSAPP_GRAPH_API_BASE_URL` e `WHATSAPP_GRAPH_API_VERSION` |
| Ingestion Gateway (`8083`) | `LEAD_SERVICE_BASE_URL`, `ANYSALE_INTERNAL_TOKEN` | `WHATSAPP_WEBHOOK_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET`; opcionalmente os limites `LEAD_CLIENT_RETRY_*` e `LEAD_CLIENT_CB_*` |
| Catalog Service (`8082`) | `SPRING_DATA_MONGODB_URI`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_KAFKA_CONSUMER_GROUP_ID`, `SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET`, `LEAD_SERVICE_BASE_URL` | `KEYCLOAK_ISSUER` ao executar com o perfil `dev-keycloak`; `ANYSALE_CATALOG_ENCRYPTION_SECRET` para criptografia AES-256 de segredos de conectores de catálogo externos |

| Console (`5173`) | `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID` | `VITE_LEAD_SERVICE_URL` em deploy, apontando para a URL pública da API/gateway |

Regras de relacionamento entre variáveis:

- `ANYSALE_INTERNAL_TOKEN` deve ter o **mesmo valor secreto** no Lead, Notification e Gateway. Ele é enviado como `X-Internal-Token` nas chamadas internas; não o exponha no Console.
- `KEYCLOAK_ISSUER` é a URL do realm, por exemplo `http://localhost:8180/realms/anysale-realm` localmente. A configuração é necessária quando o perfil `dev-keycloak` estiver ativo.
- Configure na Meta o mesmo `WHATSAPP_WEBHOOK_VERIFY_TOKEN` usado no Gateway. `WHATSAPP_APP_SECRET`, `WHATSAPP_ACCESS_TOKEN` e `WHATSAPP_PHONE_NUMBER_ID` ficam exclusivamente no backend.
- Qualquer variável iniciada por `VITE_` é embutida no JavaScript do navegador: jamais use esse prefixo para segredos.
- `ANYSALE_LOCAL_TENANT_ID` só é útil no modo local sem segurança; com Keycloak, o tenant vem da claim `tenant_id` do token.

### IA opcional

O Lead Service continua com regras locais quando a IA estiver desligada, sem chave ou acima dos limites. Para disponibilizar o provedor, configure no ambiente do **processo do Lead Service**:

```text
ANYSALE_AI_OPENAI_ENABLED=true
OPENAI_API_KEY=<chave-secreta>
OPENAI_MODEL=<modelo-padrao>
OPENAI_ALLOWED_MODELS=<modelo-1,modelo-2>
OPENAI_BASE_URL=https://api.openai.com/v1
```

`OPENAI_ALLOWED_MODELS` define a lista que o administrador pode escolher no Console. A chave nunca vai para o browser. `mvn spring-boot:run` não lê automaticamente arquivos `.env`; carregue-os antes no terminal ou use a seção **Environment variables** da Run Configuration da IntelliJ.

## 4. Executar os serviços

Em terminais separados, no backend:

```powershell
mvn -f anysale-lead-service\pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev-keycloak"
mvn -f anysale-catalog-service\pom.xml spring-boot:run
mvn -f anysale-notification-service\pom.xml spring-boot:run
mvn -f anysale-ingestion-gateway\pom.xml spring-boot:run
```

Portas: Lead `8080`, Notification `8081`, Catalog `8082`, Gateway `8083`.

Na IntelliJ, use as configurações compartilhadas em `.run` ou o composto **AnySale (All Services)**. Ajuste variáveis secretas apenas na configuração local da sua IDE.

Valide:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8081/actuator/health
Invoke-WebRequest http://localhost:8082/actuator/health
Invoke-WebRequest http://localhost:8083/actuator/health
```

## 5. Executar o Console

No diretório `C:\Development\frontend\anysale-console`:

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev -- --host 127.0.0.1
```

Abra `http://localhost:5173`. Sem `VITE_LEAD_SERVICE_URL`, o Vite encaminha `/api` ao Lead Service em `http://localhost:8080`; isso evita CORS no desenvolvimento. Faça login pelo Keycloak. O Console não armazena nem envia segredos de WhatsApp/OpenAI.

Verificações antes de enviar mudanças:

```powershell
npm run build
npm run lint
```

## Fluxo local de teste

1. Suba Docker e os quatro serviços.
2. Faça login no Console.
3. Crie/consulte leads pelo Console ou pela coleção Postman.
4. Para testar recebimento sem Meta, use o endpoint normalizado descrito em [n8n-contract.md](n8n-contract.md).
5. Não teste envio real de WhatsApp sem número registrado, configuração Meta válida e consentimento do destinatário.

## Convenções de segurança

- Arquivos reais `.env`, `*.env`, credenciais de IDE e tokens nunca entram em commits.
- Se um token aparecer em log, screenshot, chat ou arquivo rastreado, revogue-o e gere outro.
- Não use valores de produção no ambiente local.
- Antes de um push, confira `git status`, `git diff --check` e procure segredos somente por nomes/padrões, sem copiar valores para issues ou documentação.
