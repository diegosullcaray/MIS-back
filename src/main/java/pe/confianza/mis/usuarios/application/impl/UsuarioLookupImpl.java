package pe.confianza.mis.usuarios.application.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.usuarios.application.service.UsuarioLookup;
import pe.confianza.mis.usuarios.domain.repository.UsuarioRepository;

import java.util.UUID;

/**
 * Implementa el puerto {@link UsuarioLookup} que `usuarios` ofrece a `roles`.
 * Solo depende del repositorio para no formar ciclos de beans con RolServiceImpl.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioLookupImpl implements UsuarioLookup {

    private final UsuarioRepository usuarios;

    public UsuarioLookupImpl(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public boolean algunoConRol(UUID rolId) {
        return usuarios.existsByRolId(rolId);
    }
}
