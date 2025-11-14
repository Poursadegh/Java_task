-- PostgreSQL Partitioning Setup for Orders Table
-- This script sets up range partitioning by month based on createdAt

-- Step 1: Create the partitioned table (parent table)
-- Note: Primary key must include partition key (created_at) for PostgreSQL partitioning
-- We'll use a unique index on id for JPA compatibility
CREATE TABLE IF NOT EXISTS orders_partitioned (
    id BIGSERIAL NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Note: Unique constraints on partitioned tables must include partition key
-- JPA will work with composite primary key (id, created_at)
-- For queries by id only, PostgreSQL will search across partitions efficiently

-- Step 2: Create indexes on the partitioned table
-- These indexes will be automatically created on all partitions
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders_partitioned (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders_partitioned (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders_partitioned (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON orders_partitioned (customer_id, created_at);

-- Step 3: Create initial partitions for the current and next 12 months
-- This creates monthly partitions starting from the current month

-- Function to create monthly partitions
CREATE OR REPLACE FUNCTION create_monthly_partition(
    table_name TEXT,
    start_date DATE
) RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := (start_date + INTERVAL '1 month')::DATE;
    
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I PARTITION OF %I
        FOR VALUES FROM (%L) TO (%L)',
        partition_name, table_name, start_date, end_date
    );
    
    RAISE NOTICE 'Created partition: %', partition_name;
END;
$$ LANGUAGE plpgsql;

-- Create partitions for current month and next 12 months
DO $$
DECLARE
    current_month DATE;
    i INTEGER;
BEGIN
    current_month := date_trunc('month', CURRENT_DATE);
    
    FOR i IN 0..12 LOOP
        PERFORM create_monthly_partition('orders_partitioned', (current_month + (i || ' months')::INTERVAL)::DATE);
    END LOOP;
END $$;

-- Step 4: Create a function to automatically create future partitions
-- This should be run periodically (e.g., monthly via cron job)
CREATE OR REPLACE FUNCTION create_future_partitions(
    table_name TEXT DEFAULT 'orders_partitioned',
    months_ahead INTEGER DEFAULT 3
) RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    start_date DATE;
    end_date DATE;
    current_max_date DATE;
    i INTEGER;
BEGIN
    -- Find the latest partition end date
    SELECT MAX(upper_bound::DATE) INTO current_max_date
    FROM (
        SELECT (regexp_match(pg_get_expr(c.relpartbound, c.oid), 'FOR VALUES FROM \(''([^'']+)''\) TO \(''([^'']+)''\)'))[2]::DATE AS upper_bound
        FROM pg_class c
        JOIN pg_inherits i ON c.oid = i.inhrelid
        JOIN pg_class p ON i.inhparent = p.oid
        WHERE p.relname = table_name
    ) AS partition_bounds;
    
    -- If no partitions exist, start from current month
    IF current_max_date IS NULL THEN
        current_max_date := date_trunc('month', CURRENT_DATE);
    END IF;
    
    -- Create partitions for the specified number of months ahead
    FOR i IN 1..months_ahead LOOP
        start_date := current_max_date + ((i - 1) || ' months')::INTERVAL;
        end_date := start_date + INTERVAL '1 month';
        partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
        
        -- Check if partition already exists
        IF NOT EXISTS (
            SELECT 1 FROM pg_class WHERE relname = partition_name
        ) THEN
            EXECUTE format('
                CREATE TABLE %I PARTITION OF %I
                FOR VALUES FROM (%L) TO (%L)',
                partition_name, table_name, start_date, end_date
            );
            RAISE NOTICE 'Created partition: %', partition_name;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Step 5: Create a function to drop old partitions (for data retention)
-- This can be used to archive or drop partitions older than a specified period
CREATE OR REPLACE FUNCTION drop_old_partitions(
    table_name TEXT DEFAULT 'orders_partitioned',
    months_to_keep INTEGER DEFAULT 12
) RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    partition_date DATE;
    cutoff_date DATE;
BEGIN
    cutoff_date := date_trunc('month', CURRENT_DATE) - (months_to_keep || ' months')::INTERVAL;
    
    -- Find and drop partitions older than cutoff_date
    FOR partition_name, partition_date IN
        SELECT c.relname, 
               (regexp_match(pg_get_expr(c.relpartbound, c.oid), 'FOR VALUES FROM \(''([^'']+)''\) TO \(''([^'']+)''\)'))[1]::DATE
        FROM pg_class c
        JOIN pg_inherits i ON c.oid = i.inhrelid
        JOIN pg_class p ON i.inhparent = p.oid
        WHERE p.relname = table_name
          AND (regexp_match(pg_get_expr(c.relpartbound, c.oid), 'FOR VALUES FROM \(''([^'']+)''\) TO \(''([^'']+)''\)'))[1]::DATE < cutoff_date
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE', partition_name);
        RAISE NOTICE 'Dropped old partition: % (date: %)', partition_name, partition_date;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Step 6: Create a view for easier querying (optional, if you want to keep the old table name)
-- CREATE OR REPLACE VIEW orders AS SELECT * FROM orders_partitioned;

-- Step 7: Grant necessary permissions (adjust as needed)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON orders_partitioned TO your_app_user;
-- GRANT USAGE, SELECT ON SEQUENCE orders_partitioned_id_seq TO your_app_user;

COMMENT ON TABLE orders_partitioned IS 'Partitioned orders table for handling millions of records';
COMMENT ON FUNCTION create_monthly_partition(TEXT, DATE) IS 'Creates a monthly partition for the specified table and start date';
COMMENT ON FUNCTION create_future_partitions(TEXT, INTEGER) IS 'Automatically creates future partitions for the specified number of months ahead';
COMMENT ON FUNCTION drop_old_partitions(TEXT, INTEGER) IS 'Drops partitions older than the specified number of months';

