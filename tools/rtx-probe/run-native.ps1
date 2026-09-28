param([Parameter(Mandatory=$true)][string]$Scene,[string]$Jdk='C:\Portable\jdks\temurin-21.0.12.1')
$ErrorActionPreference='Stop'
$scenePath=(Resolve-Path -LiteralPath $Scene).Path
if(-not (Test-Path -LiteralPath (Join-Path $scenePath 'triangles.bin'))){throw 'Scene must name a complete native capture directory'}
& "$PSScriptRoot/run.ps1" -Jdk $Jdk -PrepareOnly
& "$Jdk/bin/javac.exe" -encoding UTF-8 -cp 'run/rtx/classes;run/rtx/lib/*' -d run/rtx/classes tools/rtx-probe/NativeReplay.java
if($LASTEXITCODE){throw 'Native replay compilation failed'}
& "$Jdk/bin/java.exe" '-Dorg.lwjgl.system.stackSize=512' -Xmx4G -cp 'run/rtx/classes;run/rtx/lib/*' NativeReplay $scenePath
if($LASTEXITCODE){throw 'Native replay failed; inspect correctness/setup before using timing'}
