package pe.confianza.mis.usuarios.application.service;

import java.util.UUID;

/**
 * Puerto público del módulo `usuarios` para que `roles` verifique asignaciones
 * sin acoplarse al dominio de usuarios (BE-01).
 */
public interface UsuarioLookup {

    /** ¿Existe al menos un usuario con el rol indicado? */
    boolean algunoConRol(UUID rolId);
}
