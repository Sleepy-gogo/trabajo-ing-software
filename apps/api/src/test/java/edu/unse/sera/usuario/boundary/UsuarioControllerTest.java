package edu.unse.sera.usuario.boundary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unse.sera.shared.config.SecurityConfig;
import edu.unse.sera.usuario.control.PasswordInvalidaException;
import edu.unse.sera.usuario.control.UsuarioDetalle;
import edu.unse.sera.usuario.control.UsuarioDuplicadoException;
import edu.unse.sera.usuario.control.UsuarioNoEncontradoException;
import edu.unse.sera.usuario.control.UsuarioService;
import edu.unse.sera.usuario.entity.EstadoUsuario;
import edu.unse.sera.usuario.entity.RolUsuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

  private final MockMvc mockMvc;

  @MockitoBean private UsuarioService usuarioService;

  @Autowired
  UsuarioControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void creaUnUsuarioSinExponerElPassword() throws Exception {
    UUID id = UUID.randomUUID();
    when(usuarioService.registrarUsuario(
            eq("Ada Lovelace"),
            eq("ada@example.com"),
            eq(12345678),
            eq(RolUsuario.USUARIO),
            eq("password-seguro")))
        .thenReturn(detalle(id));

    mockMvc
        .perform(
            post("/api/usuarios")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombreCompleto": "Ada Lovelace",
                      "email": "ada@example.com",
                      "dni": 12345678,
                      "rol": "USUARIO",
                      "password": "password-seguro"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/usuarios/" + id))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.rol").value("USUARIO"))
        .andExpect(jsonPath("$.qrUsuario").value("SERA-U0123456789ab"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void rechazaDatosInvalidos() throws Exception {
    mockMvc
        .perform(
            post("/api/usuarios")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombreCompleto": " ",
                      "email": "no-es-email",
                      "dni": 0,
                      "rol": null,
                      "password": "corta"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("datos_invalidos"))
        .andExpect(jsonPath("$.campos.nombreCompleto").exists())
        .andExpect(jsonPath("$.campos.email").exists())
        .andExpect(jsonPath("$.campos.password").exists());
  }

  @Test
  void rechazaUnRolFueraDelEnum() throws Exception {
    mockMvc
        .perform(
            post("/api/usuarios")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombreCompleto": "Ada Lovelace",
                      "email": "ada@example.com",
                      "dni": 12345678,
                      "rol": "SUPERVISOR",
                      "password": "password-seguro"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void informaUnEmailDuplicadoComoConflicto() throws Exception {
    when(usuarioService.registrarUsuario(any(), any(), anyInt(), any(), any()))
        .thenThrow(new UsuarioDuplicadoException("email"));

    mockMvc
        .perform(
            post("/api/usuarios")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "nombreCompleto": "Ada Lovelace",
                      "email": "ada@example.com",
                      "dni": 12345678,
                      "rol": "USUARIO",
                      "password": "password-seguro"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("usuario_duplicado"))
        .andExpect(jsonPath("$.campos.email").exists());
  }

  @Test
  void buscaUsuariosPorNombre() throws Exception {
    UUID id = UUID.randomUUID();
    when(usuarioService.listar("ada")).thenReturn(List.of(detalle(id)));

    mockMvc
        .perform(
            get("/api/usuarios").with(user("admin").roles("ADMIN")).queryParam("buscar", "ada"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].nombreCompleto").value("Ada Lovelace"));
  }

  @Test
  void daDeBajaUnUsuario() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/usuarios/{id}", id).with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isNoContent());

    verify(usuarioService).darDeBaja(id);
  }

  @Test
  void consultaUnUsuarioSinExponerCredenciales() throws Exception {
    UUID id = UUID.randomUUID();
    when(usuarioService.consultarDetalle(id)).thenReturn(detalle(id));

    mockMvc
        .perform(get("/api/usuarios/{id}", id).with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void consultaInexistenteResponde404() throws Exception {
    UUID id = UUID.randomUUID();
    when(usuarioService.consultarDetalle(id)).thenThrow(new UsuarioNoEncontradoException(id));

    mockMvc
        .perform(get("/api/usuarios/{id}", id).with(user("admin").roles("ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("recurso_no_encontrado"));
  }

  @Test
  void actualizaLosDatosDelUsuario() throws Exception {
    UUID id = UUID.randomUUID();
    when(usuarioService.actualizarUsuario(
            id,
            "Ada Lovelace",
            "ada@example.com",
            12345678,
            RolUsuario.USUARIO,
            EstadoUsuario.ACTIVO))
        .thenReturn(detalle(id));

    mockMvc
        .perform(
            put("/api/usuarios/{id}", id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
          {"nombreCompleto":"Ada Lovelace", "email":"ada@example.com", "dni":12345678,
           "rol":"USUARIO", "estadoCuenta":"ACTIVO"}
          """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
    verify(usuarioService)
        .actualizarUsuario(
            id,
            "Ada Lovelace",
            "ada@example.com",
            12345678,
            RolUsuario.USUARIO,
            EstadoUsuario.ACTIVO);
  }

  @Test
  void cambiaLaPasswordSinDevolverla() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(
            put("/api/usuarios/{id}/password", id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"password-nueva\"}"))
        .andExpect(status().isNoContent());
    verify(usuarioService).actualizarPasswordUsuario(id, "password-nueva");
  }

  @Test
  void passwordQueSuperaElLimiteEnBytesResponde400() throws Exception {
    UUID id = UUID.randomUUID();
    String password = "á".repeat(37);
    org.mockito.Mockito.doThrow(new PasswordInvalidaException())
        .when(usuarioService)
        .actualizarPasswordUsuario(id, password);

    mockMvc
        .perform(
            put("/api/usuarios/{id}/password", id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"" + password + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("datos_invalidos"))
        .andExpect(
            jsonPath("$.campos.password")
                .value("La contraseña supera los 72 bytes. Usá menos caracteres."));
  }

  private UsuarioDetalle detalle(UUID id) {
    OffsetDateTime ahora = OffsetDateTime.parse("2026-09-10T20:00:00Z");
    return new UsuarioDetalle(
        id,
        "Ada Lovelace",
        "ada@example.com",
        12345678,
        RolUsuario.USUARIO,
        "SERA-U0123456789ab",
        EstadoUsuario.ACTIVO,
        ahora,
        ahora);
  }
}
