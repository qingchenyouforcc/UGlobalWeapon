param([string]$Python = $env:WEAPON_PYTHON)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$project = Split-Path $PSScriptRoot -Parent
$packSource = Join-Path $project 'resource-pack'
$packZip = Join-Path $project 'dist/UGlobalWeapon-Demo-ResourcePack.zip'
$hostDirectory = Join-Path $project 'run/resource-pack-host'
$propertiesPath = Join-Path $project 'run/paper-server/server.properties'
New-Item -ItemType Directory -Path (Split-Path $packZip -Parent) -Force | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)

if (-not $Python) {
    $bundledPython = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
    if (Test-Path -LiteralPath $bundledPython) { $Python = $bundledPython }
    else { $Python = (Get-Command python -ErrorAction Stop).Source }
}
& $Python (Join-Path $PSScriptRoot 'modeling/validate_resources.py')
if ($LASTEXITCODE -ne 0) { throw 'Resource atlas validation failed; pack was not replaced.' }

# Validate JSON before replacing the downloadable pack.
Get-ChildItem -LiteralPath $packSource -Recurse -File | Where-Object {
    $_.Extension -eq '.json' -or $_.Name -eq 'pack.mcmeta'
} | ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json | Out-Null }

$zipStream = [System.IO.File]::Open($packZip, [System.IO.FileMode]::Create)
$archive = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -LiteralPath $packSource -Recurse -File | Sort-Object FullName | ForEach-Object {
        # ZIP resource paths must use forward slashes, including on Windows.
        $entryName = $_.FullName.Substring($packSource.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $entryName) | Out-Null
    }
} finally {
    $archive.Dispose()
    $zipStream.Dispose()
}

if (Test-Path -LiteralPath $hostDirectory) {
    Copy-Item -LiteralPath $packZip -Destination (Join-Path $hostDirectory 'UGlobalWeapon-Demo-ResourcePack.zip') -Force
}
$packHash = (Get-FileHash -LiteralPath $packZip -Algorithm SHA1).Hash.ToLowerInvariant()
if (Test-Path -LiteralPath $propertiesPath) {
$properties = [System.IO.File]::ReadAllText($propertiesPath)
if ($properties -match '(?m)^resource-pack-sha1=') {
    $properties = [regex]::Replace($properties, '(?m)^resource-pack-sha1=[^\r\n]*', "resource-pack-sha1=$packHash")
} else {
    $properties += "`r`nresource-pack-sha1=$packHash`r`n"
}
[System.IO.File]::WriteAllText($propertiesPath, $properties, $utf8)
}
Write-Host "Resource pack built and hosted copy updated. SHA-1: $packHash"
Write-Host 'Restart the Minecraft server, then reconnect clients to download the updated pack.'
