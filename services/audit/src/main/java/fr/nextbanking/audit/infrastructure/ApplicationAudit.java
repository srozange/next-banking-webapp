package fr.nextbanking.audit.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Point d'entrée du service audit ; le scan couvre toute la racine du service.
 */
@SpringBootApplication(scanBasePackages = "fr.nextbanking.audit")
public class ApplicationAudit {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationAudit.class, args);
    }

    @Bean
    static ControleMotDePasseBase controleMotDePasseBase() {
        return new ControleMotDePasseBase();
    }
}
