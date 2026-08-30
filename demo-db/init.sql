CREATE ROLE advisor LOGIN PASSWORD 'advisor';

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL,
    email TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    price NUMERIC(12,2) NOT NULL
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status TEXT NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL
);

INSERT INTO users (username, email, created_at)
SELECT 'user_' || n,
       CASE WHEN n = 42 THEN 'alex.morgan@example.com' ELSE 'customer_' || n || '@example.test' END,
       now() - (random() * interval '5 years')
FROM generate_series(1, 250000) n;

INSERT INTO products (sku, name, price)
SELECT 'SKU-' || lpad(n::text, 7, '0'), 'Product ' || n, round((5 + random() * 995)::numeric, 2)
FROM generate_series(1, 25000) n;

INSERT INTO orders (user_id, status, total_amount, created_at)
SELECT 1 + (random() * 249999)::bigint,
       (ARRAY['paid', 'shipped', 'cancelled', 'pending'])[1 + (random() * 3)::int],
       round((10 + random() * 1490)::numeric, 2),
       now() - (random() * interval '4 years')
FROM generate_series(1, 1000000);

INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT 1 + (random() * 999999)::bigint,
       1 + (random() * 24999)::bigint,
       1 + (random() * 4)::int,
       round((5 + random() * 995)::numeric, 2)
FROM generate_series(1, 2500000);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

ANALYZE;

GRANT CONNECT ON DATABASE shop TO advisor;
GRANT USAGE ON SCHEMA public TO advisor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO advisor;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO advisor;
