-- Migration script to move existing orders to partitioned table
-- WARNING: This script assumes you have an existing 'orders' table
-- Run this only if you have existing data to migrate

-- Step 1: Check if old orders table exists and has data
DO $$
DECLARE
    row_count BIGINT;
BEGIN
    -- Check if old table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN
        SELECT COUNT(*) INTO row_count FROM orders;
        RAISE NOTICE 'Found % rows in existing orders table', row_count;
        
        IF row_count > 0 THEN
            -- Copy data to partitioned table
            INSERT INTO orders_partitioned 
            SELECT * FROM orders
            ON CONFLICT DO NOTHING;
            
            RAISE NOTICE 'Migrated % rows to partitioned table', row_count;
        END IF;
    ELSE
        RAISE NOTICE 'No existing orders table found. Skipping migration.';
    END IF;
END $$;

-- Step 2: Rename tables (optional - uncomment if you want to replace the old table)
-- ALTER TABLE orders RENAME TO orders_old;
-- ALTER TABLE orders_partitioned RENAME TO orders;

-- Alternative: Create a view that points to the partitioned table
-- This allows existing code to work without changes
-- DROP VIEW IF EXISTS orders;
-- CREATE VIEW orders AS SELECT * FROM orders_partitioned;

-- Note: If using JPA with @Table(name = "orders"), you may need to either:
-- 1. Rename orders_partitioned to orders (recommended)
-- 2. Update the @Table annotation to use "orders_partitioned"
-- 3. Create a view named "orders" that selects from "orders_partitioned"

