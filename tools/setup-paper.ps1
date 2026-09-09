$ErrorActionPreference = 'Stop'

$root = Split-Path $PSScriptRoot -Parent
$serverDir = Join-Path $root 'run/paper-server'
$toolsDir = Join-Path $root 'run/toolchains'
$mavenDir = Join-Path $toolsDir 'apache-maven-3.9.11'
$mavenZip = Join-Path $toolsDir 'apache-maven-3.9.11-bin.zip'

New-Item -ItemType Directory -Force -Path $serverDir, $toolsDir | Out-Null

if (-not (Test-Path (Join-Path $mavenDir 'bin\mvn.cmd'))) {
    if (-not (Test-Path $mavenZip) -or ((Get-Item $mavenZip).Length -lt 1000000)) {
        Remove-Item $mavenZip -Force -ErrorAction SilentlyContinue
        & curl.exe --ssl-no-revoke -L --fail --retry 3 -o $mavenZip 'https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.zip'
        if ($LASTEXITCODE -ne 0) { throw "Maven download failed with exit code $LASTEXITCODE" }
    }
    Expand-Archive -Path $mavenZip -DestinationPath $toolsDir -Force
}

$paperVersion = '1.21.1'
$userAgent = 'UGlobalWeaponSetup/1.0 (local development)'
$buildsJson = & curl.exe --ssl-no-revoke -L --fail -H "User-Agent: $userAgent" "https://fill.papermc.io/v3/projects/paper/versions/$paperVersion/builds"
if ($LASTEXITCODE -ne 0) { throw "Paper build metadata download failed with exit code $LASTEXITCODE" }
$builds = $buildsJson | ConvertFrom-Json
$build = $builds | Where-Object { $_.channel -eq 'STABLE' } | Select-Object -First 1
if (-not $build) { throw "No stable Paper build found for Minecraft $paperVersion" }
$paperJarName = $build.downloads.'server:default'.name
$paperJar = Join-Path $serverDir $paperJarName
if (-not (Test-Path $paperJar)) {
    $paperUrl = $build.downloads.'server:default'.url
    & curl.exe --ssl-no-revoke -L --fail --retry 3 -o $paperJar $paperUrl
    if ($LASTEXITCODE -ne 0) { throw "Paper download failed with exit code $LASTEXITCODE" }
}

$pluginDir = Join-Path $serverDir 'plugins'
New-Item -ItemType Directory -Force -Path $pluginDir | Out-Null
$mvn = Join-Path $mavenDir 'bin\mvn.cmd'
. (Join-Path $PSScriptRoot 'toolchain.ps1')
$jdk21 = Resolve-WeaponJavaHome $env:JAVA_HOME
if (-not (Test-Path (Join-Path $jdk21 'bin\javac.exe'))) { throw "JDK 21 was not found at $jdk21" }
$env:JAVA_HOME = $jdk21
$env:Path = (Join-Path $jdk21 'bin') + ';' + $env:Path
Push-Location $root
try {
    & $mvn clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$pluginJar = Get-ChildItem (Join-Path $root 'target') -Filter '*.jar' |
    Where-Object { $_.Name -notlike 'original-*' } |
    Select-Object -First 1
if (-not $pluginJar) { throw 'Compiled plugin jar was not found.' }
Copy-Item $pluginJar.FullName (Join-Path $pluginDir $pluginJar.Name) -Force

Set-Content -Path (Join-Path $serverDir 'eula.txt') -Value 'eula=true' -Encoding ASCII
Set-Content -Path (Join-Path $serverDir 'start.bat') -Value @"
@echo off
cd /d "%~dp0"
set "JAVA_HOME=$jdk21"
"%JAVA_HOME%\bin\java.exe" -Xms2G -Xmx4G -jar "$paperJarName" --nogui
pause
"@ -Encoding ASCII

Write-Host "Paper server configured in: $serverDir"
Write-Host "Plugin installed as: $($pluginJar.Name)"
Write-Host "Run run\paper-server\start.bat to start the local server."
