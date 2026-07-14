package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.confianza.mis.sistemas.domain.entity.Sistema;

import java.util.Optional;
import java.util.UUID;

public interface SistemaRepository extends JpaRepository<Sistema, UUID> {
    Optional<Sistema> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
