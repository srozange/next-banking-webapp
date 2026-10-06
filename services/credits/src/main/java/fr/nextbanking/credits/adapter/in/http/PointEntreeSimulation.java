package fr.nextbanking.credits.adapter.in.http;

import fr.nextbanking.credits.application.port.in.SimulerEmprunt;
import fr.nextbanking.credits.domain.LignePret;
import fr.nextbanking.credits.domain.Simulation;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code POST /api/credits/simulations}. Les champs saisis arrivent en chaînes ; les
 * montants repartent en chaînes à 2 décimales.
 */
@RestController
public class PointEntreeSimulation {

    private final SimulerEmprunt simulerEmprunt;

    public PointEntreeSimulation(SimulerEmprunt simulerEmprunt) {
        this.simulerEmprunt = simulerEmprunt;
    }

    @PostMapping("/api/credits/simulations")
    public ReponseSimulation simuler(@RequestBody DemandeSimulationJson demande) {
        Simulation simulation = simulerEmprunt.simuler(demande.montant(), demande.tauxAnnuel(), demande.dureeMois());
        return new ReponseSimulation(simulation.mensualite().toPlainString(), simulation.coutTotal().toPlainString(),
                simulation.totalInterets().toPlainString(), simulation.echeancier().stream().map(Ligne::de).toList());
    }

    /** Corps de la requête. */
    public record DemandeSimulationJson(String montant, String tauxAnnuel, String dureeMois) {
    }

    /** Corps de la réponse. */
    public record ReponseSimulation(String mensualite, String coutTotal, String totalInterets, List<Ligne> echeancier) {
    }

    /** Ligne de l'échéancier. */
    public record Ligne(int numero, String capital, String interets, String total, String capitalRestant) {

        static Ligne de(LignePret ligne) {
            return new Ligne(ligne.numero(), ligne.capital().toPlainString(), ligne.interets().toPlainString(),
                    ligne.total().toPlainString(), ligne.capitalRestant().toPlainString());
        }
    }
}
