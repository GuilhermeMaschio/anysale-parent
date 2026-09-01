[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string]$ConsolePublicUrl,
    [Parameter(Mandatory = $true)] [string]$BillingPublicUrl
)

function Normalize-Url([string]$value) { return $value.TrimEnd('/') }

$apiKey = Read-Host 'Chave de API do Asaas Sandbox' -AsSecureString
$plainApiKey = [System.Net.NetworkCredential]::new('', $apiKey).Password
if ([string]::IsNullOrWhiteSpace($plainApiKey)) { throw 'A chave de API Sandbox é obrigatória.' }

$webhookToken = [Environment]::GetEnvironmentVariable('ANYSALE_BILLING_ASAAS_WEBHOOK_TOKEN')
if ([string]::IsNullOrWhiteSpace($webhookToken)) {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $webhookToken = [Convert]::ToBase64String($bytes)
    Write-Host "Token de webhook gerado (cadastre-o no Asaas): $webhookToken"
}

$console = Normalize-Url $ConsolePublicUrl
$billing = Normalize-Url $BillingPublicUrl
$env:SERVER_PORT = '8084'
$env:ANYSALE_BILLING_ASAAS_ENABLED = 'true'
$env:ANYSALE_BILLING_ASAAS_API_KEY = $plainApiKey
$env:ANYSALE_BILLING_ASAAS_WEBHOOK_TOKEN = $webhookToken
$env:ANYSALE_BILLING_ASAAS_BASE_URL = 'https://api-sandbox.asaas.com/v3'
$env:ANYSALE_BILLING_CHECKOUT_SUCCESS_URL = "$console/billing/success"
$env:ANYSALE_BILLING_CHECKOUT_CANCEL_URL = "$console/billing/cancelled"
$env:ANYSALE_BILLING_CHECKOUT_EXPIRED_URL = "$console/billing/expired"

Write-Host "Billing Service preparado para Sandbox em $billing"
Write-Host "Cadastre no Asaas o webhook: $billing/v1/billing/webhooks/asaas"
Write-Host 'As variáveis existem somente nesta sessão do PowerShell. Inicie o Billing Service neste mesmo terminal.'
