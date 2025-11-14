# PowerShell script to run partition maintenance
# This should be scheduled to run monthly (e.g., via Task Scheduler on Windows)

param(
    [string]$ContainerName = "comms-postgres",
    [string]$Database = "orderdb",
    [string]$Username = "appuser",
    [string]$Password = "123456"
)

Write-Host "Running partition maintenance..." -ForegroundColor Cyan
Write-Host "Container: $ContainerName"
Write-Host "Database: $Database"
Write-Host ""

# Set password environment variable
$env:PGPASSWORD = $Password

try {
    $result = docker exec $ContainerName psql -U $Username -d $Database -c "SELECT run_partition_maintenance();"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Partition maintenance completed successfully" -ForegroundColor Green
        Write-Host $result
    } else {
        Write-Host "✗ Partition maintenance failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error running partition maintenance: $_" -ForegroundColor Red
    exit 1
} finally {
    Remove-Item Env:\PGPASSWORD
}

Write-Host ""
Write-Host "Maintenance complete!" -ForegroundColor Green

