package fr.nextbanking.identiteclients.adapter.out.motdepasse;

import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : vérification par {@code DelegatingPasswordEncoder} (empreintes {@code {bcrypt}}).
 * Un client inconnu est comparé à une empreinte factice, calculée au démarrage sur une valeur aléatoire,
 * pour que la durée de la réponse ne trahisse pas l'existence du compte.
 */
@Component
public class MotsDePasseSpring implements MotsDePasse {

    private static final Logger JOURNAL = LoggerFactory.getLogger(MotsDePasseSpring.class);

    private final PasswordEncoder encodeur;
    private final String empreinteFactice;

    public MotsDePasseSpring() {
        this(PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    MotsDePasseSpring(PasswordEncoder encodeur) {
        this.encodeur = encodeur;
        this.empreinteFactice = encodeur.encode(UUID.randomUUID().toString());
    }

    @Override
    public boolean correspond(String motDePasse, Optional<String> empreinte) {
        if (empreinte.isEmpty()) {
            // Même coût qu'une vraie vérification ; le résultat est ignoré.
            encodeur.matches(motDePasse, empreinteFactice);
            return false;
        }
        try {
            return encodeur.matches(motDePasse, empreinte.get());
        } catch (IllegalArgumentException e) {
            // Empreinte enregistrée sans préfixe d'algorithme reconnu : refus, sans rien journaliser du secret.
            JOURNAL.warn("Empreinte de mot de passe illisible : connexion refusée");
            return false;
        }
    }
}
