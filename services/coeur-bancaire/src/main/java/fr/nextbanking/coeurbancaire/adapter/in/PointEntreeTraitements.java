package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTraitements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.domain.ExecutionTraitement;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP du journal des traitements nocturnes, ouvert à tout utilisateur authentifié quel que soit
 * son rôle (FR-011-09). Les instants sortent en ISO-8601 avec le décalage du fuseau métier. Une limite non
 * numérique ou non positive est refusée par {@link GestionErreurs}.
 */
@RestController
public class PointEntreeTraitements {

    static final String CHEMIN = "/api/coeur-bancaire/traitements";

    private final ConsulterTraitements consulterTraitements;
    private final Horloge horloge;

    public PointEntreeTraitements(ConsulterTraitements consulterTraitements, Horloge horloge) {
        this.consulterTraitements = consulterTraitements;
        this.horloge = horloge;
    }

    @GetMapping(CHEMIN)
    public List<Traitement> consulter(
            @RequestParam(name = "limite", defaultValue = "" + ConsulterTraitements.LIMITE_MAXIMALE) long limite) {
        return consulterTraitements.consulter(limite).stream().map(this::depuis).toList();
    }

    private Traitement depuis(ExecutionTraitement execution) {
        return new Traitement(execution.job(), iso(execution.debut()), iso(execution.fin()), execution.dureeMs(),
                execution.enregistrements(), execution.statut() == null ? null : execution.statut().name(),
                execution.details());
    }

    private String iso(Instant instant) {
        return instant == null ? null : instant.atZone(horloge.fuseau()).toOffsetDateTime().toString();
    }

    /** Exécution d'un traitement rendue en JSON. */
    public record Traitement(String job, String debut, String fin, long dureeMs, Integer enregistrements,
            String statut, String details) {
    }
}
