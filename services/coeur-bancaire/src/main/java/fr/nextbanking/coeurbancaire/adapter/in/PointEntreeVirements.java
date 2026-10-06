package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterComptes;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterSuiviVirements;
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
    static final String CHEMIN_SUIVI = "/api/coeur-bancaire/virements/suivi";
    static final String CHEMIN_COMPTES = "/api/coeur-bancaire/comptes";
    static final String EN_TETE_IDEMPOTENCE = "Idempotency-Key";

    private final ExecuterVirement executerVirement;
    private final ConsulterComptes consulterComptes;
    private final ResolveurAdresseIp resolveurAdresseIp;
    private final ConsulterSuiviVirements consulterSuivi;
    private final EnrichissementTrace trace;

    public PointEntreeVirements(ExecuterVirement executerVirement, ConsulterComptes consulterComptes,
            ResolveurAdresseIp resolveurAdresseIp, ConsulterSuiviVirements consulterSuivi,
            EnrichissementTrace trace) {
        this.executerVirement = executerVirement;
        this.consulterComptes = consulterComptes;
        this.resolveurAdresseIp = resolveurAdresseIp;
        this.consulterSuivi = consulterSuivi;
        this.trace = trace;
    }

    @PostMapping(CHEMIN_VIREMENTS)
    public ResponseEntity<VirementExecute> executer(@AuthenticationPrincipal Jwt jeton,
            @RequestHeader(value = EN_TETE_IDEMPOTENCE, required = false) String cleIdempotence,
            @RequestBody DemandeVirement demande, HttpServletRequest requete) {
        Resultat resultat = executerVirement.executer(new Commande(clientId(jeton), demande.compteSourceId(),
                demande.compteDestinataireId(), demande.montant(), demande.motif(),
                cleIdempotence == null || cleIdempotence.isBlank() ? null : cleIdempotence.strip(),
                resolveurAdresseIp.resoudre(requete)));
        trace.issue(resultat.etat().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new VirementExecute(resultat.virementId(),
                resultat.etat().name(), resultat.heureButoir() == null ? null : resultat.heureButoir().toString(),
                resultat.statut().name(), resultat.compteSourceId(), resultat.compteDestinataireId(),
                resultat.montant().toPlainString(), resultat.motif()));
    }

    /** Virements passés en revue du client authentifié : les quatre plus récents, avec leur état. */
    @GetMapping(CHEMIN_SUIVI)
    public List<VirementSuivi> suivi(@AuthenticationPrincipal Jwt jeton) {
        return consulterSuivi.suivi(clientId(jeton)).stream()
                .map(ligne -> new VirementSuivi(ligne.virementId(), ligne.soumisLe().toString(),
                        ligne.numeroDestinataire(), ligne.montant().toPlainString(), ligne.etat().name(),
                        ligne.heureButoir().toString()))
                .toList();
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

    /**
     * Corps de la réponse 201 : état, heure butoir (ISO-8601 UTC, SUSPENDU seulement), statut d'affichage et écho des
     * champs saisis ({@code motif} nul si vide).
     */
    public record VirementExecute(long virementId, String etat, String heureButoir, String statut, long compteSourceId,
            long compteDestinataireId, String montant, String motif) {
    }

    /** Virement passé en revue vu par son client ; {@code heureButoir} : heure butoir de la revue (ISO-8601 UTC). */
    public record VirementSuivi(long virementId, String soumisLe, String compteDestinataire, String montant,
            String etat, String heureButoir) {
    }

    /** Compte du client, sans solde. */
    public record CompteClient(long id, String type, String numero) {
    }
}
