CREATE TABLE informes (
    id UUID PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    creado_por UUID NOT NULL REFERENCES usuarios(id),
    contenido TEXT NOT NULL
);
CREATE INDEX ix_informes_fecha ON informes(creado_en DESC);

CREATE TABLE encuestas (
    id UUID PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    espacio_id UUID REFERENCES espacios(id),
    desde DATE NOT NULL,
    hasta DATE NOT NULL,
    activa BOOLEAN NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL,
    CHECK (desde <= hasta)
);
CREATE TABLE preguntas_encuesta (
    id UUID PRIMARY KEY,
    encuesta_id UUID NOT NULL REFERENCES encuestas(id),
    texto VARCHAR(500) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CALIFICACION','OPCION','TEXTO')),
    obligatoria BOOLEAN NOT NULL,
    orden INTEGER NOT NULL,
    UNIQUE(encuesta_id, orden)
);
CREATE TABLE opciones_pregunta (
    pregunta_id UUID NOT NULL REFERENCES preguntas_encuesta(id),
    orden INTEGER NOT NULL,
    valor VARCHAR(200) NOT NULL,
    PRIMARY KEY(pregunta_id, orden)
);
CREATE TABLE envios_encuesta (
    id UUID PRIMARY KEY,
    encuesta_id UUID NOT NULL REFERENCES encuestas(id),
    reserva_id UUID NOT NULL REFERENCES reservas(id),
    enviada_en TIMESTAMPTZ NOT NULL,
    UNIQUE(encuesta_id, reserva_id)
);
CREATE TABLE respuestas_encuesta (
    envio_id UUID NOT NULL REFERENCES envios_encuesta(id),
    pregunta_id UUID NOT NULL REFERENCES preguntas_encuesta(id),
    valor VARCHAR(2000) NOT NULL,
    PRIMARY KEY(envio_id, pregunta_id)
);
CREATE INDEX ix_envios_encuesta ON envios_encuesta(encuesta_id, enviada_en DESC);
