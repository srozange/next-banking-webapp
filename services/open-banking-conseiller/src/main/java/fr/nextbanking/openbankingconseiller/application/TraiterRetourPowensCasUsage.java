package fr.nextbanking.openbankingconseiller.application;

import fr.nextbanking.openbankingconseiller.application.port.in.TraiterRetourPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Horloge;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Retour public de Powens (FR-014-01..08, FR-012-02, FR-012-06..09, FR-028-03..06, BUG-040, BUG-044, BUG-089). Le client
 * vient du {@code state} signé, jamais d'un paramètre libre. Ordre : {@code error} puis {@code connection_id}, le reste
 * est incomplet ; un paramètre vide compte comme absent.
 */
public class TraiterRetourPowensCasUsage implements TraiterRetourPowens {

    static final int TRANSACTIONS_MAX = 20;
    static final String SESSION_EXPIREE = "Session expiree. Veuillez recommencer la connexion.";
    static final String AUCUN_COMPTE =
            "Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.";
    static final String POWENS_INDISPONIBLE =
            "Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants.";

    private final Powens powens;
    private final DepotEtatPowens depot;
    private final EmetteurState emetteur;
    private final Horloge horloge;

    public TraiterRetourPowensCasUsage(Powens powens, DepotEtatPowens depot, EmetteurState emetteur, Horloge horloge) {
        this.powens = powens;
        this.depot = depot;
        this.emetteur = emetteur;
        this.horloge = horloge;
    }

    @Override
    public void traiter(Map<String, String> parametres) {
        Optional<String> sub = clientDuState(valeur(parametres, "state"));
        if (sub.isEmpty()) {
            return;
        }
        String erreur = valeur(parametres, "error");
        if (erreur != null) {
            depot.remettreAZero(sub.get());
            depot.poserMessage(sub.get(), "La connexion a votre banque n a pas abouti (" + erreur
                    + "). Veuillez recommencer la connexion.");
        } else if (valeur(parametres, "connection_id") != null) {
            lireLesComptes(sub.get());
        } else {
            depot.poserMessage(sub.get(), "Callback Powens incomplet. Parametres recus: " + recus(parametres));
        }
    }

    /** Client du {@code state} s'il est bien signé, non expiré et consommé ici pour la première fois. */
    private Optional<String> clientDuState(String texte) {
        if (texte == null) {
            return Optional.empty();
        }
        Instant maintenant = horloge.instant();
        return emetteur.verifier(texte)
                .filter(state -> !state.estExpire(maintenant))
                .filter(state -> depot.consommerState(state, maintenant))
                .map(StatePowens::sub);
    }

    private void lireLesComptes(String sub) {
        String jeton = depot.lire(sub).map(EtatPowens::jeton).orElse(null);
        if (jeton == null || jeton.isBlank()) {
            depot.poserMessage(sub, SESSION_EXPIREE);
            return;
        }
        List<CompteExterne> comptes;
        try {
            comptes = powens.comptes(jeton);
        } catch (PowensIndisponible indisponible) {
            depot.poserMessage(sub, POWENS_INDISPONIBLE);
            return;
        }
        if (comptes.isEmpty()) {
            depot.enregistrerConnexion(sub, comptes, null, List.of(), AUCUN_COMPTE);
            return;
        }
        long selectionne = comptes.get(0).id();
        try {
            List<TransactionExterne> transactions = powens.transactions(jeton, selectionne);
            depot.enregistrerConnexion(sub, comptes, selectionne,
                    transactions.subList(0, Math.min(TRANSACTIONS_MAX, transactions.size())), null);
        } catch (PowensIndisponible indisponible) {
            depot.enregistrerConnexion(sub, comptes, selectionne, List.of(), POWENS_INDISPONIBLE);
        }
    }

    /** Paramètres reçus non vides autres que {@code state}, dans l'ordre reçu : {@code [a, b]}. */
    private static String recus(Map<String, String> parametres) {
        return parametres.keySet().stream()
                .filter(nom -> !nom.equals("state") && valeur(parametres, nom) != null)
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    /** Valeur d'un paramètre, absente si vide ou blanche. */
    private static String valeur(Map<String, String> parametres, String nom) {
        String valeur = parametres.get(nom);
        return valeur == null || valeur.isBlank() ? null : valeur;
    }
}
