param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$resolvedPath = Resolve-Path -LiteralPath $Path -ErrorAction Stop
$loaded = 0

foreach ($rawLine in Get-Content -LiteralPath $resolvedPath) {
    $line = $rawLine.Trim()

    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
        continue
    }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) {
        throw "Invalid env entry: $rawLine"
    }

    $name = $parts[0].Trim()
    $value = $parts[1]

    if ([string]::IsNullOrWhiteSpace($name)) {
        throw "Invalid env key in line: $rawLine"
    }

    Set-Item -Path ("Env:" + $name) -Value $value
    $loaded++
}

Write-Host "Loaded $loaded environment variables from $resolvedPath"
