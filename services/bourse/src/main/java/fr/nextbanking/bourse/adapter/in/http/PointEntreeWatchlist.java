package fr.nextbanking.bourse.adapter.in.http;

import fr.nextbanking.bourse.application.port.in.AjouterValeur;
import fr.nextbanking.bourse.application.port.in.ConsulterWatchlist;
import fr.nextbanking.bourse.application.port.in.RetirerValeur;
import fr.nextbanking.bourse.domain.ValeurWatchlist;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de la watchlist commune, ouverte à tout utilisateur authentifié quel que soit son rôle
 * (FR-009-01) : lecture triée, ajout, retrait sans confirmation. Les refus sont rendus par {@link GestionErreurs}.
 */
@RestController
public class PointEntreeWatchlist {

    static final String CHEMIN = "/api/bourse/watchlist";

    private final ConsulterWatchlist consulterWatchlist;
    private final AjouterValeur ajouterValeur;
    private final RetirerValeur retirerValeur;

    public PointEntreeWatchlist(ConsulterWatchlist consulterWatchlist, AjouterValeur ajouterValeur,
            RetirerValeur retirerValeur) {
        this.consulterWatchlist = consulterWatchlist;
        this.ajouterValeur = ajouterValeur;
        this.retirerValeur = retirerValeur;
    }

    @GetMapping(CHEMIN)
    public List<Valeur> consulter() {
        return consulterWatchlist.consulter().stream().map(Valeur::depuis).toList();
    }

    @PostMapping(CHEMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public Valeur ajouter(@RequestBody DemandeAjout demande) {
        return Valeur.depuis(ajouterValeur.ajouter(demande.symbole(), demande.nom(), demande.marche(),
                demande.devise()));
    }

    @DeleteMapping(CHEMIN + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(@PathVariable long id) {
        retirerValeur.retirer(id);
    }

    /** Corps de la demande d'ajout, tel que saisi. */
    public record DemandeAjout(String symbole, String nom, String marche, String devise) {
    }

    /** Valeur de la watchlist rendue en JSON. */
    public record Valeur(long id, String symbole, String nom, String marche, String devise) {

        static Valeur depuis(ValeurWatchlist valeur) {
            return new Valeur(valeur.id(), valeur.symbole(), valeur.nom(), valeur.marche(), valeur.devise());
        }
    }
}
