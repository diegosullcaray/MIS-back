package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.sistemas.domain.entity.PermisoRolSeccion;

import java.util.List;
import java.util.UUID;

public interface PermisoRolSeccionRepository
        extends JpaRepository<PermisoRolSeccion, PermisoRolSeccion.Id> {

    List<PermisoRolSeccion> findByRolId(UUID rolId);

    @Query("""
           select p from PermisoRolSeccion p
           where p.seccionId in (
               select sec.id from Seccion sec where sec.sistema.id = :sistemaId)
           """)
    List<PermisoRolSeccion> findBySistema(UUID sistemaId);

    @Query("""
           select p.seccionId from PermisoRolSeccion p
           where p.rolId = :rolId and p.seccionId in (
               select sec.id from Seccion sec where sec.sistema.id = :sistemaId)
           """)
    List<UUID> seccionesDeRolEnSistema(UUID rolId, UUID sistemaId);

    void deleteByRolIdAndSeccionIdIn(UUID rolId, List<UUID> seccionIds);
}
