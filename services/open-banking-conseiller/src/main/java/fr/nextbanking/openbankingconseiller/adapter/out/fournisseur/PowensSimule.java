package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import fr.nextbanking.openbankingconseiller.application.PowensIndisponible;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Adaptateur simulé de Powens, actif par défaut : aucun appel réseau, réponses fixes. L'adresse rendue est celle du
 * retour avec un {@code connection_id} fixe et le {@code state}, ce qui permet de parcourir la connexion en local.
 * Les tests choisissent un {@link ModeErreur} (AR-61).
 */
public class PowensSimule implements Powens {

    /** Adresse de retour quand aucune n'est configurée. */
    static final String RETOUR_PAR_DEFAUT = "/api/open-banking-conseiller/powens/retour";
    static final String JETON_FIXE = "jeton-simule-powens";
    static final String CODE_FIXE = "code-simule-powens";
    static final String CONNECTION_ID_FIXE = "1";

    /** Comportements d'erreur sélectionnables par les tests. */
    public enum ModeErreur {
        AUCUNE,
        REFUS_CREATION,
        REPONSE_SANS_JETON,
        COMPTES_500,
        COMPTES_503,
        TRANSACTIONS_404,
        TRANSACTIONS_500,
        COMPTE_SANS_SOLDE,
        COMPTE_SOLDE_NUL,
        AUCUN_COMPTE,
        TRANSACTIONS_25
    }

    private final String retour;
    private volatile ModeErreur mode = ModeErreur.AUCUNE;
    private final java.util.concurrent.atomic.AtomicInteger appels = new java.util.concurrent.atomic.AtomicInteger();

    public PowensSimule(String retour) {
        this.retour = retour;
    }

    public void mode(ModeErreur mode) {
        this.mode = mode;
    }

    /** Nombre d'appels reçus depuis la création : les tests vérifient qu'un retour invalide n'appelle pas Powens. */
    public int nombreAppels() {
        return appels.get();
    }

    /** Banques proposées par la page web simulée. */
    public List<String> connecteurs() {
        return List.of("BNP Paribas", "Credit Agricole");
    }

    @Override
    public UtilisateurPowens creerUtilisateur() {
        appels.incrementAndGet();
        return switch (mode) {
            case REFUS_CREATION -> throw new PowensIndisponible("création de l'utilisateur refusée");
            case REPONSE_SANS_JETON -> new UtilisateurPowens(0, null);
            default -> new UtilisateurPowens(1, JETON_FIXE);
        };
    }

    @Override
    public String codeTemporaire(String jeton) {
        appels.incrementAndGet();
        return CODE_FIXE;
    }

    @Override
    public String adresseConnexion(String code, String state) {
        appels.incrementAndGet();
        return retour + "?connection_id=" + CONNECTION_ID_FIXE + "&state=" + ConstructeurAdresse.encoder(state);
    }

    @Override
    public List<CompteExterne> comptes(String jeton) {
        appels.incrementAndGet();
        return switch (mode) {
            case COMPTES_500 -> throw new PowensIndisponible("statut 500");
            case COMPTES_503 -> throw new PowensIndisponible("statut 503");
            case AUCUN_COMPTE -> List.of();
            case COMPTE_SANS_SOLDE -> List.of(new CompteExterne(101, "Compte courant BNP Paribas", "checking", null,
                    "FR7630004000031234567890143"));
            case COMPTE_SOLDE_NUL -> List.of(new CompteExterne(101, "Compte courant BNP Paribas", "checking",
                    new BigDecimal("0.00"), "FR7630004000031234567890143"));
            default -> List.of(
                    new CompteExterne(101, "Compte courant BNP Paribas", "checking", new BigDecimal("1250.50"),
                            "FR7630004000031234567890143"),
                    new CompteExterne(102, "Livret Credit Agricole", "savings", new BigDecimal("3000.00"), ""));
        };
    }

    @Override
    public List<TransactionExterne> transactions(String jeton, long compteId) {
        appels.incrementAndGet();
        return switch (mode) {
            case TRANSACTIONS_404 -> throw new PowensIndisponible("statut 404");
            case TRANSACTIONS_500 -> throw new PowensIndisponible("statut 500");
            case TRANSACTIONS_25 -> IntStream.rangeClosed(1, 25).mapToObj(i -> new TransactionExterne(i,
                    LocalDate.of(2026, 10, 1).plusDays(i), "Operation " + i, new BigDecimal("-" + i + ".00"))).toList();
            default -> List.of(
                    new TransactionExterne(1, LocalDate.of(2026, 10, 1), "Salaire", new BigDecimal("2100.00")),
                    new TransactionExterne(2, LocalDate.of(2026, 10, 2), "Courses", new BigDecimal("-54.30")));
        };
    }
}
