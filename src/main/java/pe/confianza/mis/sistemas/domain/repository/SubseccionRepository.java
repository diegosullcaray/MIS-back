package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.sistemas.domain.entity.Subseccion;

import java.util.List;
import java.util.UUID;

public interface SubseccionRepository extends JpaRepository<Subseccion, UUID> {

    @Query("select sub.id from Subseccion sub where sub.seccion.sistema.id = :sistemaId")
    List<UUID> idsPorSistema(UUID sistemaId);

    /** [subseccionId, sistemaId] para agrupar permisos por sistema. */
    @Query("select sub.id, sub.seccion.sistema.id from Subseccion sub where sub.id in :ids")
    List<Object[]> sistemaPorSubseccion(List<UUID> ids);
}
