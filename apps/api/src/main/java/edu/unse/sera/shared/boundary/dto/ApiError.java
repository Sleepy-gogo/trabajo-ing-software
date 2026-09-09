package edu.unse.sera.shared.boundary.dto;

import java.util.Map;

public record ApiError(String codigo, String mensaje, Map<String, String> campos) {}
