package edu.unse.sera.shared.config;

import edu.unse.sera.usuario.boundary.SesionFilter;
import edu.unse.sera.usuario.control.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioService usuarios)
      throws Exception {
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/health", "/api/auth/csrf", "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro")
                    .permitAll()
                    .requestMatchers("/api/usuarios/me")
                    .authenticated()
                    .requestMatchers("/api/usuarios", "/api/usuarios/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/auth/logout")
                    .authenticated()
                    .requestMatchers("/api/espacios", "/api/espacios/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(new SesionFilter(usuarios), AnonymousAuthenticationFilter.class)
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          response.setStatus(401);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"codigo\":\"sesion_requerida\","
                                      + "\"mensaje\":\"Iniciá sesión para continuar.\",\"campos\":{}}");
                        })
                    .accessDeniedHandler(
                        (request, response, exception) -> {
                          response.setStatus(403);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"codigo\":\"acceso_denegado\","
                                      + "\"mensaje\":\"No se pudo autorizar la operación. "
                                      + "Actualizá la página e intentá de nuevo.\",\"campos\":{}}");
                        }))
        .build();
  }
}
