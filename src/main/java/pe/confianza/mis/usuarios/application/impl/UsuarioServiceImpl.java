package pe.confianza.mis.usuarios.application.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.core.exception.ConflictException;
import pe.confianza.mis.core.exception.NotFoundException;
import pe.confianza.mis.core.exception.ValidationException;
import pe.confianza.mis.roles.application.service.RolLookup;
import pe.confianza.mis.sistemas.application.service.SistemaLookup;
import pe.confianza.mis.usuarios.application.service.UsuarioService;
import pe.confianza.mis.usuarios.domain.entity.Credencial;
import pe.confianza.mis.usuarios.domain.entity.Usuario;
import pe.confianza.mis.usuarios.domain.repository.CredencialRepository;
import pe.confianza.mis.usuarios.domain.repository.UsuarioRepository;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.UsuarioDto;
import pe.confianza.mis.usuarios.presentation.dto.UsuarioDtos.UsuarioRequest;
import pe.confianza.mis.usuarios.presentation.mapper.UsuarioMapper;

import java.util.*;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarios;
    private final CredencialRepository credenciales;
    private final RolLookup rolLookup;
    private final SistemaLookup sistemaLookup;
    private final PasswordEncoder encoder;
    private final UsuarioMapper mapper;

    public UsuarioServiceImpl(UsuarioRepository usuarios, CredencialRepository credenciales,
                              RolLookup rolLookup, SistemaLookup sistemaLookup,
                              PasswordEncoder encoder, UsuarioMapper mapper) {
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.rolLookup = rolLookup;
        this.sistemaLookup = sistemaLookup;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioDto> listar(String q, Boolean activo, int page, int pageSize) {
        Page<Usuario> res = usuarios.buscar(
                (q == null || q.isBlank()) ? null : q, activo,
                PageRequest.of(Math.max(page - 1, 0), pageSize));
        return res.map(this::aDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto obtener(UUID id) {
        return aDto(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDto> usuariosDeRol(UUID rolId) {
        return usuarios.findByRolId(rolId).stream().map(this::aDto).toList();
    }

    @Override
    public UsuarioDto crear(UsuarioRequest req) {
        if (usuarios.existsByEmailIgnoreCase(req.email().trim()))
            throw new ConflictException("Ya existe un usuario con el email '" + req.email() + "'.");
        if (req.password() == null || req.password().length() < 6)
            throw new ValidationException("La contraseña es requerida (mín. 6 caracteres).");
        validarRol(req.rolId());

        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setRolId(req.rolId());
        u.setActivo(true);
        u.setSistemaIds(resolver(req.subsistemas()));
        Usuario guardado = usuarios.save(u);

        credenciales.save(new Credencial(guardado.getId(), encoder.encode(req.password())));
        return aDto(guardado);
    }

    @Override
    public UsuarioDto actualizar(UUID id, UsuarioRequest req) {
        Usuario u = buscar(id);
        validarRol(req.rolId());
        u.setNombre(req.nombre().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setRolId(req.rolId());
        u.setSistemaIds(resolver(req.subsistemas()));

        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 6)
                throw new ValidationException("La contraseña debe tener mín. 6 caracteres.");
            credenciales.findByUsuarioId(id)
                    .ifPresentOrElse(
                            c -> c.setPasswordHash(encoder.encode(req.password())),
                            () -> credenciales.save(new Credencial(id, encoder.encode(req.password()))));
        }
        return aDto(u);
    }

    @Override
    public UsuarioDto cambiarEstado(UUID id, boolean activo) {
        Usuario u = buscar(id);
        u.setActivo(activo);
        return aDto(u);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Usuario buscar(UUID id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new NotFoundException("El usuario '" + id + "' no existe."));
    }

    private void validarRol(UUID rolId) {
        if (!rolLookup.existe(rolId))
            throw new ValidationException("El rol '" + rolId + "' no existe.");
    }

    private Set<UUID> resolver(List<String> slugs) {
        return new HashSet<>(sistemaLookup.idsPorSlugs(slugs == null ? List.of() : slugs));
    }

    private UsuarioDto aDto(Usuario u) {
        String rolSlug = rolLookup.porId(u.getRolId())
                .map(RolLookup.RolRef::slug).orElse(null);
        Map<UUID, String> slugs = sistemaLookup.slugsPorIds(u.getSistemaIds());
        List<String> subsistemas = u.getSistemaIds().stream()
                .map(slugs::get).filter(Objects::nonNull).sorted().toList();
        return mapper.toDto(u, rolSlug, subsistemas);
    }
}
