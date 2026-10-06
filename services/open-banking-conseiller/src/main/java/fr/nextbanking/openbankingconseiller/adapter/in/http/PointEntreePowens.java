package fr.nextbanking.openbankingconseiller.adapter.in.http;

import fr.nextbanking.openbankingconseiller.application.port.in.ConnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.TraiterRetourPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.DeconnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.RafraichirComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.in.SelectionnerCompteExterne;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.SituationPowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de l'intégration Powens, ouvert à tout utilisateur authentifié (FR-012-05, FR-012-17). L'état
 * est celui du client du jeton ({@code sub}). Ni le jeton Powens ni le secret client ne sortent jamais.
 */
@RestController
public class PointEntreePowens {

    static final String CHEMIN = "/api/open-banking-conseiller/powens";
    static final String CHEMIN_CONNEXION = CHEMIN + "/connexion";
    static final String CHEMIN_RETOUR = CHEMIN + "/retour";
    static final String CHEMIN_SELECTION = CHEMIN + "/comptes/{compteId}/selection";
    static final String CHEMIN_RAFRAICHISSEMENT = CHEMIN + "/rafraichissement";
    static final String DESTINATION_RETOUR = "/comptes-tiers";

    private final ConnecterPowens connecter;
    private final ConsulterEtatPowens consulter;
    private final TraiterRetourPowens traiterRetour;
    private final SelectionnerCompteExterne selectionner;
    private final RafraichirComptesExternes rafraichir;
    private final DeconnecterPowens deconnecter;

    public PointEntreePowens(ConnecterPowens connecter, ConsulterEtatPowens consulter, TraiterRetourPowens retour,
            SelectionnerCompteExterne selectionner, RafraichirComptesExternes rafraichir,
            DeconnecterPowens deconnecter) {
        this.connecter = connecter;
        this.consulter = consulter;
        this.traiterRetour = retour;
        this.selectionner = selectionner;
        this.rafraichir = rafraichir;
        this.deconnecter = deconnecter;
    }

    /**
     * Retour public de la page web de Powens (chemin exact seul, sans jeton d'accès) : répond toujours 302 vers
     * « Comptes Tiers », sans corps ; le {@code state} signé seul retrouve le client.
     */
    @GetMapping(CHEMIN_RETOUR)
    public ResponseEntity<Void> retour(@RequestParam Map<String, String> parametres) {
        traiterRetour.traiter(new LinkedHashMap<>(parametres));
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, DESTINATION_RETOUR).build();
    }

    @PostMapping(CHEMIN_CONNEXION)
    public ReponseConnexion connexion(@AuthenticationPrincipal Jwt jeton) {
        return new ReponseConnexion(connecter.connecter(jeton.getSubject()));
    }

    @GetMapping(CHEMIN)
    public ReponseEtat etat(@AuthenticationPrincipal Jwt jeton) {
        return ReponseEtat.depuis(consulter.consulter(jeton.getSubject()));
    }

    @PostMapping(CHEMIN_SELECTION)
    public ReponseEtat selection(@AuthenticationPrincipal Jwt jeton, @PathVariable long compteId) {
        return ReponseEtat.depuis(selectionner.selectionner(jeton.getSubject(), compteId));
    }

    @PostMapping(CHEMIN_RAFRAICHISSEMENT)
    public ReponseEtat rafraichissement(@AuthenticationPrincipal Jwt jeton) {
        return ReponseEtat.depuis(rafraichir.rafraichir(jeton.getSubject()));
    }

    @DeleteMapping(CHEMIN_CONNEXION)
    public ResponseEntity<Void> deconnexion(@AuthenticationPrincipal Jwt jeton) {
        deconnecter.deconnecter(jeton.getSubject());
        return ResponseEntity.noContent().build();
    }

    /** Adresse de la page web de Powens. */
    public record ReponseConnexion(String adresse) {
    }

    /** Situation Powens du client ; jamais le jeton. Montants en chaîne à 2 décimales, solde absent distinct de zéro. */
    public record ReponseEtat(boolean connecte, List<ReponseCompte> comptes, Long compteSelectionne,
            List<ReponseTransaction> transactions, String message, String patrimoineTotal) {

        static ReponseEtat depuis(SituationPowens situation) {
            return new ReponseEtat(situation.etat().connecte(),
                    situation.comptes().stream().map(ReponseCompte::depuis).toList(), situation.selectionne(),
                    situation.transactions().stream().map(ReponseTransaction::depuis).toList(),
                    situation.etat().message(), enChaine(situation.patrimoineTotal()));
        }
    }

    /** Compte externe : {@code solde} nul et {@code soldeAbsent} vrai quand Powens ne donne aucun solde. */
    public record ReponseCompte(long id, String nom, String type, String solde, boolean soldeAbsent, String iban) {

        static ReponseCompte depuis(CompteExterne compte) {
            return new ReponseCompte(compte.id(), compte.nom(), compte.type(),
                    compte.solde() == null ? null : enChaine(compte.solde()), compte.solde() == null, compte.iban());
        }
    }

    /** Transaction du compte sélectionné. */
    public record ReponseTransaction(String date, String libelle, String montant) {

        static ReponseTransaction depuis(TransactionExterne transaction) {
            return new ReponseTransaction(transaction.date().toString(), transaction.libelle(),
                    enChaine(transaction.montant()));
        }
    }

    private static String enChaine(BigDecimal valeur) {
        return valeur.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
