package edu.unse.sera.shared.boundary;

import edu.unse.sera.shared.boundary.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/api/health")
  public HealthResponse health() {
    return new HealthResponse("ok");
  }
}
