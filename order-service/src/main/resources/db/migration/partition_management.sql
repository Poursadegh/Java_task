-- Quick Reference: Partition Management Commands
-- Use these commands for common partition operations

-- ============================================
-- CREATE PARTITIONS
-- ============================================

-- Create partitions for next 3 months
SELECT create_future_partitions('orders_partitioned', 3);

-- Create a specific month partition
SELECT create_monthly_partition('orders_partitioned', '2024-01-01'::DATE);

-- Run full maintenance (create future, optionally drop old)
SELECT maintain_order_partitions(3, 24);  -- 3 months ahead, keep 24 months

-- ============================================
-- VIEW PARTITION INFORMATION
-- ============================================

-- List all partitions with sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) AS indexes_size
FROM pg_tables
WHERE tablename LIKE 'orders_partitioned_%'
ORDER BY tablename;

-- View partition constraints (date ranges)
SELECT 
    c.relname AS partition_name,
    pg_get_expr(c.relpartbound, c.oid) AS partition_constraint
FROM pg_class c
JOIN pg_inherits i ON c.oid = i.inhrelid
JOIN pg_class p ON i.inhparent = p.oid
WHERE p.relname = 'orders_partitioned'
ORDER BY partition_constraint;

-- Count rows in each partition
SELECT 
    schemaname,
    tablename,
    (SELECT COUNT(*) 
     FROM information_schema.tables t 
     WHERE t.table_schema = schemaname 
       AND t.table_name = tablename) AS estimated_rows
FROM pg_tables
WHERE tablename LIKE 'orders_partitioned_%';

-- More accurate row count (slower for large tables)
DO $$
DECLARE
    r RECORD;
    row_count BIGINT;
BEGIN
    FOR r IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE tablename LIKE 'orders_partitioned_%'
        ORDER BY tablename
    LOOP
        EXECUTE format('SELECT COUNT(*) FROM %I', r.tablename) INTO row_count;
        RAISE NOTICE 'Partition %: % rows', r.tablename, row_count;
    END LOOP;
END $$;

-- ============================================
-- PARTITION STATISTICS
-- ============================================

-- View partition access statistics
SELECT 
    schemaname,
    tablename,
    n_tup_ins AS inserts,
    n_tup_upd AS updates,
    n_tup_del AS deletes,
    n_live_tup AS live_rows,
    n_dead_tup AS dead_rows,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE tablename LIKE 'orders_partitioned_%'
ORDER BY n_live_tup DESC;

-- ============================================
-- MAINTENANCE OPERATIONS
-- ============================================

-- Vacuum a specific partition
VACUUM ANALYZE orders_partitioned_2024_01;

-- Vacuum all partitions
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE tablename LIKE 'orders_partitioned_%'
    LOOP
        EXECUTE format('VACUUM ANALYZE %I', r.tablename);
        RAISE NOTICE 'Vacuumed: %', r.tablename;
    END LOOP;
END $$;

-- Reindex all partitions (run during maintenance window)
REINDEX TABLE orders_partitioned;

-- ============================================
-- DATA MANAGEMENT
-- ============================================

-- Drop old partitions (BE CAREFUL - backup first!)
-- SELECT drop_old_partitions('orders_partitioned', 12);  -- Keep last 12 months

-- Archive a partition (move to archive table)
-- CREATE TABLE orders_archive_2023_12 (LIKE orders_partitioned_2023_12 INCLUDING ALL);
-- INSERT INTO orders_archive_2023_12 SELECT * FROM orders_partitioned_2023_12;
-- DROP TABLE orders_partitioned_2023_12;

-- ============================================
-- QUERY OPTIMIZATION
-- ============================================

-- Check if partition pruning is working
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM orders_partitioned 
WHERE created_at >= '2024-01-01' 
  AND created_at < '2024-02-01';

-- Should show: "Partition pruning" in the plan

-- ============================================
-- TROUBLESHOOTING
-- ============================================

-- Find missing partitions for a date range
SELECT 
    generate_series(
        date_trunc('month', '2024-01-01'::DATE),
        date_trunc('month', '2024-12-01'::DATE),
        '1 month'::INTERVAL
    )::DATE AS month_start,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_inherits i ON c.oid = i.inhrelid
            JOIN pg_class p ON i.inhparent = p.oid
            WHERE p.relname = 'orders_partitioned'
              AND c.relname = 'orders_partitioned_' || to_char(
                  generate_series(
                      date_trunc('month', '2024-01-01'::DATE),
                      date_trunc('month', '2024-12-01'::DATE),
                      '1 month'::INTERVAL
                  )::DATE, 'YYYY_MM'
              )
        ) THEN 'EXISTS'
        ELSE 'MISSING'
    END AS status;

