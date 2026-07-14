package pe.confianza.mis.usuarios.application.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.confianza.mis.roles.application.service.RolLookup;
import pe.confianza.mis.roles.application.service.RolLookup.RolRef;
import pe.confianza.mis.sistemas.application.service.SistemaLookup;
import pe.confianza.mis.usuarios.application.service.PasswordAuthenticator;
import pe.confianza.mis.usuarios.application.service.UsuarioDirectory;
import pe.confianza.mis.usuarios.domain.entity.Credencial;
import pe.confianza.mis.usuarios.domain.entity.Usuario;
import pe.confianza.mis.usuarios.domain.repository.CredencialRepository;
import pe.confianza.mis.usuarios.domain.repository.UsuarioRepository;

import java.time.Duration;
import java.util.*;

/**
 * Implementa los puertos que `usuarios` ofrece a `auth`: directorio de usuarios
 * y autenticación por contraseña con política de lockout.
 */
@Service
@Transactional
public class UsuarioAuthAdapter implements UsuarioDirectory, PasswordAuthenticator {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final UsuarioRepository usuarios;
    private final CredencialRepository credenciales;
    private final RolLookup rolLookup;
    private final SistemaLookup sistemaLookup;
    private final PasswordEncoder encoder;

    public UsuarioAuthAdapter(UsuarioRepository usuarios, CredencialRepository credenciales,
                              RolLookup rolLookup, SistemaLookup sistemaLookup,
                              PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.rolLookup = rolLookup;
        this.sistemaLookup = sistemaLookup;
        this.encoder = encoder;
    }

    // ─── UsuarioDirectory ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> porEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email).map(this::aAuthUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> porId(UUID id) {
        return usuarios.findById(id).map(this::aAuthUser);
    }

    // ─── PasswordAuthenticator (dev) ──────────────────────────────────────────

    @Override
    public UUID autenticar(String email, String rawPassword) {
        Usuario u = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthFallida(AuthFallida.Motivo.CREDENCIALES,
                        "Correo electrónico o contraseña incorrectos."));

        Credencial cred = credenciales.findByUsuarioId(u.getId())
                .orElseThrow(() -> new AuthFallida(AuthFallida.Motivo.CREDENCIALES,
                        "Correo electrónico o contraseña incorrectos."));

        if (cred.estaBloqueada())
            throw new AuthFallida(AuthFallida.Motivo.BLOQUEADA,
                    "Cuenta bloqueada temporalmente por intentos fallidos.");

        if (!encoder.matches(rawPassword, cred.getPasswordHash())) {
            cred.registrarFallo(MAX_INTENTOS, BLOQUEO);
            throw new AuthFallida(AuthFallida.Motivo.CREDENCIALES,
                    "Correo electrónico o contraseña incorrectos.");
        }

        if (!u.isActivo())
            throw new AuthFallida(AuthFallida.Motivo.INACTIVO,
                    "El usuario está desactivado. Contacta al administrador.");

        cred.registrarExito();
        return u.getId();
    }

    // ─── Mapeo ────────────────────────────────────────────────────────────────

    private AuthUser aAuthUser(Usuario u) {
        RolRef rol = rolLookup.porId(u.getRolId())
                .orElseThrow(() -> new IllegalStateException(
                        "El rol '" + u.getRolId() + "' del usuario no existe."));
        // Subsistemas efectivos: override del usuario si existe, si no los del rol.
        Set<UUID> ids = !u.getSistemaIds().isEmpty()
                ? u.getSistemaIds() : rol.sistemaIds();
        Map<UUID, String> slugs = sistemaLookup.slugsPorIds(ids);
        List<String> subsistemas = ids.stream()
                .map(slugs::get).filter(Objects::nonNull).sorted().toList();
        return new AuthUser(u.getId(), u.getNombre(), u.getEmail(), rol.slug(),
                subsistemas, u.isActivo(), u.getCreadoEn());
    }
}
