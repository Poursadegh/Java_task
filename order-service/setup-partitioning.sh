#!/bin/bash
# Quick setup script for PostgreSQL partitioning
# Usage: ./setup-partitioning.sh [database_name] [username]

DB_NAME=${1:-orderdb}
DB_USER=${2:-postgres}

echo "Setting up PostgreSQL partitioning for orders table..."
echo "Database: $DB_NAME"
echo "User: $DB_USER"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "Error: psql command not found. Please install PostgreSQL client tools."
    exit 1
fi

# Run migration scripts
echo "Step 1: Creating partitioned table structure..."
psql -U "$DB_USER" -d "$DB_NAME" -f src/main/resources/db/migration/V1__create_orders_partitioned_table.sql

if [ $? -eq 0 ]; then
    echo "✓ Partitioned table created successfully"
else
    echo "✗ Failed to create partitioned table"
    exit 1
fi

echo ""
echo "Step 2: Migrating existing data (if any)..."
psql -U "$DB_USER" -d "$DB_NAME" -f src/main/resources/db/migration/V2__migrate_existing_orders_to_partitioned.sql

if [ $? -eq 0 ]; then
    echo "✓ Data migration completed"
else
    echo "⚠ Warning: Data migration had issues (this is OK if table is empty)"
fi

echo ""
echo "Step 3: Setting up maintenance functions..."
psql -U "$DB_USER" -d "$DB_NAME" -f src/main/resources/db/migration/V3__create_partition_maintenance_job.sql

if [ $? -eq 0 ]; then
    echo "✓ Maintenance functions created successfully"
else
    echo "✗ Failed to create maintenance functions"
    exit 1
fi

echo ""
echo "=========================================="
echo "Partitioning setup complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Verify partitions: SELECT * FROM pg_tables WHERE tablename LIKE 'orders_partitioned_%';"
echo "2. Set up scheduled maintenance (see PARTITIONING_GUIDE.md)"
echo "3. Update application to use partitioned table (if needed)"
echo ""
echo "For more information, see: PARTITIONING_GUIDE.md"

