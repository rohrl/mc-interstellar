param([string]$Jdk = 'C:\Portable\jdks\temurin-21.0.12.1')
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location -LiteralPath $repo
if (Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -match 'net.fabricmc.devlaunchinjector.Main|net.fabricmc.loader.impl.launch.knot.KnotClient' }) {
    throw 'Close Minecraft normally before benchmarking, so it saves the world and releases GPU load.'
}
New-Item -ItemType Directory -Force -Path 'run/rtx/lib','run/rtx/classes' | Out-Null
$dependencies = @(
    @('lwjgl','lwjgl-3.3.3.jar','DC9C7B2D48E8396D68895F8902FFA01E46253DE44DFE927533FF09457EBFEEC4'),
    @('lwjgl','lwjgl-3.3.3-natives-windows.jar','5EE63A19187EFE5BB8747FD24F7B13257DB348DF5AD641113A3695E43F213959'),
    @('lwjgl-vulkan','lwjgl-vulkan-3.3.3.jar','D7715B77ABB79C02C2B1928C0B3686762DECCFBFD36A4C676C170C1EB138DE78'),
    @('lwjgl-shaderc','lwjgl-shaderc-3.3.3.jar','E0DD074BE9137E3359E66D911F7B2EE2BE6050BF6F0809244BD8206141DD6A49'),
    @('lwjgl-shaderc','lwjgl-shaderc-3.3.3-natives-windows.jar','FA0667C4C6982378A4F2F479064637ADFE0C0D2353AE930FED73073E903AD10D')
)
foreach ($dependency in $dependencies) {
    $file = Join-Path 'run/rtx/lib' $dependency[1]
    if (!(Test-Path -LiteralPath $file)) {
        Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/lwjgl/$($dependency[0])/3.3.3/$($dependency[1])" -OutFile $file
    }
    if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ine $dependency[2]) { throw "Checksum mismatch: $file" }
}
& "$Jdk/bin/javac.exe" -encoding UTF-8 -cp 'run/rtx/lib/*' -d run/rtx/classes tools/rtx-probe/Probe.java
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$Jdk/bin/java.exe" '-Dorg.lwjgl.system.stackSize=512' -Xmx2G -cp 'run/rtx/classes;run/rtx/lib/*' Probe
if ($LASTEXITCODE -ne 0) { throw 'Probe failed; do not use incomplete results' }
Write-Output 'Raw timings: run/rtx/results.jsonl'
