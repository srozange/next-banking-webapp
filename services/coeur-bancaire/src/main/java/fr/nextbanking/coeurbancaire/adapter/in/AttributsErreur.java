package fr.nextbanking.coeurbancaire.adapter.in;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Corps des erreurs rendues par le dispatch d'erreur ({@code /error}) : le seul champ {@code message}, texte
 * générique du statut, au lieu des attributs par défaut (horodatage, chemin, exception...).
 */
@Component
public class AttributsErreur extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest requete, ErrorAttributeOptions options) {
        Object statut = super.getErrorAttributes(requete, ErrorAttributeOptions.defaults()).get("status");
        int valeur = statut instanceof Integer entier ? entier : 500;
        return Map.of("message", Textes.texte(Textes.statutPublie(valeur)));
    }
}
