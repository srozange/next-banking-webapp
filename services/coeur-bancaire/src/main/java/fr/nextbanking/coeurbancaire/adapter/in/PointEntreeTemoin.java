package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : point témoin {@code POST /api/coeur-bancaire/temoin} (chemin public).
 * Une demande refusée (400) répond par son seul statut, sans corps.
 */
@RestController
public class PointEntreeTemoin {

    private final EcrireTemoin ecrireTemoin;

    public PointEntreeTemoin(EcrireTemoin ecrireTemoin) {
        this.ecrireTemoin = ecrireTemoin;
    }

    @PostMapping("/api/coeur-bancaire/temoin")
    public ResponseEntity<TemoinCree> ecrire(@RequestBody DemandeTemoin demande) {
        long temoinId = ecrireTemoin.ecrire(demande.libelle());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TemoinCree(temoinId));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Void> demandeRefusee() {
        return ResponseEntity.badRequest().build();
    }

    /** Corps de la demande. */
    public record DemandeTemoin(String libelle) {
    }

    /** Corps de la réponse. */
    public record TemoinCree(long temoinId) {
    }
}
