package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.unse.sera.pagos.entity.SuscripcionMercadoPago;
import edu.unse.sera.pagos.persistence.SuscripcionMercadoPagoRepository;
import edu.unse.sera.shared.exception.OperacionNoPermitidaException;
import edu.unse.sera.usuario.entity.RolUsuario;
import edu.unse.sera.usuario.entity.Usuario;
import edu.unse.sera.usuario.persistence.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConciliacionPagoServiceTest {
  @Mock private SuscripcionMercadoPagoRepository suscripciones;
  @Mock private UsuarioRepository usuarios;
  @Mock private MercadoPagoGateway mercadoPago;
  @Mock private SuscripcionMercadoPagoService procesador;
  @Mock private SuscripcionMercadoPago suscripcion;
  @Mock private Usuario admin;
  private ConciliacionPagoService service;

  @BeforeEach
  void preparar() {
    service = new ConciliacionPagoService(suscripciones, usuarios, mercadoPago, procesador);
  }

  @Test
  void recorreTodasLasFacturasDeLaSuscripcionYReusaElProcesadorVerificado() {
    UUID actor = UUID.randomUUID();
    UUID membresia = UUID.randomUUID();
    when(usuarios.findById(actor)).thenReturn(Optional.of(admin));
    when(admin.getRol()).thenReturn(RolUsuario.ADMIN);
    when(suscripciones.findAllByMembresiaIdOrderByCreatedAtDesc(membresia))
        .thenReturn(List.of(suscripcion));
    when(suscripcion.getPreapprovalId()).thenReturn("suscripcion-1");
    when(mercadoPago.buscarFacturas("suscripcion-1", 0))
        .thenReturn(new MercadoPagoGateway.PaginaFacturas(List.of(11L, 12L), 4, 2));
    when(mercadoPago.buscarFacturas("suscripcion-1", 2))
        .thenReturn(new MercadoPagoGateway.PaginaFacturas(List.of(), 4, 1));
    when(mercadoPago.buscarFacturas("suscripcion-1", 3))
        .thenReturn(new MercadoPagoGateway.PaginaFacturas(List.of(13L), 4, 1));

    assertThat(service.conciliar(membresia, actor)).isEqualTo(3);
    verify(procesador).recibirFactura(11L);
    verify(procesador).recibirFactura(12L);
    verify(procesador).recibirFactura(13L);
  }

  @Test
  void soloUnAdministradorPuedeConciliar() {
    UUID actor = UUID.randomUUID();
    when(usuarios.findById(actor)).thenReturn(Optional.of(admin));
    when(admin.getRol()).thenReturn(RolUsuario.USUARIO);

    assertThatThrownBy(() -> service.conciliar(UUID.randomUUID(), actor))
        .isInstanceOf(OperacionNoPermitidaException.class);
    verifyNoInteractions(mercadoPago, procesador);
  }
}
