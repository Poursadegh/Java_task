$ErrorActionPreference = "Stop"

Write-Host "Running pre-commit checks..." -ForegroundColor Cyan

$rootDir = git rev-parse --show-toplevel
Set-Location $rootDir

Write-Host "Compiling project..." -ForegroundColor Yellow
mvn clean compile -DskipTests -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Compilation failed. Commit aborted." -ForegroundColor Red
    exit 1
}

Write-Host "Running checkstyle..." -ForegroundColor Yellow
mvn checkstyle:check -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Checkstyle violations found. Commit aborted." -ForegroundColor Red
    exit 1
}

Write-Host "Running spotbugs..." -ForegroundColor Yellow
mvn spotbugs:check -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ SpotBugs issues found. Commit aborted." -ForegroundColor Red
    exit 1
}

Write-Host "✅ All pre-commit checks passed!" -ForegroundColor Green
exit 0

