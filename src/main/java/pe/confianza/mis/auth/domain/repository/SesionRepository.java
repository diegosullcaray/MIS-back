package pe.confianza.mis.auth.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.confianza.mis.auth.domain.entity.Sesion;

import java.util.UUID;

public interface SesionRepository extends JpaRepository<Sesion, UUID> {
}
