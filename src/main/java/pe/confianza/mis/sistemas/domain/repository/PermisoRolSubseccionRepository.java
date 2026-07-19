package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.sistemas.domain.entity.PermisoRolSubseccion;

import java.util.List;
import java.util.UUID;

public interface PermisoRolSubseccionRepository
        extends JpaRepository<PermisoRolSubseccion, PermisoRolSubseccion.Id> {

    List<PermisoRolSubseccion> findByRolId(UUID rolId);

    @Query("""
           select p from PermisoRolSubseccion p
           where p.subseccionId in (
               select sub.id from Subseccion sub where sub.seccion.sistema.id = :sistemaId)
           """)
    List<PermisoRolSubseccion> findBySistema(UUID sistemaId);

    @Query("""
           select p.subseccionId from PermisoRolSubseccion p
           where p.rolId = :rolId and p.subseccionId in (
               select sub.id from Subseccion sub where sub.seccion.sistema.id = :sistemaId)
           """)
    List<UUID> subseccionesDeRolEnSistema(UUID rolId, UUID sistemaId);

    void deleteByRolIdAndSubseccionIdIn(UUID rolId, List<UUID> subseccionIds);
}
