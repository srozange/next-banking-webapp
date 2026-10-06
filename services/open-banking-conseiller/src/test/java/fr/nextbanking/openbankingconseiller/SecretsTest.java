package fr.nextbanking.openbankingconseiller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Aucun secret, aucune clé d'accès ni mode réel dans les fichiers de configuration versionnés du service (BUG-038,
 * FR-010-08, NFR-SEC-06) : toute propriété dont le nom évoque un secret, une clé d'API ou un jeton ne peut être que
 * vide ou un renvoi {@code ${…}} sans valeur par défaut, et aucun mode ne vaut {@code reel}.
 */
@Tag("FR-010-08")
@Tag("BUG-038")
@Tag("FR-028-12")
@Tag("FR-028-01")
class SecretsTest {

    private static final Path RESSOURCES = Path.of("src/main/resources");
    private static final Pattern FICHIER = Pattern.compile("application.*\\.(properties|ya?ml)");
    private static final Pattern PROPRIETE = Pattern.compile("^-?\\s*([^\\s:=#!]+)\\s*[:=]\\s*(.*)$");
    private static final Pattern NOM_SECRET = Pattern.compile("secret|api-key|apikey|api_key|cle-api|token|jeton\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RENVOI = Pattern.compile("\\$\\{[^:}]*(?::(.*))?}");

    @Test
    void les_fichiers_de_configuration_versionnes_ne_portent_ni_secret_ni_mode_reel() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> fichiers = Files.list(RESSOURCES)) {
            for (Path fichier : fichiers.filter(f -> FICHIER.matcher(f.getFileName().toString()).matches()).toList()) {
                violations.addAll(violations(fichier.getFileName().toString(),
                        Files.readString(fichier, StandardCharsets.UTF_8)));
            }
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void le_controle_detecte_une_cle_ou_un_defaut_et_un_mode_reel() {
        assertThat(violations("application.properties", "nb.azure.API_KEY=${AZURE_OPENAI_API_KEY:VALEUR}\n"
                + "azure.cle-api = 'abc' # commentaire\n" + "nb.fournisseurs.azure-openai.mode=reel"))
                .hasSize(3);
    }

    @Test
    void le_controle_accepte_une_cle_vide_ou_un_renvoi_sans_defaut() {
        assertThat(violations("application.properties", """
                # azure.api-key=commentaire-ignore
                nb.azure.api-key=
                nb.azure.api_key=${AZURE_OPENAI_API_KEY}
                nb.azure.cle-api=${AZURE_OPENAI_API_KEY:}
                nb.azure.api-key-vide=""
                nb.fournisseurs.azure-openai.mode=simule
                ligne sans separateur
                """)).isEmpty();
    }

    @Test
    void le_controle_detecte_un_secret_un_defaut_et_un_mode_reel() {
        assertThat(violations("application.yml", """
                nb:
                  fournisseurs:
                    powens:
                      client-secret: VALEURSECRETE
                      mode: "reel"
                """)).hasSize(2);
        assertThat(violations("application.properties", "nb.powens.SECRET=${POWENS_CLIENT_SECRET:valeur}\n"
                + "nb.powens.token = 'abc' # commentaire\n" + "nb.fournisseurs.powens.mode=reel")).hasSize(3);
    }

    @Test
    void le_controle_accepte_un_secret_vide_ou_un_renvoi_sans_defaut() {
        assertThat(violations("application.properties", """
                # nb.powens.secret=commentaire-ignore
                nb.powens.secret=
                nb.powens.client-secret=${POWENS_CLIENT_SECRET}
                nb.powens.token=${POWENS_TOKEN:}
                nb.fournisseurs.powens.mode=simule
                nb.jeton.cle-publique=${NB_JETON_CLE_PUBLIQUE}
                ligne sans separateur
                """)).isEmpty();
    }

    static List<String> violations(String fichier, String contenu) {
        List<String> violations = new ArrayList<>();
        String[] lignes = contenu.split("\\R");
        for (int numero = 0; numero < lignes.length; numero++) {
            String ligne = lignes[numero].strip();
            Matcher propriete = PROPRIETE.matcher(ligne);
            if (ligne.startsWith("#") || ligne.startsWith("!") || !propriete.matches()) {
                continue;
            }
            String nom = propriete.group(1);
            String valeur = valeur(propriete.group(2));
            if (NOM_SECRET.matcher(nom).find() && secret(valeur)) {
                violations.add(fichier + ":" + (numero + 1) + " secret versionné (" + nom + ")");
            }
            if (nom.toLowerCase(Locale.ROOT).endsWith("mode") && valeur.equalsIgnoreCase("reel")) {
                violations.add(fichier + ":" + (numero + 1) + " mode réel versionné (" + nom + ")");
            }
        }
        return violations;
    }

    private static String valeur(String brute) {
        String valeur = brute.replaceFirst("\\s+#.*$", "").strip();
        if (valeur.length() >= 2 && (valeur.startsWith("\"") && valeur.endsWith("\"")
                || valeur.startsWith("'") && valeur.endsWith("'"))) {
            valeur = valeur.substring(1, valeur.length() - 1).strip();
        }
        return valeur;
    }

    private static boolean secret(String valeur) {
        Matcher renvoi = RENVOI.matcher(valeur);
        if (renvoi.matches()) {
            return renvoi.group(1) != null && !renvoi.group(1).isBlank();
        }
        return !valeur.isEmpty();
    }
}
