-- Create a view and INSTEAD OF triggers to make partitioned table work with JPA
-- This allows JPA to use the "orders" table name while working with the partitioned table

-- Drop existing view if it exists
DROP VIEW IF EXISTS orders CASCADE;

-- Create view that maps to partitioned table
CREATE VIEW orders AS 
SELECT 
    id,
    customer_id,
    amount,
    status,
    description,
    created_at,
    updated_at
FROM orders_partitioned;

-- Create INSTEAD OF INSERT trigger
CREATE OR REPLACE FUNCTION orders_insert_trigger()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO orders_partitioned (
        id, 
        customer_id, 
        amount, 
        status, 
        description, 
        created_at, 
        updated_at
    ) VALUES (
        COALESCE(NEW.id, nextval('orders_partitioned_id_seq')),
        NEW.customer_id,
        NEW.amount,
        NEW.status,
        NEW.description,
        COALESCE(NEW.created_at, NOW()),
        COALESCE(NEW.updated_at, NOW())
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_insert_trigger
INSTEAD OF INSERT ON orders
FOR EACH ROW
EXECUTE FUNCTION orders_insert_trigger();

-- Create INSTEAD OF UPDATE trigger
CREATE OR REPLACE FUNCTION orders_update_trigger()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE orders_partitioned
    SET 
        customer_id = NEW.customer_id,
        amount = NEW.amount,
        status = NEW.status,
        description = NEW.description,
        updated_at = NOW()
    WHERE id = NEW.id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Order with id % not found', NEW.id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_update_trigger
INSTEAD OF UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION orders_update_trigger();

-- Create INSTEAD OF DELETE trigger
CREATE OR REPLACE FUNCTION orders_delete_trigger()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM orders_partitioned WHERE id = OLD.id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_delete_trigger
INSTEAD OF DELETE ON orders
FOR EACH ROW
EXECUTE FUNCTION orders_delete_trigger();

COMMENT ON VIEW orders IS 'View mapping to partitioned orders_partitioned table for JPA compatibility';
COMMENT ON FUNCTION orders_insert_trigger() IS 'Handles INSERT operations on orders view by routing to partitioned table';
COMMENT ON FUNCTION orders_update_trigger() IS 'Handles UPDATE operations on orders view by routing to partitioned table';
COMMENT ON FUNCTION orders_delete_trigger() IS 'Handles DELETE operations on orders view by routing to partitioned table';

