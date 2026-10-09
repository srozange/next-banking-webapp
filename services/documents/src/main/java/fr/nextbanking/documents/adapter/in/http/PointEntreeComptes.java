package fr.nextbanking.documents.adapter.in.http;

import fr.nextbanking.documents.application.port.in.ConsulterPageDocuments;
import fr.nextbanking.documents.application.port.in.ConsulterPageDocuments.Page;
import fr.nextbanking.documents.domain.CompteProjete;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/documents/comptes}, les comptes du client désigné par le sujet du jeton (jamais
 * un identifiant fourni par l'appelant, aucun contrôle de rôle) et la date du jour {@code aujourdhui} (yyyy-MM-dd, Europe/Paris).
 */
@RestController
public class PointEntreeComptes {

    /** Compte tel que présenté à l'écran. */
    record CompteDto(long compteId, String numero, String type) {
    }

    /** Réponse : champs JSON en français. */
    record PageDto(String aujourdhui, List<CompteDto> comptes) {
    }

    private final ConsulterPageDocuments consulterPageDocuments;

    public PointEntreeComptes(ConsulterPageDocuments consulterPageDocuments) {
        this.consulterPageDocuments = consulterPageDocuments;
    }

    @GetMapping("/api/documents/comptes")
    public PageDto comptes(@AuthenticationPrincipal Jwt jeton) {
        long clientId;
        try {
            clientId = Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
        Page page = consulterPageDocuments.consulter(clientId);
        return new PageDto(page.aujourdhui().toString(), page.comptes().stream().map(PointEntreeComptes::dto).toList());
    }

    private static CompteDto dto(CompteProjete compte) {
        return new CompteDto(compte.compteId(), compte.numero(), compte.type().name());
    }
}
