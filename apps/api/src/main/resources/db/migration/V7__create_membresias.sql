
CREATE TABLE membresias (
    id UUID PRIMARY KEY,
    socio_id UUID NOT NULL UNIQUE,
    nivel_membresia_id UUID NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha_alta DATE NOT NULL,
    fecha_baja DATE,
    proximo_vencimiento DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_membresia_socio FOREIGN KEY (socio_id) REFERENCES socios(id),
    CONSTRAINT fk_membresia_nivel FOREIGN KEY (nivel_membresia_id) REFERENCES nivel_membresias(id)
);
