# Build AppleSkin for Minecraft 1.21.4 from official source (1.21.3-fabric branch).
# Run from RichClient folder. Requires: git, Java 21.
$ErrorActionPreference = "Stop"
$clientRoot = $PSScriptRoot
$appleSkinDir = Join-Path $clientRoot "AppleSkin-build"
$libsDir = Join-Path $clientRoot "libs"

if (-not (Test-Path $libsDir)) { New-Item -ItemType Directory -Path $libsDir | Out-Null }

if (-not (Test-Path $appleSkinDir)) {
    Write-Host "Cloning AppleSkin..."
    git clone --depth 1 --branch 1.21.3-fabric https://github.com/squeek502/AppleSkin.git $appleSkinDir
} else {
    Write-Host "AppleSkin-build exists, updating..."
    Push-Location $appleSkinDir
    git fetch origin 1.21.3-fabric
    git checkout 1.21.3-fabric
    git pull
    Pop-Location
}

$gradleProps = Join-Path $appleSkinDir "gradle.properties"
$content = Get-Content $gradleProps -Raw
$content = $content -replace "minecraft_version=1\.21\.3", "minecraft_version=1.21.4"
$content = $content -replace "yarn_mappings=1\.21\.3\+build\.\d+", "yarn_mappings=1.21.4+build.8"
$content = $content -replace "loader_version=0\.16\.\d+", "loader_version=0.16.14"
$content = $content -replace "fabric_version=[\d\.]+\+1\.21\.3", "fabric_version=0.119.3+1.21.4"
$content = $content -replace "cloth_version=[\d\.]+", "cloth_version=17.0.144"
Set-Content -Path $gradleProps -Value $content -NoNewline:$false

Write-Host "Building AppleSkin for 1.21.4..."
Push-Location $appleSkinDir
& .\gradlew.bat build --no-daemon
if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }
Pop-Location

$buildLibs = Join-Path $appleSkinDir "build\libs"
$jars = Get-ChildItem $buildLibs -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "sources|api" }
$jar = $jars | Select-Object -First 1
if ($jar) {
    $dest = Join-Path $libsDir "appleskin-fabric-mc1.21.4-3.0.6.jar"
    Copy-Item $jar.FullName -Destination $dest -Force
    Write-Host "Done. Jar copied to libs\appleskin-fabric-mc1.21.4-3.0.6.jar"
} else {
    Write-Host "Build succeeded but jar not found in build\libs"
    exit 1
}
