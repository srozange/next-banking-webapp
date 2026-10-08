package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.GererPlafonds;
import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.ErreurPlafond;
import fr.nextbanking.coeurbancaire.domain.ErreurPlafond.Cause;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Cas d'usage : définition et suppression des plafonds. Le client vient du jeton. L'enregistrement remplace le plafond
 * existant en une seule opération du port ; la suppression filtre sur le client, si bien qu'un plafond d'autrui et un
 * plafond inexistant donnent le même refus. Le journal ne porte que des identifiants numériques.
 */
public class GererPlafondsCasUsage implements GererPlafonds {

    private static final Logger JOURNAL = Logger.getLogger(GererPlafondsCasUsage.class.getName());
    private static final Pattern FORMAT_MOIS = Pattern.compile("\\d{4}-\\d{2}");

    private final Plafonds plafonds;

    public GererPlafondsCasUsage(Plafonds plafonds) {
        this.plafonds = plafonds;
    }

    @Override
    public Plafond enregistrer(long clientId, String categorie, String mois, String plafond) {
        CategorieBudget lue = CategorieBudget.deLibelleAffiche(categorie);
        if (lue == null || !lue.estPlafonnable()) {
            throw new ErreurPlafond(Cause.CATEGORIE_INVALIDE);
        }
        Montant montant = Plafond.lire(plafond);
        String periode = lireMois(mois);
        Plafond enregistre = plafonds.enregistrer(clientId, lue, periode, montant);
        JOURNAL.info(() -> "Plafond " + enregistre.id() + " du client " + clientId + " enregistré");
        return enregistre;
    }

    @Override
    public void supprimer(long clientId, long plafondId) {
        if (!plafonds.retirer(clientId, plafondId)) {
            JOURNAL.info(() -> "Suppression refusée du plafond " + plafondId + " pour le client " + clientId);
            throw new ErreurPlafond(Cause.INTROUVABLE);
        }
        JOURNAL.info(() -> "Plafond " + plafondId + " du client " + clientId + " supprimé");
    }

    private static String lireMois(String mois) {
        if (mois == null || !FORMAT_MOIS.matcher(mois).matches()) {
            throw new ErreurPlafond(Cause.MOIS_INVALIDE);
        }
        try {
            return YearMonth.parse(mois).toString();
        } catch (DateTimeParseException e) {
            throw new ErreurPlafond(Cause.MOIS_INVALIDE);
        }
    }
}
