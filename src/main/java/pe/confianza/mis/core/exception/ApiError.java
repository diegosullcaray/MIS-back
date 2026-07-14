package pe.confianza.mis.core.exception;

import java.time.Instant;

/** Formato de error estándar del API (doc 04 §3.1) — el que ya espera el frontend. */
public record ApiError(int status, String message, Instant timestamp) {
    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now());
    }
}
