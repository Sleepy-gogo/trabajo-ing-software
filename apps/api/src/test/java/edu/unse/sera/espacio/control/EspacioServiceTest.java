package edu.unse.sera.espacio.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unse.sera.espacio.entity.Espacio;
import edu.unse.sera.espacio.persistence.EspacioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EspacioServiceTest {

  @Mock private EspacioRepository espacioRepository;

  private EspacioService espacioService;

  @BeforeEach
  void setUp() {
    espacioService = new EspacioService(espacioRepository);
  }

  @Test
  void creaUnEspacioNormalizandoLosTextos() {
    when(espacioRepository.save(any(Espacio.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EspacioDetalle response = espacioService.crear("  Cancha cubierta  ", "  Piso de parquet  ");

    assertThat(response.id()).isNotNull();
    assertThat(response.nombre()).isEqualTo("Cancha cubierta");
    assertThat(response.descripcion()).isEqualTo("Piso de parquet");
    verify(espacioRepository).save(any(Espacio.class));
  }

  @Test
  void informaCuandoElEspacioNoExiste() {
    UUID id = UUID.randomUUID();
    when(espacioRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> espacioService.obtener(id))
        .isInstanceOf(EspacioNoEncontradoException.class)
        .hasMessageContaining(id.toString());
  }
}
