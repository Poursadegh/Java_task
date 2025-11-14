-- Create a scheduled job function for partition maintenance
-- This should be run periodically (e.g., via pg_cron extension or external scheduler)

-- Function to maintain partitions (create future ones, drop old ones)
CREATE OR REPLACE FUNCTION maintain_order_partitions(
    months_ahead INTEGER DEFAULT 3,
    months_to_keep INTEGER DEFAULT 24
) RETURNS TABLE(
    action TEXT,
    partition_name TEXT,
    message TEXT
) AS $$
DECLARE
    created_count INTEGER := 0;
    dropped_count INTEGER := 0;
BEGIN
    -- Create future partitions
    PERFORM create_future_partitions('orders_partitioned', months_ahead);
    GET DIAGNOSTICS created_count = ROW_COUNT;
    
    -- Drop old partitions (optional - comment out if you want to keep all data)
    -- PERFORM drop_old_partitions('orders_partitioned', months_to_keep);
    -- GET DIAGNOSTICS dropped_count = ROW_COUNT;
    
    -- Return summary
    RETURN QUERY SELECT 
        'SUMMARY'::TEXT,
        NULL::TEXT,
        format('Created %s partitions, dropped %s partitions', created_count, dropped_count)::TEXT;
END;
$$ LANGUAGE plpgsql;

-- If pg_cron extension is available, you can schedule this:
-- SELECT cron.schedule('maintain-order-partitions', '0 2 1 * *', 'SELECT maintain_order_partitions(3, 24)');
-- This runs at 2 AM on the 1st of every month

COMMENT ON FUNCTION maintain_order_partitions(INTEGER, INTEGER) IS 
'Maintains order partitions by creating future partitions and optionally dropping old ones. 
Should be run monthly via cron job or scheduler.';

