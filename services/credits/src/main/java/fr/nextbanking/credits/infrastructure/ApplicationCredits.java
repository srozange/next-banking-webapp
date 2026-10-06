package fr.nextbanking.credits.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Point d'entrée du service credits ; le scan couvre toute la racine du service.
 */
@SpringBootApplication(scanBasePackages = "fr.nextbanking.credits")
public class ApplicationCredits {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationCredits.class, args);
    }

    @Bean
    static ControleMotDePasseBase controleMotDePasseBase() {
        return new ControleMotDePasseBase();
    }
}
