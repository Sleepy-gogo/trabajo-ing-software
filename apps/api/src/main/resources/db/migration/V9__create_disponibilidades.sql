CREATE TABLE disponibilidades (
    id UUID PRIMARY KEY,
    id_espacio UUID NOT NULL,
    dia_semana VARCHAR(10) NOT NULL,
    hora_desde TIME NOT NULL,
    hora_hasta TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disponibilidades_espacio
        FOREIGN KEY (id_espacio) REFERENCES espacios(id) ON DELETE CASCADE,
    CONSTRAINT ck_disponibilidades_dia_semana
        CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    CONSTRAINT ck_disponibilidades_rango CHECK (hora_desde < hora_hasta)
);
