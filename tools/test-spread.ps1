param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$project = $workspace
$classes = Join-Path $project 'target\spread-check-classes'
. (Join-Path $PSScriptRoot 'toolchain.ps1')
$JavaHome = Resolve-WeaponJavaHome $JavaHome
$jdk = Join-Path $JavaHome 'bin'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$classpath = (@((Join-Path $project 'run/dependencies/spigot-api-1.21.1.jar')) + @(
    Get-ChildItem (Join-Path $workspace 'run/paper-server/libraries') -Recurse -Filter '*.jar' |
        ForEach-Object FullName
)) -join ';'
$sources = @(Get-ChildItem (Join-Path $project 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources += Join-Path $project 'src\test\java\org\mmga\uglobal\weapon\SpreadRegressionTest.java'
& (Join-Path $jdk 'javac.exe') -proc:none -encoding UTF-8 --release 21 -cp $classpath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& (Join-Path $jdk 'java.exe') -cp "$classes;$classpath" org.mmga.uglobal.weapon.SpreadRegressionTest
if ($LASTEXITCODE -ne 0) { throw 'Spread regression failed' }
