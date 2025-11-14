# PowerShell script for PostgreSQL partitioning setup
# Usage: .\setup-partitioning.ps1 [database_name] [username] [password]

param(
    [string]$DatabaseName = "orderdb",
    [string]$Username = "postgres",
    [string]$Password = "postgres"
)

Write-Host "Setting up PostgreSQL partitioning for orders table..." -ForegroundColor Cyan
Write-Host "Database: $DatabaseName"
Write-Host "User: $Username"
Write-Host ""

# Set PGPASSWORD environment variable for psql
$env:PGPASSWORD = $Password

# Check if psql is available
try {
    $null = Get-Command psql -ErrorAction Stop
} catch {
    Write-Host "Error: psql command not found. Please install PostgreSQL client tools." -ForegroundColor Red
    exit 1
}

# Run migration scripts
Write-Host "Step 1: Creating partitioned table structure..." -ForegroundColor Yellow
$script1 = "src\main\resources\db\migration\V1__create_orders_partitioned_table.sql"
psql -U $Username -d $DatabaseName -f $script1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Partitioned table created successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to create partitioned table" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Migrating existing data (if any)..." -ForegroundColor Yellow
$script2 = "src\main\resources\db\migration\V2__migrate_existing_orders_to_partitioned.sql"
psql -U $Username -d $DatabaseName -f $script2

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Data migration completed" -ForegroundColor Green
} else {
    Write-Host "⚠ Warning: Data migration had issues (this is OK if table is empty)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Step 3: Setting up maintenance functions..." -ForegroundColor Yellow
$script3 = "src\main\resources\db\migration\V3__create_partition_maintenance_job.sql"
psql -U $Username -d $DatabaseName -f $script3

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Maintenance functions created successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to create maintenance functions" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Partitioning setup complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Verify partitions: SELECT * FROM pg_tables WHERE tablename LIKE 'orders_partitioned_%';"
Write-Host "2. Set up scheduled maintenance (see PARTITIONING_GUIDE.md)"
Write-Host "3. Update application to use partitioned table (if needed)"
Write-Host ""
Write-Host "For more information, see: PARTITIONING_GUIDE.md" -ForegroundColor Cyan

# Clear password from environment
Remove-Item Env:\PGPASSWORD

