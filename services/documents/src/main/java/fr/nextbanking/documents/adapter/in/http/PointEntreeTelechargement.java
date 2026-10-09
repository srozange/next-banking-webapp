package fr.nextbanking.documents.adapter.in.http;

import fr.nextbanking.documents.application.port.in.TelechargerDocument;
import fr.nextbanking.documents.application.port.in.TelechargerDocument.Commande;
import fr.nextbanking.documents.domain.DocumentPdf;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/documents/telechargement?type=&compteId=&annee=&mois=}, pour le client désigné par le
 * sujet du jeton, tout rôle confondu. Les paramètres sont reçus en chaînes : leur validation, et les refus
 * (400, 404, 403), sont ceux du cas d'usage. Un sujet qui n'est pas un identifiant numérique donne 401.
 */
@RestController
public class PointEntreeTelechargement {

    /** Valeur exacte du legacy. */
    static final String CACHE_CONTROL = "no-cache, no-store, must-revalidate";

    private final TelechargerDocument telechargerDocument;

    public PointEntreeTelechargement(TelechargerDocument telechargerDocument) {
        this.telechargerDocument = telechargerDocument;
    }

    @GetMapping("/api/documents/telechargement")
    public ResponseEntity<byte[]> telecharger(@AuthenticationPrincipal Jwt jeton,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "compteId", required = false) String compteId,
            @RequestParam(name = "annee", required = false) String annee,
            @RequestParam(name = "mois", required = false) String mois) {
        DocumentPdf document = telechargerDocument
                .telecharger(new Commande(clientId(jeton), type, compteId, annee, mois));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.nomFichier() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .contentLength(document.contenu().length)
                .body(document.contenu());
    }

    private static long clientId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }
}
