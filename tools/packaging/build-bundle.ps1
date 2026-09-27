#Requires -Version 5.1
<#
.SYNOPSIS
  Build the Royan distributable bundle: fat jar, jpackage app-image, zip.
.DESCRIPTION
  Runs 'mvn package', then jpackage --type app-image (needs no WiX on
  Windows) into dist/, copies the fat jar alongside, and zips the app
  image as dist/RoyanRPG-win.zip.
.PARAMETER SkipTests
  Skip the test suite during packaging (faster iteration; the release
  gate still requires a full green 'mvn test').
#>
[CmdletBinding()]
param(
  [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
Set-Location $root

if (-not $env:JAVA_HOME -and (Test-Path (Join-Path $env:TEMP "royan-tools\jdk17"))) {
  $env:JAVA_HOME = Join-Path $env:TEMP "royan-tools\jdk17"
  $env:Path = "$env:JAVA_HOME\bin;$(Join-Path $env:TEMP 'royan-tools\maven\bin');$env:Path"
}

$mvnArgs = @("package")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
Write-Host "[royan] mvn $($mvnArgs -join ' ')"
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw "mvn package failed with exit $LASTEXITCODE" }

$jar = Get-ChildItem (Join-Path $root "target") -Filter "royan-*.jar" |
  Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-javadoc.jar" -and $_.Name -notlike "*-shaded.jar" } |
  Sort-Object Name |
  Select-Object -First 1
if (-not $jar) { throw "no fat jar found in target/ after mvn package" }

[xml]$pom = Get-Content (Join-Path $root "pom.xml")
$appVersion = ($pom.project.version -split "-")[0]
if ($appVersion -notmatch '^\d+(\.\d+){0,2}$') { $appVersion = "0.1.0" }

$jpackage = $null
foreach ($candidate in @(
    (Join-Path $env:JAVA_HOME "bin\jpackage.exe"),
    "jpackage", "jpackage.exe")) {
  if (-not $candidate) { continue }
  $found = Get-Command $candidate -ErrorAction SilentlyContinue
  if ($found) { $jpackage = $found.Source; break }
}
if (-not $jpackage) { throw "jpackage not found (needs JDK 17+ on JAVA_HOME or PATH)" }

$dist = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
$appImage = Join-Path $dist "RoyanRPG"
if (Test-Path $appImage) { Remove-Item -Recurse -Force $appImage }

$stage = Join-Path $dist ".jpackage-input"
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Force -Path $stage | Out-Null
Copy-Item $jar.FullName (Join-Path $stage $jar.Name) -Force

Write-Host "[royan] jpackage app-image $appVersion"
& $jpackage --type app-image --name RoyanRPG --app-version $appVersion `
  --input $stage --main-jar $jar.Name `
  --main-class com.chris.cardgame.Main --dest $dist `
  --description "Royan RPG Card Game" --vendor "Royan" --win-console --arguments serve --arguments 0 `
  --java-options "-Dfile.encoding=UTF-8"
$jpackageExit = $LASTEXITCODE
Remove-Item -Recurse -Force $stage
if ($jpackageExit -ne 0) { throw "jpackage failed with exit $jpackageExit" }

Copy-Item $jar.FullName (Join-Path $dist $jar.Name) -Force

$zip = Join-Path $dist "RoyanRPG-win.zip"
if (Test-Path $zip) { Remove-Item -Force $zip }
Compress-Archive -Path $appImage -DestinationPath $zip
Write-Host "[royan] bundle ready: $zip"
