package edu.unse.sera.encuesta.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "envios_encuesta")
public class EnvioEncuesta {
  @Id private UUID id = UUID.randomUUID();
  private UUID encuestaId;
  private UUID reservaId;
  private OffsetDateTime enviadaEn = OffsetDateTime.now(java.time.ZoneOffset.UTC);

  @ElementCollection
  @CollectionTable(name = "respuestas_encuesta", joinColumns = @JoinColumn(name = "envio_id"))
  @MapKeyColumn(name = "pregunta_id")
  @Column(name = "valor", length = 2000)
  private Map<UUID, String> respuestas = new LinkedHashMap<>();

  protected EnvioEncuesta() {}

  public EnvioEncuesta(UUID encuestaId, UUID reservaId, Map<UUID, String> respuestas) {
    this.encuestaId = encuestaId;
    this.reservaId = reservaId;
    this.respuestas = new LinkedHashMap<>(respuestas);
  }

  public UUID getId() {
    return id;
  }

  public UUID getReservaId() {
    return reservaId;
  }

  public UUID getEncuestaId() {
    return encuestaId;
  }

  public OffsetDateTime getEnviadaEn() {
    return enviadaEn;
  }

  public Map<UUID, String> getRespuestas() {
    return Map.copyOf(respuestas);
  }
}
