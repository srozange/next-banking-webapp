package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/** Évaluations de test : par défaut un virement sans aucun signal (10:00 Paris, bénéficiaire actif, FR, rien d'inhabituel). */
final class ControleFraudeOutils {

    static final ParametresControleFraude PARAMETRES = new ParametresControleFraude(75, new BigDecimal("5000"),
            new BigDecimal("15000"), 8, 30, 60, 5, new BigDecimal("500"));
    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final Instant DIX_HEURES = Instant.parse("2026-11-17T09:00:00Z");
    static final Iban FRANCE = new Iban("FR7630006000011234567890189");
    static final HistoriqueClient VIDE = new HistoriqueClient(0, null, 0);

    private ControleFraudeOutils() {
    }

    static Evaluation evaluer(String montant, Iban destinataire, boolean beneficiaire, HistoriqueClient historique,
            Instant maintenant) {
        return ControleFraude.evaluer(PARAMETRES, new BigDecimal(montant), destinataire, beneficiaire, historique,
                maintenant, PARIS);
    }

    static Evaluation evaluerMontant(String montant) {
        return evaluer(montant, FRANCE, true, VIDE, DIX_HEURES);
    }

    static Evaluation evaluerHistorique(HistoriqueClient historique) {
        return evaluer("100.00", FRANCE, true, historique, DIX_HEURES);
    }

    static Optional<Signal> signal(Evaluation evaluation, TypeSignal type) {
        return evaluation.signaux().stream().filter(s -> s.type() == type).findFirst();
    }
}
