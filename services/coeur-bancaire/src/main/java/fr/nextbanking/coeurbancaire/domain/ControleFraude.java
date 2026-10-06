package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Évaluation des huit signaux de fraude d'un virement (FEAT-027), en Java pur, sans accès aux données : l'historique
 * est lu par l'application avant l'appel. Les scores s'additionnent ; au-delà de {@code scoreCumuleMax}, une alerte
 * par signal levé est à écrire. Le contrôle ne refuse rien ici (suspension : story 3.4).
 */
public final class ControleFraude {

    static final int SCORE_MONTANT_ELEVE = 40;
    static final int SCORE_MONTANT_TRES_ELEVE = 80;
    static final int SCORE_FREQUENCE = 50;
    static final int SCORE_NOUVEAU_BENEFICIAIRE = 35;
    static final int SCORE_HEURE_INHABITUELLE = 45;
    static final int SCORE_MONTANT_ROND = 20;
    static final int SCORE_IBAN_ETRANGER = 30;
    static final int SCORE_SUCCESSION_RAPIDE = 55;
    static final int SCORE_VELOCITE = 65;

    /** Heure d'ouverture : avant 06:00, l'heure est inhabituelle. */
    static final int HEURE_OUVERTURE = 6;
    /** Heure de fermeture : à partir de 23:00, l'heure est inhabituelle. */
    static final int HEURE_FERMETURE = 23;
    static final BigDecimal MULTIPLE_ROND = new BigDecimal("1000");
    static final String PREFIXE_FRANCE = "FR";

    /** Signaux levés, somme de leurs scores et décision d'écrire les alertes. */
    public record Evaluation(List<Signal> signaux, int score, boolean alerter) {

        public Evaluation {
            signaux = List.copyOf(signaux);
        }

        /** Une alerte OPEN par signal levé si la somme dépasse le seuil ; aucune sinon. */
        public List<AlerteFraude> alertes(long clientId, long virementId, Instant creeLe) {
            if (!alerter) {
                return List.of();
            }
            return signaux.stream().map(signal -> new AlerteFraude(clientId, virementId, signal.type(),
                    signal.score(), signal.description(), StatutAlerte.OPEN, creeLe)).toList();
        }
    }

    private ControleFraude() {
    }

    /**
     * Évalue les huit signaux, dans l'ordre de {@link TypeSignal}.
     *
     * @param beneficiaireActif le destinataire est un bénéficiaire actif du client
     * @param maintenant instant de la soumission (port Horloge) ; {@code fuseau} : fuseau métier (Horloge)
     */
    public static Evaluation evaluer(ParametresControleFraude parametres, BigDecimal montant, Iban destinataire,
            boolean beneficiaireActif, HistoriqueClient historique, Instant maintenant, ZoneId fuseau) {
        List<Signal> signaux = new ArrayList<>();
        montantEleve(parametres, montant, signaux);
        if (historique.operationsDuJour() > parametres.plafondQuotidien()) {
            signaux.add(new Signal(TypeSignal.HIGH_FREQUENCY, SCORE_FREQUENCE, TextesVirement
                    .descriptionFrequence(historique.operationsDuJour(), parametres.plafondQuotidien())));
        }
        if (!beneficiaireActif) {
            signaux.add(new Signal(TypeSignal.NEW_BENEFICIARY, SCORE_NOUVEAU_BENEFICIAIRE,
                    TextesVirement.descriptionNouveauBeneficiaire(destinataire.enClair())));
        }
        int heure = maintenant.atZone(fuseau).getHour();
        if (heure < HEURE_OUVERTURE || heure >= HEURE_FERMETURE) {
            signaux.add(new Signal(TypeSignal.UNUSUAL_HOURS, SCORE_HEURE_INHABITUELLE,
                    TextesVirement.descriptionHeureInhabituelle(heure)));
        }
        if (montant.remainder(MULTIPLE_ROND).signum() == 0) {
            signaux.add(new Signal(TypeSignal.ROUND_AMOUNT, SCORE_MONTANT_ROND,
                    TextesVirement.descriptionMontantRond(montant.toPlainString())));
        }
        if (!destinataire.commencePar(PREFIXE_FRANCE)) {
            signaux.add(new Signal(TypeSignal.FOREIGN_IBAN, SCORE_IBAN_ETRANGER,
                    TextesVirement.descriptionIbanEtranger(destinataire.enClair())));
        }
        successionRapide(parametres, historique, maintenant, signaux);
        if (historique.virementsImportants() >= parametres.velociteNombreMax()) {
            signaux.add(new Signal(TypeSignal.VELOCITY_CHECK, SCORE_VELOCITE, TextesVirement.descriptionVelocite(
                    historique.virementsImportants(), parametres.velociteFenetreMinutes(),
                    parametres.velociteNombreMax())));
        }
        int score = signaux.stream().mapToInt(Signal::score).sum();
        return new Evaluation(signaux, score, score > parametres.scoreCumuleMax());
    }

    /** Un seul signal LARGE_AMOUNT par virement : 80 au seuil très élevé, sinon 40 au seuil élevé (A-003-4). */
    private static void montantEleve(ParametresControleFraude parametres, BigDecimal montant, List<Signal> signaux) {
        if (montant.compareTo(parametres.montantTresEleve()) >= 0) {
            signaux.add(new Signal(TypeSignal.LARGE_AMOUNT, SCORE_MONTANT_TRES_ELEVE,
                    TextesVirement.descriptionMontantTresEleve(montant.toPlainString(),
                            parametres.montantTresEleve().toPlainString())));
        } else if (montant.compareTo(parametres.montantEleve()) >= 0) {
            signaux.add(new Signal(TypeSignal.LARGE_AMOUNT, SCORE_MONTANT_ELEVE,
                    TextesVirement.descriptionMontantEleve(montant.toPlainString(),
                            parametres.montantEleve().toPlainString())));
        }
    }

    /** RAPID_SUCCESSION : compare au précédent virement enregistré du client (A-003-5). */
    private static void successionRapide(ParametresControleFraude parametres, HistoriqueClient historique,
            Instant maintenant, List<Signal> signaux) {
        if (historique.dernierVirement() == null) {
            return;
        }
        long ecart = Duration.between(historique.dernierVirement(), maintenant).getSeconds();
        if (ecart < parametres.successionRapideSecondes()) {
            signaux.add(new Signal(TypeSignal.RAPID_SUCCESSION, SCORE_SUCCESSION_RAPIDE,
                    TextesVirement.descriptionSuccessionRapide(ecart, parametres.successionRapideSecondes())));
        }
    }
}
