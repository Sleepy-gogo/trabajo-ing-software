package edu.unse.sera.usuario.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.disponibilidad.boundary.DisponibilidadController;
import edu.unse.sera.disponibilidad.control.DisponibilidadService;
import edu.unse.sera.espacio.boundary.EspacioController;
import edu.unse.sera.espacio.control.EspacioService;
import edu.unse.sera.shared.config.SecurityConfig;
import edu.unse.sera.socio.entity.RelacionUnse;
import edu.unse.sera.usuario.boundary.dto.LoginRequest;
import edu.unse.sera.usuario.control.UsuarioDetalle;
import edu.unse.sera.usuario.control.UsuarioService;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  AuthController.class,
  UsuarioController.class,
  EspacioController.class,
  DisponibilidadController.class
})
@Import(SecurityConfig.class)
class AuthControllerTest {
  private final MockMvc mvc;

  @Autowired
  AuthControllerTest(MockMvc mvc) {
    this.mvc = mvc;
  }

  @MockitoBean private UsuarioService usuarios;
  @MockitoBean private EspacioService espacios;
  @MockitoBean private DisponibilidadService disponibilidades;

  @Test
  void exigeSesionParaConsultarUsuarios() throws Exception {
    mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    verifyNoInteractions(usuarios);
  }

  @Test
  void exigeCsrfEnRegistro() throws Exception {
    mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(usuarios);
  }

  @Test
  void registroPublicoNoPuedeElegirRol() throws Exception {
    when(usuarios.registrarUsuario(any(), any(), anyInt(), eq(RolUsuario.USUARIO), any()))
        .thenReturn(detalle(RolUsuario.USUARIO));
    mvc.perform(
            post("/api/auth/registro")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"nombreCompleto":"Ada", "email":"ada@example.com", "dni":12345678,
             "password":"password-seguro", "rol":"ADMIN"}
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("USUARIO"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    verify(usuarios)
        .registrarUsuario(
            "Ada", "ada@example.com", 12345678, RolUsuario.USUARIO, "password-seguro");
  }

  @Test
  void loginRechazaCredencialesInvalidas() throws Exception {
    when(usuarios.autenticar(any(), any())).thenReturn(Optional.empty());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@example.com\",\"password\":\"incorrecta\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.codigo").value("credenciales_invalidas"));
  }

  @Test
  void loginRotaSesionYLogoutImpideReutilizarla() throws Exception {
    var usuario = detalle(RolUsuario.USUARIO);
    when(usuarios.autenticar(any(), any())).thenReturn(Optional.of(usuario));
    when(usuarios.consultarActivo(usuario.id())).thenReturn(Optional.of(usuario));
    when(usuarios.consultarDetalle(usuario.id())).thenReturn(usuario);
    var anterior = new MockHttpSession();
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .session(anterior)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"ada@example.com\",\"password\":\"password-seguro\"}"))
            .andExpect(status().isOk())
            .andReturn();
    var session = (MockHttpSession) result.getRequest().getSession(false);
    org.junit.jupiter.api.Assertions.assertTrue(anterior.isInvalid());
    org.junit.jupiter.api.Assertions.assertNotEquals(anterior.getId(), session.getId());
    mvc.perform(get("/api/usuarios/me").session(session)).andExpect(status().isOk());
    mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void usuarioYStaffNoPuedenAdministrar() throws Exception {
    for (var rol : new RolUsuario[] {RolUsuario.USUARIO, RolUsuario.STAFF}) {
      var usuario = detalle(rol);
      var session = sesion(usuario);
      mvc.perform(get("/api/usuarios").session(session)).andExpect(status().isForbidden());
      mvc.perform(
              post("/api/usuarios")
                  .session(session)
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isForbidden());
      mvc.perform(delete("/api/usuarios/" + UUID.randomUUID()).session(session).with(csrf()))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void administradorPuedeListarYLaBajaRevocaSesion() throws Exception {
    var usuario = detalle(RolUsuario.ADMIN);
    var session = sesion(usuario);
    mvc.perform(get("/api/usuarios").session(session)).andExpect(status().isOk());
    when(usuarios.consultarActivo(usuario.id())).thenReturn(Optional.empty());
    mvc.perform(get("/api/usuarios").session(session)).andExpect(status().isUnauthorized());
    org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
  }

  @Test
  void usuarioPuedeConsultarEspaciosPeroNoAdministrarlos() throws Exception {
    var usuario = detalle(RolUsuario.USUARIO);
    var session = sesion(usuario);
    when(espacios.listar(null)).thenReturn(List.of());

    mvc.perform(get("/api/espacios").session(session)).andExpect(status().isOk());
    mvc.perform(
            post("/api/espacios")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());

    verify(espacios).listar(null);
  }

  @Test
  void perfilUsaIdentidadDeSesionEIgnoraRolYEstadoDelCliente() throws Exception {
    var usuario = detalle(RolUsuario.USUARIO);
    var session = sesion(usuario);
    when(usuarios.actualizarPerfil(eq(usuario.id()), any(), any(), anyInt())).thenReturn(usuario);
    mvc.perform(
            put("/api/usuarios/me")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
            {"nombreCompleto":"Ada", "email":"ada@example.com", "dni":12345678,
             "rol":"ADMIN", "estadoCuenta":"ACTIVO", "id":"00000000-0000-0000-0000-000000000000"}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rol").value("USUARIO"));
    verify(usuarios).actualizarPerfil(usuario.id(), "Ada", "ada@example.com", 12345678);
  }

  @Test
  void obtieneTokenCsrfAntesDeIniciarSesion() throws Exception {
    mvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    verifyNoInteractions(usuarios);
  }

  @Test
  void registroPublicoConservaRelacionUnseSinPermitirRolAdministrativo() throws Exception {
    when(usuarios.registrarUsuario(
            any(),
            any(),
            anyInt(),
            eq(RolUsuario.USUARIO),
            any(),
            eq(RelacionUnse.ESTUDIANTE),
            eq("123")))
        .thenReturn(detalle(RolUsuario.USUARIO));

    mvc.perform(
            post("/api/auth/registro")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
          {"nombreCompleto":"Ada", "email":"ada@example.com", "dni":12345678,
           "password":"password-seguro", "relacionUnse":"ESTUDIANTE",
           "identificadorUnse":"123", "rol":"ADMIN"}
          """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("USUARIO"));
    verify(usuarios)
        .registrarUsuario(
            "Ada",
            "ada@example.com",
            12345678,
            RolUsuario.USUARIO,
            "password-seguro",
            RelacionUnse.ESTUDIANTE,
            "123");
  }

  @Test
  void loginSinSesionAnteriorCreaUnaNuevaConLaIdentidadAutenticada() {
    var usuario = detalle(RolUsuario.USUARIO);
    when(usuarios.autenticar("ada@example.com", "password-seguro"))
        .thenReturn(Optional.of(usuario));
    var request = new MockHttpServletRequest();

    var respuesta =
        new AuthController(usuarios)
            .login(new LoginRequest("ada@example.com", "password-seguro"), request);

    assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
    assertThat(request.getSession(false).getAttribute(SesionFilter.USUARIO_ID))
        .isEqualTo(usuario.id());
  }

  @Test
  void logoutSinSesionEsIdempotente() {
    var request = new MockHttpServletRequest();
    assertThat(new AuthController(usuarios).logout(request).getStatusCode().value()).isEqualTo(204);
    assertThat(request.getSession(false)).isNull();
    verifyNoInteractions(usuarios);
  }

  @Test
  void rechazaLoginInvalidoAntesDeAutenticar() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"sin-arroba\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.email").exists())
        .andExpect(jsonPath("$.campos.password").exists());
    verifyNoInteractions(usuarios);
  }

  @Test
  void cambioDeRolSeAplicaEnLaSiguientePeticion() throws Exception {
    var admin = detalle(RolUsuario.ADMIN);
    var session = sesion(admin);
    mvc.perform(get("/api/usuarios").session(session)).andExpect(status().isOk());
    var usuario =
        new UsuarioDetalle(
            admin.id(),
            admin.nombreCompleto(),
            admin.email(),
            admin.dni(),
            RolUsuario.USUARIO,
            admin.qrUsuario(),
            admin.estadoCuenta(),
            admin.creadoEn(),
            admin.actualizadoEn());
    when(usuarios.consultarActivo(admin.id())).thenReturn(Optional.of(usuario));

    mvc.perform(get("/api/usuarios").session(session)).andExpect(status().isForbidden());
    assertThat(session.isInvalid()).isFalse();
  }

  @Test
  void unaSesionSinIdentidadValidaNoAutoriza() throws Exception {
    var session = new MockHttpSession();
    session.setAttribute(SesionFilter.USUARIO_ID, "id-incorrecto");

    mvc.perform(get("/api/usuarios/me").session(session)).andExpect(status().isUnauthorized());
    verifyNoInteractions(usuarios);
  }

  private MockHttpSession sesion(UsuarioDetalle usuario) {
    var session = new MockHttpSession();
    session.setAttribute(SesionFilter.USUARIO_ID, usuario.id());
    when(usuarios.consultarActivo(usuario.id())).thenReturn(Optional.of(usuario));
    return session;
  }

  private UsuarioDetalle detalle(RolUsuario rol) {
    var ahora = OffsetDateTime.now();
    return new UsuarioDetalle(
        UUID.randomUUID(),
        "Ada",
        "ada@example.com",
        12345678,
        rol,
        "SERA-U0123456789ab",
        EstadoUsuario.ACTIVO,
        ahora,
        ahora);
  }
}
