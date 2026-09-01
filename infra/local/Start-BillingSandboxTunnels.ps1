[CmdletBinding()]
param(
    [int]$ConsolePort = 5173,
    [int]$BillingPort = 8084,
    [int]$TimeoutSeconds = 30
)

$cloudflared = Get-Command cloudflared -ErrorAction SilentlyContinue
if (-not $cloudflared) { throw 'cloudflared não foi encontrado. Instale o Cloudflare Tunnel antes de continuar.' }

function Start-QuickTunnel([string]$name, [int]$port) {
    $stdout = [System.IO.Path]::GetTempFileName()
    $stderr = [System.IO.Path]::GetTempFileName()
    $process = Start-Process -FilePath $cloudflared.Source -ArgumentList @('tunnel', '--url', "http://127.0.0.1:$port", '--no-autoupdate') -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
    [pscustomobject]@{ Name = $name; Port = $port; Process = $process; Stdout = $stdout; Stderr = $stderr }
}

$tunnels = @(Start-QuickTunnel 'Console' $ConsolePort; Start-QuickTunnel 'Billing' $BillingPort)
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    Start-Sleep -Milliseconds 500
    $resolved = foreach ($tunnel in $tunnels) {
        $log = (Get-Content -LiteralPath $tunnel.Stdout -Raw -ErrorAction SilentlyContinue) + (Get-Content -LiteralPath $tunnel.Stderr -Raw -ErrorAction SilentlyContinue)
        $match = [regex]::Match($log, 'https://[-a-z0-9]+\.trycloudflare\.com')
        if ($match.Success) { [pscustomobject]@{ Name = $tunnel.Name; Url = $match.Value } }
    }
} while ($resolved.Count -lt 2 -and (Get-Date) -lt $deadline)

if ($resolved.Count -lt 2) {
    $tunnels | ForEach-Object { if (-not $_.Process.HasExited) { Stop-Process -Id $_.Process.Id } }
    throw 'Não foi possível obter as URLs públicas. Confirme que o Cloudflare Tunnel pode acessar a internet.'
}

$resolved | Format-Table -AutoSize
Write-Host "Webhook Asaas: $($resolved.Where({ $_.Name -eq 'Billing' }).Url)/v1/billing/webhooks/asaas"
Write-Host 'Os túneis continuam em execução em segundo plano enquanto os processos cloudflared estiverem ativos.'
