CREATE TABLE disponibilidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_espacio UUID,
    dia_semana VARCHAR(10) NOT NULL,
    hora_desde TIME NOT NULL,
    hora_hasta TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disponibilidad_espacio FOREIGN KEY (id_espacio) REFERENCES espacios(id)
);
