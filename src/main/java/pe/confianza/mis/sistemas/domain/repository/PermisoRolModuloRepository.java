package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.sistemas.domain.entity.PermisoRolModulo;

import java.util.List;
import java.util.UUID;

public interface PermisoRolModuloRepository
        extends JpaRepository<PermisoRolModulo, PermisoRolModulo.Id> {

    List<PermisoRolModulo> findByRolId(UUID rolId);

    @Query("""
           select p from PermisoRolModulo p
           where p.moduloId in (
               select m.id from Modulo m where m.subseccion.seccion.sistema.id = :sistemaId)
           """)
    List<PermisoRolModulo> findBySistema(UUID sistemaId);

    @Query("""
           select p.moduloId from PermisoRolModulo p
           where p.rolId = :rolId and p.moduloId in (
               select m.id from Modulo m where m.subseccion.seccion.sistema.id = :sistemaId)
           """)
    List<UUID> modulosDeRolEnSistema(UUID rolId, UUID sistemaId);

    void deleteByRolIdAndModuloIdIn(UUID rolId, List<UUID> moduloIds);
}
