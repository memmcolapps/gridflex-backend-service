CREATE TABLE IF NOT EXISTS public.api_client (
        id UUID PRIMARY KEY,
        client_id VARCHAR(100) UNIQUE NOT NULL,
        client_name VARCHAR(150) NOT NULL,
        client_secret_hash VARCHAR(255) NOT NULL,
        org_id UUID NULL,
        status BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.api_client_scope  (
          id UUID PRIMARY KEY,
          client_id VARCHAR(100) NOT NULL,
          scope VARCHAR(100) NOT NULL,
          CONSTRAINT fk_client_scope
              FOREIGN KEY (client_id)
                  REFERENCES api_client(client_id)
                  ON DELETE CASCADE
);