package pe.confianza.mis.usuarios.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.confianza.mis.usuarios.domain.entity.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findByRolId(UUID rolId);
    boolean existsByRolId(UUID rolId);

    @Query("""
           select u from Usuario u
           where (:q is null
                  or lower(u.nombre) like lower(concat('%', cast(:q as string), '%'))
                  or lower(u.email)  like lower(concat('%', cast(:q as string), '%')))
             and (:activo is null or u.activo = :activo)
           """)
    Page<Usuario> buscar(String q, Boolean activo, Pageable pageable);
}
