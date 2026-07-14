package pe.confianza.mis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * MIS Host — Backend (monolito modular: core · auth · usuarios · roles · sistemas).
 * Contrato en .docs/Backend/04_BACKEND_SCHEMA.md · DDL en .docs/Backend/07_DATABASE_SCHEMA.sql
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MisBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MisBackendApplication.class, args);
    }
}
