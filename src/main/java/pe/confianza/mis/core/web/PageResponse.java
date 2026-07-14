package pe.confianza.mis.core.web;

import org.springframework.data.domain.Page;

import java.util.List;

/** Respuesta paginada (doc 04 §3.2) — coincide con `PageResponse<T>` del frontend. */
public record PageResponse<T>(int page, int pageSize, long total, List<T> items) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getNumber() + 1,   // el frontend pagina desde 1
                page.getSize(),
                page.getTotalElements(),
                page.getContent());
    }
}
