ALTER TABLE usuarios
    RENAME CONSTRAINT usuarios_email_key TO uk_usuarios_email;

ALTER TABLE usuarios
    RENAME CONSTRAINT usuarios_dni_key TO uk_usuarios_dni;
