package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterComptes;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Resultat;
import fr.nextbanking.coeurbancaire.domain.Compte;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code POST /api/coeur-bancaire/virements} (virement immédiat) et
 * {@code GET /api/coeur-bancaire/comptes} (comptes du client authentifié). Le client est celui du jeton ({@code sub}) ;
 * la propriété des comptes est contrôlée par le service (NFR-SEC-03). L'adresse IP d'origine est résolue ici, une
 * seule fois, et passée au cas d'usage. Montants en chaîne décimale à point.
 */
@RestController
public class PointEntreeVirements {

    static final String CHEMIN_VIREMENTS = "/api/coeur-bancaire/virements";
    static final String CHEMIN_COMPTES = "/api/coeur-bancaire/comptes";
    static final String EN_TETE_IDEMPOTENCE = "Idempotency-Key";

    private final ExecuterVirement executerVirement;
    private final ConsulterComptes consulterComptes;
    private final ResolveurAdresseIp resolveurAdresseIp;

    public PointEntreeVirements(ExecuterVirement executerVirement, ConsulterComptes consulterComptes,
            ResolveurAdresseIp resolveurAdresseIp) {
        this.executerVirement = executerVirement;
        this.consulterComptes = consulterComptes;
        this.resolveurAdresseIp = resolveurAdresseIp;
    }

    @PostMapping(CHEMIN_VIREMENTS)
    public ResponseEntity<VirementExecute> executer(@AuthenticationPrincipal Jwt jeton,
            @RequestHeader(value = EN_TETE_IDEMPOTENCE, required = false) String cleIdempotence,
            @RequestBody DemandeVirement demande, HttpServletRequest requete) {
        Resultat resultat = executerVirement.executer(new Commande(clientId(jeton), demande.compteSourceId(),
                demande.compteDestinataireId(), demande.montant(), demande.motif(),
                cleIdempotence == null || cleIdempotence.isBlank() ? null : cleIdempotence.strip(),
                resolveurAdresseIp.resoudre(requete)));
        return ResponseEntity.status(HttpStatus.CREATED).body(new VirementExecute(resultat.virementId(),
                resultat.etat().name(), resultat.statut().name(), resultat.compteSourceId(),
                resultat.compteDestinataireId(), resultat.montant().toPlainString(), resultat.motif()));
    }

    @GetMapping(CHEMIN_COMPTES)
    public List<CompteClient> comptes(@AuthenticationPrincipal Jwt jeton) {
        return consulterComptes.comptesDuClient(clientId(jeton)).stream().map(PointEntreeVirements::compte).toList();
    }

    private static long clientId(Jwt jeton) {
        return Long.parseLong(jeton.getSubject());
    }

    private static CompteClient compte(Compte compte) {
        return new CompteClient(compte.id(), compte.type().name(), compte.numero());
    }

    /** Corps de la demande : les champs absents sont contrôlés par le domaine. */
    public record DemandeVirement(Long compteSourceId, Long compteDestinataireId, String montant, String motif) {
    }

    /** Corps de la réponse 201 : état, statut d'affichage et écho des champs saisis ({@code motif} nul si vide). */
    public record VirementExecute(long virementId, String etat, String statut, long compteSourceId,
            long compteDestinataireId, String montant, String motif) {
    }

    /** Compte du client, sans solde. */
    public record CompteClient(long id, String type, String numero) {
    }
}
