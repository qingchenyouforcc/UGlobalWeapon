function Resolve-WeaponJavaHome {
    param([string]$Preferred)
    $candidates = @($Preferred, 'C:/Program Files/Java/jdk-21') | Where-Object { $_ } | Select-Object -Unique
    foreach ($candidate in $candidates) {
        $compiler = Join-Path $candidate 'bin/javac.exe'
        if (-not (Test-Path -LiteralPath $compiler)) { continue }
        $version = (& $compiler -version 2>&1 | Out-String).Trim()
        if ($LASTEXITCODE -eq 0 -and $version -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 21) {
            return $candidate
        }
    }
    throw 'JDK 21 or newer is required. Set JAVA_HOME or pass -JavaHome to the build script.'
}
