package pe.confianza.mis.roles.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.roles.domain.entity.Rol;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {
    Optional<Rol> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("select count(r) from Rol r join r.sistemaIds sid where sid = :sistemaId")
    long contarConSistema(UUID sistemaId);
}
