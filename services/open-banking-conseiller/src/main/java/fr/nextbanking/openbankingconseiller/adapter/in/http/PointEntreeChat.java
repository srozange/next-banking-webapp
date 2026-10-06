package fr.nextbanking.openbankingconseiller.adapter.in.http;

import fr.nextbanking.openbankingconseiller.application.port.in.Dialoguer;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import fr.nextbanking.openbankingconseiller.domain.RoleEchange;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de l'échange avec Sophie, sans état : l'appelant fournit l'historique (rôles
 * {@code utilisateur} / {@code assistant}), hors message d'accueil et message courant (RG-010-011, RG-010-012).
 * Un message blanc, un rôle inconnu ou un échange incomplet donnent 400 (voir {@link GestionErreurs}).
 */
@RestController
public class PointEntreeChat {

    static final String CHEMIN = "/api/open-banking-conseiller/chat/messages";

    private final Dialoguer dialoguer;

    public PointEntreeChat(Dialoguer dialoguer) {
        this.dialoguer = dialoguer;
    }

    @PostMapping(CHEMIN)
    public ReponseChat envoyer(@RequestBody RequeteChat requete) {
        List<Echange> historique = requete.historique() == null ? List.of()
                : requete.historique().stream().map(EchangeJson::versDomaine).toList();
        return new ReponseChat(dialoguer.dialoguer(requete.message(), historique));
    }

    /** Corps de la requête. */
    public record RequeteChat(String message, List<EchangeJson> historique) {
    }

    /** Un tour de l'historique. */
    public record EchangeJson(String role, String contenu) {

        Echange versDomaine() {
            RoleEchange auteur = "utilisateur".equals(role) ? RoleEchange.UTILISATEUR
                    : "assistant".equals(role) ? RoleEchange.ASSISTANT : null;
            if (auteur == null || contenu == null) {
                throw new RequeteInvalide();
            }
            return new Echange(auteur, contenu);
        }
    }

    /** Corps de la réponse. */
    public record ReponseChat(String reponse) {
    }

    /** Historique mal formé. */
    static final class RequeteInvalide extends RuntimeException {

        RequeteInvalide() {
            super("historique invalide", null, false, false);
        }
    }
}
