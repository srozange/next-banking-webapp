package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterRevue;
import fr.nextbanking.coeurbancaire.application.port.in.IssueDecision;
import fr.nextbanking.coeurbancaire.application.port.in.LibererVirement;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterVirement;
import fr.nextbanking.coeurbancaire.domain.LigneRevue;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de la revue des virements suspendus : file, historique des virements traités, libération et
 * rejet, sous {@code /api/coeur-bancaire/revue/}. Le jeton ne porte aucun rôle par chemin : le rôle ADMIN est vérifié
 * par le service (403 sinon, D6). L'adresse IP d'origine est résolue ici, une seule fois.
 */
@RestController
public class PointEntreeRevue {

    static final String CHEMIN_FILE = "/api/coeur-bancaire/revue/virements-suspendus";
    static final String CHEMIN_HISTORIQUE = "/api/coeur-bancaire/revue/virements-traites";
    static final String CHEMIN_LIBERER = "/api/coeur-bancaire/revue/virements/{virementId}/liberer";
    static final String CHEMIN_REJETER = "/api/coeur-bancaire/revue/virements/{virementId}/rejeter";

    private final ConsulterRevue consulterRevue;
    private final LibererVirement libererVirement;
    private final RejeterVirement rejeterVirement;
    private final ResolveurAdresseIp resolveurAdresseIp;

    public PointEntreeRevue(ConsulterRevue consulterRevue, LibererVirement libererVirement,
            RejeterVirement rejeterVirement, ResolveurAdresseIp resolveurAdresseIp) {
        this.consulterRevue = consulterRevue;
        this.libererVirement = libererVirement;
        this.rejeterVirement = rejeterVirement;
        this.resolveurAdresseIp = resolveurAdresseIp;
    }

    @GetMapping(CHEMIN_FILE)
    public List<VirementSuspendu> file(@AuthenticationPrincipal Jwt jeton) {
        return consulterRevue.fileDeRevue(JetonRoles.depuis(jeton)).stream().map(PointEntreeRevue::suspendu).toList();
    }

    @GetMapping(CHEMIN_HISTORIQUE)
    public List<VirementTraite> historique(@AuthenticationPrincipal Jwt jeton) {
        return consulterRevue.historique(JetonRoles.depuis(jeton)).stream()
                .map(traite -> new VirementTraite(suspendu(traite.ligne()), traite.issue().name(),
                        traite.dateIssue().toString()))
                .toList();
    }

    @PostMapping(CHEMIN_LIBERER)
    public ResponseEntity<Object> liberer(@AuthenticationPrincipal Jwt jeton, @PathVariable long virementId,
            @RequestBody DemandeDecision demande, HttpServletRequest requete) {
        return reponse(virementId, libererVirement.liberer(commande(jeton, virementId, demande, requete)));
    }

    @PostMapping(CHEMIN_REJETER)
    public ResponseEntity<Object> rejeter(@AuthenticationPrincipal Jwt jeton, @PathVariable long virementId,
            @RequestBody DemandeDecision demande, HttpServletRequest requete) {
        return reponse(virementId, rejeterVirement.rejeter(commande(jeton, virementId, demande, requete)));
    }

    private CommandeDecision commande(Jwt jeton, long virementId, DemandeDecision demande, HttpServletRequest requete) {
        return new CommandeDecision(Long.parseLong(jeton.getSubject()), JetonRoles.depuis(jeton), virementId,
                demande.motif(), resolveurAdresseIp.resoudre(requete));
    }

    /** Décision acceptée : 200 ; refusée avec rejet sans débit : 409 et le texte de l'issue. */
    private static ResponseEntity<Object> reponse(long virementId, IssueDecision issue) {
        return switch (issue) {
            case ACCEPTEE -> ResponseEntity.ok(new DecisionEnregistree(virementId, issue.name()));
            case REFUSEE_SOLDE_INSUFFISANT -> refus(issue, TextesVirement.LIBERATION_REFUSEE_SOLDE);
            case REFUSEE_HEURE_BUTOIR -> refus(issue, TextesVirement.HEURE_BUTOIR_DEPASSEE);
        };
    }

    private static ResponseEntity<Object> refus(IssueDecision issue, String texte) {
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body(new DecisionRefusee(texte, issue.name()));
    }

    private static VirementSuspendu suspendu(LigneRevue ligne) {
        return new VirementSuspendu(ligne.virementId(), ligne.clientId(), ligne.nomClient(),
                ligne.montant().toPlainString(), ligne.numeroDestinataire(), ligne.motifSuspension(),
                ligne.heureButoir().toString());
    }

    /** Corps de la décision : le motif, obligatoire (contrôlé par le service). */
    public record DemandeDecision(String motif) {
    }

    /** Décision acceptée. */
    public record DecisionEnregistree(long virementId, String issue) {
    }

    /** Décision refusée, virement rejeté sans débit : texte à afficher et issue. */
    public record DecisionRefusee(String message, String issue) {
    }

    /** Ligne de la file de revue ; {@code nomClient} est nul tant que le client n'est pas dans la projection. */
    public record VirementSuspendu(long virementId, long clientId, String nomClient, String montant,
            String compteDestinataire, String motifSuspension, String heureButoir) {
    }

    /** Ligne de l'historique : la ligne de revue, l'issue et sa date (ISO-8601 UTC). */
    public record VirementTraite(VirementSuspendu virement, String issue, String dateIssue) {
    }
}
