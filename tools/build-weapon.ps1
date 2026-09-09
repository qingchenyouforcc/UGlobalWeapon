param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$project = $workspace
$classes = Join-Path $project 'target/release-classes'
. (Join-Path $PSScriptRoot 'toolchain.ps1')
$JavaHome = Resolve-WeaponJavaHome $JavaHome
$jdk = Join-Path $JavaHome 'bin'
if (-not (Test-Path (Join-Path $jdk 'javac.exe'))) { throw 'Set JAVA_HOME or pass -JavaHome to a JDK 21 installation.' }
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$classpath = (@((Join-Path $project 'run/dependencies/spigot-api-1.21.1.jar')) + @(
    Get-ChildItem (Join-Path $workspace 'run/paper-server/libraries') -Recurse -Filter '*.jar' | ForEach-Object FullName
)) -join ';'
$sources = @(Get-ChildItem (Join-Path $project 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $jdk 'javac.exe') -proc:none -encoding UTF-8 --release 21 -cp $classpath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Weapon compilation failed' }
Copy-Item -Path (Join-Path $project 'src/main/resources/*') -Destination $classes -Recurse -Force
& (Join-Path $jdk 'jar.exe') --create --file (Join-Path $project 'target/weapon-1.0-SNAPSHOT.jar') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Weapon packaging failed' }
Write-Host 'Built weapon-1.0-SNAPSHOT.jar (compilation only).'
