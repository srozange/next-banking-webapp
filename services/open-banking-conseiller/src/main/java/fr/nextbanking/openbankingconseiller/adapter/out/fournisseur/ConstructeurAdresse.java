package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Adresse de la page web de Powens : domaine, client, retour et code encodés, banques proposées, {@code state}. */
final class ConstructeurAdresse {

    static final String PAGE_WEB = "https://webview.powens.com/connect";

    private ConstructeurAdresse() {
    }

    static String adresse(String domaine, String clientId, String retour, String code, String banques,
            String state) {
        return PAGE_WEB + "?domain=" + encoder(domaine) + "&client_id=" + encoder(clientId)
                + "&redirect_uri=" + encoder(retour) + "&code=" + encoder(code)
                + "&connector_ids=" + encoder(banques) + "&state=" + encoder(state);
    }

    static String encoder(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }
}
