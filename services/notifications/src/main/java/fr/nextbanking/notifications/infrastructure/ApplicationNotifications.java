package fr.nextbanking.notifications.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Point d'entrée du service notifications ; le scan couvre toute la racine du service.
 */
@SpringBootApplication(scanBasePackages = "fr.nextbanking.notifications")
public class ApplicationNotifications {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationNotifications.class, args);
    }

    @Bean
    static ControleMotDePasseBase controleMotDePasseBase() {
        return new ControleMotDePasseBase();
    }
}
