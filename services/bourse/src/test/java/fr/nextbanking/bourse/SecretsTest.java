package fr.nextbanking.bourse;

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
 * Aucune clé d'accès ni mode réel dans les fichiers de configuration versionnés du service (BUG-034, BUG-091) : toute
 * propriété dont le nom évoque une clé d'API ne peut être que vide ou un renvoi {@code ${…}} sans valeur par défaut,
 * et aucun mode ne vaut {@code reel}.
 */
@Tag("BUG-091")
@Tag("BUG-034")
@Tag("FR-009-14")
@Tag("FR-029-05")
class SecretsTest {

    private static final Path RESSOURCES = Path.of("src/main/resources");
    private static final Pattern FICHIER = Pattern.compile("application.*\\.(properties|ya?ml)");
    private static final Pattern PROPRIETE = Pattern.compile("^-?\\s*([^\\s:=#!]+)\\s*[:=]\\s*(.*)$");
    private static final Pattern NOM_DE_CLE = Pattern.compile("api-key|apikey|api_key|cle-api", Pattern.CASE_INSENSITIVE);
    private static final Pattern RENVOI = Pattern.compile("\\$\\{[^:}]*(?::(.*))?}");

    @Test
    void les_fichiers_de_configuration_versionnes_ne_portent_ni_cle_ni_mode_reel() throws IOException {
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
        assertThat(violations("application.yml", """
                nb:
                  fournisseurs:
                    alphavantage:
                      api-key: VALEURSECRETE
                      mode: "reel"
                """)).hasSize(2);
        assertThat(violations("application.properties", "nb.alpha.API_KEY=${ALPHA_VANTAGE_API_KEY:VALEUR}\n"
                + "alpha.cle-api = 'abc' # commentaire\n" + "nb.alpha" + "vantage.apikey" + "=VALEURSECRETE\n"
                + "nb.fournisseurs.alphavantage.mode=reel"))
                .hasSize(4);
    }

    @Test
    void le_controle_accepte_une_cle_vide_ou_un_renvoi_sans_defaut() {
        assertThat(violations("application.properties", """
                # alpha.api-key=commentaire-ignore
                ! alpha.api-key=commentaire-ignore
                nb.alpha.api-key=
                nb.alpha.api_key=${ALPHA_VANTAGE_API_KEY}
                nb.alpha.cle-api=${ALPHA_VANTAGE_API_KEY:}
                nb.alpha.api-key-vide=""
                nb.fournisseurs.alphavantage.mode=simule
                spring.application.name=bourse
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
            if (NOM_DE_CLE.matcher(nom).find() && secret(valeur)) {
                violations.add(fichier + ":" + (numero + 1) + " clé d'accès versionnée (" + nom + ")");
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
