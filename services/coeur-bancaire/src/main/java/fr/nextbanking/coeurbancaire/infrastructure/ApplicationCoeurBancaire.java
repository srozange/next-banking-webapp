package fr.nextbanking.coeurbancaire.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Point d'entrée du service coeur-bancaire ; le scan couvre toute la racine du service.
 */
@SpringBootApplication(scanBasePackages = "fr.nextbanking.coeurbancaire")
public class ApplicationCoeurBancaire {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationCoeurBancaire.class, args);
    }

    @Bean
    static ControleMotDePasseBase controleMotDePasseBase() {
        return new ControleMotDePasseBase();
    }
}
