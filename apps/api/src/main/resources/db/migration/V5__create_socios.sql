CREATE TABLE socios (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE,
    relacion_unse VARCHAR(50) NOT NULL,
    estado_verificacion_unse VARCHAR(50) NOT NULL,
    identificador_unse VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_socio_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
