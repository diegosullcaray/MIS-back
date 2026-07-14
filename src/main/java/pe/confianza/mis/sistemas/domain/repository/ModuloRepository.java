package pe.confianza.mis.sistemas.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.sistemas.domain.entity.Modulo;

import java.util.List;
import java.util.UUID;

public interface ModuloRepository extends JpaRepository<Modulo, UUID> {

    @Query("select m.id from Modulo m where m.subseccion.seccion.sistema.id = :sistemaId")
    List<UUID> idsPorSistema(UUID sistemaId);

    /** [moduloId, sistemaId] para agrupar permisos por sistema. */
    @Query("select m.id, m.subseccion.seccion.sistema.id from Modulo m where m.id in :ids")
    List<Object[]> sistemaPorModulo(List<UUID> ids);
}
