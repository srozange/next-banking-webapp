package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBudget;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cas d'usage : budget d'un mois (FEAT-006). Le client vient du jeton ; ses opérations sont celles de tous ses comptes.
 * Les mois sont ceux du fuseau de l'{@code Horloge}.
 */
public class ConsulterBudgetCasUsage implements ConsulterBudget {

    static final int PERIODES_MAX = 24;
    private static final Pattern FORMAT_PERIODE = Pattern.compile("\\d{4}-\\d{2}");

    private final Operations operations;
    private final Plafonds plafonds;
    private final Horloge horloge;

    public ConsulterBudgetCasUsage(Operations operations, Plafonds plafonds, Horloge horloge) {
        this.operations = operations;
        this.plafonds = plafonds;
        this.horloge = horloge;
    }

    @Override
    public Budget budget(long clientId, String periodeDemandee) {
        List<YearMonth> periodes = operations.moisAvecOperations(clientId, horloge.fuseau(), PERIODES_MAX);
        YearMonth periode = lire(periodeDemandee);
        if (periode == null) {
            periode = periodes.isEmpty() ? YearMonth.from(horloge.aujourdhui()) : periodes.get(0);
        }
        Instant debut = periode.atDay(1).atStartOfDay(horloge.fuseau()).toInstant();
        Instant fin = periode.plusMonths(1).atDay(1).atStartOfDay(horloge.fuseau()).toInstant();
        Montant depenses = Montant.ZERO;
        Montant revenus = Montant.ZERO;
        Map<CategorieBudget, Montant> parCategorie = new EnumMap<>(CategorieBudget.class);
        for (Operation operation : operations.duClientSurPeriode(clientId, debut, fin)) {
            if (operation.type() == TypeOperation.DEBIT) {
                depenses = depenses.ajouter(operation.montant());
                parCategorie.merge(CategorieBudget.deLibelle(operation.description()), operation.montant(),
                        Montant::ajouter);
            } else {
                revenus = revenus.ajouter(operation.montant());
            }
        }
        List<LigneRepartition> repartition = new ArrayList<>();
        parCategorie.forEach((categorie, montant) -> {
            if (montant.valeur().signum() > 0) {
                repartition.add(new LigneRepartition(categorie, montant));
            }
        });
        List<LignePlafond> lignesPlafonds = plafonds.duMois(clientId, periode.toString()).stream()
                .sorted(Comparator.comparing(Plafond::categorie))
                .map(p -> new LignePlafond(p.id(), p.categorie(), p.montant(),
                        parCategorie.getOrDefault(p.categorie(), Montant.ZERO)))
                .toList();
        return new Budget(horloge.aujourdhui(), periode, periodes, depenses, revenus,
                plafonds.compter(clientId, periode.toString()), repartition, lignesPlafonds);
    }

    /** Période lue, ou nulle si le texte est absent ou illisible. */
    private static YearMonth lire(String texte) {
        if (texte == null || !FORMAT_PERIODE.matcher(texte).matches()) {
            return null;
        }
        try {
            return YearMonth.parse(texte);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
