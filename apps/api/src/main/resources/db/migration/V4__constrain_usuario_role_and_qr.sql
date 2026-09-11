ALTER TABLE usuarios
    ADD CONSTRAINT ck_usuarios_rol
        CHECK (rol IN ('ADMIN', 'STAFF', 'USUARIO')),
    ADD CONSTRAINT ck_usuarios_qr_formato
        CHECK (qr_usuario ~ '^SERA-U[a-z0-9]{12}$'),
    ADD CONSTRAINT uk_usuarios_qr_usuario UNIQUE (qr_usuario);
