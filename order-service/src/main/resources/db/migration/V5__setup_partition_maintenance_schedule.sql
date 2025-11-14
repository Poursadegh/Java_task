-- Set up partition maintenance schedule
-- This creates a function that can be called by external schedulers

-- Grant execute permissions
GRANT EXECUTE ON FUNCTION maintain_order_partitions(INTEGER, INTEGER) TO appuser;
GRANT EXECUTE ON FUNCTION create_future_partitions(TEXT, INTEGER) TO appuser;
GRANT EXECUTE ON FUNCTION drop_old_partitions(TEXT, INTEGER) TO appuser;

-- Create a simple maintenance function that can be called via cron or scheduler
CREATE OR REPLACE FUNCTION run_partition_maintenance()
RETURNS TEXT AS $$
DECLARE
    result TEXT;
BEGIN
    -- Create partitions for next 3 months, keep last 24 months
    PERFORM maintain_order_partitions(3, 24);
    result := 'Partition maintenance completed successfully at ' || NOW()::TEXT;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

GRANT EXECUTE ON FUNCTION run_partition_maintenance() TO appuser;

COMMENT ON FUNCTION run_partition_maintenance() IS 
'Maintenance function to be called monthly. Creates future partitions and optionally drops old ones.';

-- Example: To schedule this monthly, add to crontab:
-- 0 2 1 * * psql -U appuser -d orderdb -c "SELECT run_partition_maintenance();"

-- Or use pg_cron extension if available:
-- SELECT cron.schedule('partition-maintenance', '0 2 1 * *', 'SELECT run_partition_maintenance();');

