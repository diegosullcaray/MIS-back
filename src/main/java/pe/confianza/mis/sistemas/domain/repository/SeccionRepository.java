package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.sistemas.domain.entity.Seccion;

import java.util.List;
import java.util.UUID;

public interface SeccionRepository extends JpaRepository<Seccion, UUID> {

    @Query("select sec.id from Seccion sec where sec.sistema.id = :sistemaId")
    List<UUID> idsPorSistema(UUID sistemaId);

    /** [seccionId, sistemaId] para agrupar permisos por sistema. */
    @Query("select sec.id, sec.sistema.id from Seccion sec where sec.id in :ids")
    List<Object[]> sistemaPorSeccion(List<UUID> ids);
}
