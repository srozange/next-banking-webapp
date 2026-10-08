package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBudget;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBudget.Budget;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/coeur-bancaire/budget?periode=AAAA-MM}. Le client est celui du jeton
 * ({@code sub}) ; aucun autre paramètre (identifiant de client ou de compte) n'est lu (NFR-SEC-03). Montants en chaîne
 * décimale à point.
 */
@RestController
public class PointEntreeBudget {

    static final String CHEMIN_BUDGET = "/api/coeur-bancaire/budget";

    private final ConsulterBudget consulterBudget;

    public PointEntreeBudget(ConsulterBudget consulterBudget) {
        this.consulterBudget = consulterBudget;
    }

    @GetMapping(CHEMIN_BUDGET)
    public BudgetClient budget(@AuthenticationPrincipal Jwt jeton,
            @RequestParam(value = "periode", required = false) String periode) {
        Budget budget = consulterBudget.budget(Long.parseLong(jeton.getSubject()), periode);
        return new BudgetClient(budget.aujourdhui().toString(), budget.periode().toString(),
                budget.periodes().stream().map(YearMonth::toString).toList(), budget.totalDepenses().texte(),
                budget.totalRevenus().texte(), budget.plafondsDefinis(),
                budget.repartition().stream()
                        .map(ligne -> new LigneRepartition(ligne.categorie().libelle(), ligne.montant().texte()))
                        .toList(),
                budget.plafonds().stream()
                        .map(l -> new LignePlafond(l.id(), l.categorie().libelle(), l.plafond().texte(),
                                l.depense().texte()))
                        .toList());
    }

    /** Budget d'un mois : dates {@code AAAA-MM-JJ} et {@code AAAA-MM}, montants en chaîne décimale à point. */
    public record BudgetClient(String aujourdhui, String periode, List<String> periodes, String totalDepenses,
            String totalRevenus, long plafondsDefinis, List<LigneRepartition> repartition,
            List<LignePlafond> plafonds) {
    }

    /** Dépense d'une catégorie. */
    public record LigneRepartition(String categorie, String montant) {
    }

    /** Plafond d'une catégorie et dépense de la catégorie sur la période. */
    public record LignePlafond(long id, String categorie, String plafond, String depense) {
    }
}
