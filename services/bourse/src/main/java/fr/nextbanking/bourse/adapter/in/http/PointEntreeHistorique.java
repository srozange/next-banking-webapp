package fr.nextbanking.bourse.adapter.in.http;

import fr.nextbanking.bourse.application.port.in.ConsulterHistorique;
import fr.nextbanking.bourse.domain.HistoriqueCours;
import fr.nextbanking.bourse.domain.PointCotation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historique des cours d'une valeur, ouvert à tout utilisateur authentifié (FR-009-08, FR-029-01, FR-029-02) : prix en
 * chaîne décimale exacte, dates {@code yyyy-MM-dd}, points triés par date croissante ; liste vide si le fournisseur
 * n'a rien rendu. Un symbole absent ou blanc est refusé par {@link GestionErreurs} (400 « Requête invalide »).
 */
@RestController
public class PointEntreeHistorique {

    static final String CHEMIN = "/api/bourse/historique";

    private final ConsulterHistorique consulterHistorique;

    public PointEntreeHistorique(ConsulterHistorique consulterHistorique) {
        this.consulterHistorique = consulterHistorique;
    }

    @GetMapping(CHEMIN)
    public Historique consulter(@RequestParam(required = false) String symbole,
            @RequestParam(required = false) String periode) {
        return Historique.depuis(consulterHistorique.consulter(symbole, periode));
    }

    /** Historique rendu en JSON ; la période est celle retenue (une période inconnue vaut 1M). */
    public record Historique(String symbole, String periode, List<Point> points) {

        static Historique depuis(HistoriqueCours historique) {
            return new Historique(historique.symbole(), historique.periode().code(),
                    historique.points().stream().map(Point::depuis).toList());
        }
    }

    /** Point de cours : prix en chaîne décimale exacte, jamais en flottant. */
    public record Point(String date, String ouverture, String haut, String bas, String cloture, long volume) {

        static Point depuis(PointCotation point) {
            return new Point(point.date().toString(), point.ouverture().toPlainString(), point.haut().toPlainString(),
                    point.bas().toPlainString(), point.cloture().toPlainString(), point.volume());
        }
    }
}
