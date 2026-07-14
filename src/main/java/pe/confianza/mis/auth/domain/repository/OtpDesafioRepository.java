package pe.confianza.mis.auth.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.confianza.mis.auth.domain.entity.OtpDesafio;

import java.util.UUID;

public interface OtpDesafioRepository extends JpaRepository<OtpDesafio, UUID> {
}
