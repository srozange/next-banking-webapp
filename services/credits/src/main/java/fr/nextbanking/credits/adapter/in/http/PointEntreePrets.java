package fr.nextbanking.credits.adapter.in.http;

import fr.nextbanking.credits.application.port.in.ConsulterEcheances;
import fr.nextbanking.credits.application.port.in.ConsulterPret;
import fr.nextbanking.credits.application.port.in.DeposerDemandePret;
import fr.nextbanking.credits.application.port.in.ListerPrets;
import fr.nextbanking.credits.application.port.in.PayerEcheance;
import fr.nextbanking.credits.application.port.in.SupprimerPret;
import fr.nextbanking.credits.domain.Echeance;
import fr.nextbanking.credits.domain.Pret;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : liste, consultation et dépôt de demande de prêts du client désigné par le sujet du jeton, tout rôle confondu. Un sujet qui n'est pas
 * un identifiant numérique donne 401.
 */
@RestController
public class PointEntreePrets {

    private final ListerPrets listerPrets;
    private final ConsulterPret consulterPret;
    private final DeposerDemandePret deposerDemandePret;
    private final ConsulterEcheances consulterEcheances;
    private final PayerEcheance payerEcheance;
    private final SupprimerPret supprimerPret;

    public PointEntreePrets(ListerPrets listerPrets, ConsulterPret consulterPret,
            DeposerDemandePret deposerDemandePret, ConsulterEcheances consulterEcheances,
            PayerEcheance payerEcheance, SupprimerPret supprimerPret) {
        this.consulterEcheances = consulterEcheances;
        this.payerEcheance = payerEcheance;
        this.supprimerPret = supprimerPret;
        this.listerPrets = listerPrets;
        this.consulterPret = consulterPret;
        this.deposerDemandePret = deposerDemandePret;
    }

    @GetMapping("/api/credits/prets")
    public List<PretJson> lister(@AuthenticationPrincipal Jwt jeton) {
        return listerPrets.lister(clientId(jeton)).stream().map(PretJson::de).toList();
    }

    @GetMapping("/api/credits/prets/{pretId}")
    public PretJson consulter(@AuthenticationPrincipal Jwt jeton, @PathVariable long pretId) {
        return PretJson.de(consulterPret.consulter(clientId(jeton), pretId));
    }

    @PostMapping("/api/credits/prets")
    @ResponseStatus(HttpStatus.CREATED)
    public PretJson deposer(@AuthenticationPrincipal Jwt jeton, @RequestBody DemandePretJson demande) {
        return PretJson.de(deposerDemandePret.deposer(clientId(jeton), demande.type(), demande.montant(),
                demande.tauxAnnuel(), demande.dureeMois(), demande.objet()));
    }

    @GetMapping("/api/credits/prets/{pretId}/echeances")
    public List<EcheanceJson> echeances(@AuthenticationPrincipal Jwt jeton, @PathVariable long pretId) {
        return consulterEcheances.consulter(clientId(jeton), pretId).stream().map(EcheanceJson::de).toList();
    }

    @PostMapping("/api/credits/echeances/{echeanceId}/paiement")
    public EcheanceJson payer(@AuthenticationPrincipal Jwt jeton, @PathVariable long echeanceId) {
        return EcheanceJson.de(payerEcheance.payer(clientId(jeton), echeanceId));
    }

    @DeleteMapping("/api/credits/prets/{pretId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal Jwt jeton, @PathVariable long pretId) {
        supprimerPret.supprimer(clientId(jeton), pretId);
    }

    private static long clientId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    /** Corps de la demande : champs saisis, en chaînes. */
    public record DemandePretJson(String type, String montant, String tauxAnnuel, String dureeMois, String objet) {
    }

    /** Prêt : champs en français, montants en chaînes à 2 décimales, dates {@code yyyy-MM-dd}. */
    public record PretJson(long id, String type, String montant, String tauxAnnuel, int dureeMois, String mensualite,
            String coutTotal, String totalInterets, String objet, String statut, String dateDebut, String dateFin) {

        static PretJson de(Pret pret) {
            return new PretJson(pret.id(), pret.type() == null ? null : pret.type().name(), pret.montant().toPlainString(),
                    pret.tauxAnnuel().toPlainString(), pret.dureeMois(), pret.mensualite().toPlainString(),
                    pret.coutTotal().toPlainString(), pret.totalInterets().toPlainString(), pret.objet(),
                    pret.statut().name(), pret.dateDebut().toString(), pret.dateFin().toString());
        }
    }

    /** Échéance : montants en chaînes à 2 décimales, date {@code yyyy-MM-dd}, paiement en instant ISO UTC ou null. */
    public record EcheanceJson(long id, int numero, String dateEcheance, String capital, String interets, String total,
            String capitalRestant, boolean paye, String datePaiement) {

        static EcheanceJson de(Echeance echeance) {
            return new EcheanceJson(echeance.id(), echeance.numero(), echeance.dateEcheance().toString(),
                    echeance.capital().toPlainString(), echeance.interets().toPlainString(),
                    echeance.total().toPlainString(), echeance.capitalRestant().toPlainString(), echeance.paye(),
                    echeance.datePaiement() == null ? null : echeance.datePaiement().toString());
        }
    }
}
