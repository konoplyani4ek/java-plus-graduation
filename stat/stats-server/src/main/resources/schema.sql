CREATE TABLE IF NOT EXISTS hits (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app VARCHAR(100) NOT NULL CHECK (length(trim(app)) > 0),
    uri TEXT NOT NULL CHECK (length(trim(uri)) > 0),
    ip VARCHAR(45) NOT NULL CHECK (length(trim(ip)) > 0),
    created_at TIMESTAMP NOT NULL
);