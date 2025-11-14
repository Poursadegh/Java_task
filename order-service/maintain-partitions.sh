#!/bin/bash
# Bash script to run partition maintenance
# This should be scheduled to run monthly (e.g., via cron)

CONTAINER_NAME=${1:-comms-postgres}
DATABASE=${2:-orderdb}
USERNAME=${3:-appuser}
PASSWORD=${4:-123456}

echo "Running partition maintenance..."
echo "Container: $CONTAINER_NAME"
echo "Database: $DATABASE"
echo ""

export PGPASSWORD=$PASSWORD

docker exec $CONTAINER_NAME psql -U $USERNAME -d $DATABASE -c "SELECT run_partition_maintenance();"

if [ $? -eq 0 ]; then
    echo "✓ Partition maintenance completed successfully"
else
    echo "✗ Partition maintenance failed"
    exit 1
fi

unset PGPASSWORD

echo ""
echo "Maintenance complete!"

