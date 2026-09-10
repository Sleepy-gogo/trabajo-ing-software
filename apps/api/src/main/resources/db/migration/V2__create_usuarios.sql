CREATE TYPE ESTADOS as ENUM ('ACTIVO','DESHABILITADO','INACTIVO');

CREATE TYPE ROLES as ENUM ('ADMIN','STAFF','USUARIO');

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    nombre_completo VARCHAR(200) NOT NULL,
    email VARCHAR(100) unique not null,
    dni INTEGER unique not null,
    estado_cuenta ESTADOS not null,
    rol_usuario ROLES not null,
    qr_code VARCHAR(18) not null unique,
    password_hash VARCHAR not null,
    created_at TIMESTAMPTZ not null default CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ not null default CURRENT_TIMESTAMP
);
