package pe.confianza.mis.usuarios.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.confianza.mis.usuarios.domain.entity.Credencial;

import java.util.Optional;
import java.util.UUID;

public interface CredencialRepository extends JpaRepository<Credencial, UUID> {
    Optional<Credencial> findByUsuarioId(UUID usuarioId);
}
