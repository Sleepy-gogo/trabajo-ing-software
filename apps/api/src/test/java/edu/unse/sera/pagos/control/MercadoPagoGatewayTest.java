package edu.unse.sera.pagos.control;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MercadoPagoGatewayTest {
  @Test
  void validaFirmaOficialYRechazaIdentificadorAlterado() throws Exception {
    String manifest = "id:123;request-id:request-1;ts:1704908010;";
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec("secreto".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature =
        "ts=1704908010,v1="
            + HexFormat.of().formatHex(hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    var gateway = new MercadoPagoGateway("", "secreto", "", new ObjectMapper());

    assertThat(gateway.firmaValida(signature, "request-1", "123")).isTrue();
    assertThat(gateway.firmaValida(signature, "request-1", "999")).isFalse();
    assertThat(gateway.firmaValida(null, "request-1", "123")).isFalse();
  }
}
