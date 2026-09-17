CREATE TABLE nivel_membresias (
    id UUID PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    descripcion VARCHAR(500),
    disponible BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 4. Tabla nivel_membresia_precio_relacion (Depende de nivel_membresias)
CREATE TABLE nivel_membresia_precio_relacion (
    nivel_membresia_id UUID NOT NULL,
    relacion VARCHAR(50) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (nivel_membresia_id, relacion),
    CONSTRAINT fk_precio_nivel FOREIGN KEY (nivel_membresia_id) REFERENCES nivel_membresias(id) ON DELETE CASCADE
);

-- 5. Tabla nivel_membresia_beneficios (Depende de nivel_membresias)
CREATE TABLE nivel_membresia_beneficios (
    nivel_membresia_id UUID NOT NULL,
    beneficio VARCHAR(255) NOT NULL,
    CONSTRAINT fk_beneficio_nivel FOREIGN KEY (nivel_membresia_id) REFERENCES nivel_membresias(id) ON DELETE CASCADE
);
