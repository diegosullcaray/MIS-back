package pe.confianza.mis.core.exception;

import org.springframework.http.HttpStatus;

/** Base de las excepciones de negocio traducidas por GlobalExceptionHandler (BE-04). */
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
