package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.domain.ConnexionRefusee;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code POST /api/identite-clients/connexion} (chemin public). Rend le jeton émis ;
 * les refus sont rendus par {@link GestionErreurs}. L'adresse IP d'origine est résolue ici, une seule fois, par
 * {@link ResolveurAdresseIp}, puis passée au cas d'usage. Le journal ne porte ni le mot de passe ni le jeton.
 */
@RestController
public class PointEntreeConnexion {

    static final String CHEMIN = "/api/identite-clients/connexion";

    private static final Logger JOURNAL = LoggerFactory.getLogger(PointEntreeConnexion.class);

    private final SeConnecter seConnecter;
    private final ResolveurAdresseIp resolveurAdresseIp;

    public PointEntreeConnexion(SeConnecter seConnecter, ResolveurAdresseIp resolveurAdresseIp) {
        this.seConnecter = seConnecter;
        this.resolveurAdresseIp = resolveurAdresseIp;
    }

    @PostMapping(CHEMIN)
    public JetonEmis seConnecter(@RequestBody DemandeConnexion demande, HttpServletRequest requete) {
        try {
            SeConnecter.Connexion connexion = seConnecter.seConnecter(demande.identifiant(), demande.motDePasse(),
                    resolveurAdresseIp.resoudre(requete));
            JOURNAL.info("Connexion réussie (client {})", connexion.clientId());
            return new JetonEmis(connexion.jeton());
        } catch (ConnexionRefusee refus) {
            JOURNAL.info("Connexion refusée ({})", refus.motif());
            throw refus;
        }
    }

    /** Corps de la demande ; le mot de passe n'apparaît jamais dans {@link #toString()}. */
    public record DemandeConnexion(String identifiant, String motDePasse) {

        @Override
        public String toString() {
            return "DemandeConnexion[identifiant=" + identifiant + ", motDePasse=****]";
        }
    }

    /** Corps de la réponse ; le jeton n'apparaît jamais dans {@link #toString()}. */
    public record JetonEmis(String jeton) {

        @Override
        public String toString() {
            return "JetonEmis[jeton=****]";
        }
    }
}
