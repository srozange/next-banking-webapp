package fr.nextbanking.openbankingconseiller.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Point d'entrée du service open-banking-conseiller ; le scan couvre toute la racine du service.
 */
@SpringBootApplication(scanBasePackages = "fr.nextbanking.openbankingconseiller")
public class ApplicationOpenBankingConseiller {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationOpenBankingConseiller.class, args);
    }

    @Bean
    static ControleMotDePasseBase controleMotDePasseBase() {
        return new ControleMotDePasseBase();
    }
}
