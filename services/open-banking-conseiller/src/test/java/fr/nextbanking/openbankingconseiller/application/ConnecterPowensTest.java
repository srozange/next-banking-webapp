package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Horloge;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Lancement de la connexion : ordre des appels, état laissé en cas d'échec. */
@Tag("FR-028-01")
@Tag("FR-028-02")
@Tag("FR-012-05")
class ConnecterPowensTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-06T08:00:00Z");

    private final List<String> appels = new ArrayList<>();
    private UtilisateurPowens utilisateur = new UtilisateurPowens(7, "jeton-permanent");
    private String code = "code-temporaire";
    private RuntimeException panne;
    private StatePowens stateEnregistre;

    private final Powens powens = new Powens() {
        @Override
        public UtilisateurPowens creerUtilisateur() {
            appels.add("creerUtilisateur");
            if (panne != null) {
                throw panne;
            }
            return utilisateur;
        }

        @Override
        public String codeTemporaire(String jeton) {
            appels.add("code:" + jeton);
            return code;
        }

        @Override
        public String adresseConnexion(String code, String state) {
            appels.add("adresse");
            return "https://powens/" + code + "/" + state;
        }

        @Override
        public List<CompteExterne> comptes(String jeton) {
            throw new AssertionError("non attendu");
        }

        @Override
        public List<TransactionExterne> transactions(String jeton, long compteId) {
            throw new AssertionError("non attendu");
        }
    };

    private final DepotEtatPowens depot = new DepotEtatPowens() {
        @Override
        public Optional<EtatPowens> lire(String sub) {
            return Optional.empty();
        }

        @Override
        public void enregistrerJeton(String sub, String jeton) {
            appels.add("jeton:" + sub + ":" + jeton);
        }

        @Override
        public void enregistrerState(StatePowens state) {
            appels.add("state");
            stateEnregistre = state;
        }
    };

    private final EmetteurState emetteur = new EmetteurState() {
        @Override
        public String emettre(StatePowens state) {
            return "signe(" + state.sub() + ")";
        }

        @Override
        public Optional<StatePowens> verifier(String texte) {
            return Optional.empty();
        }
    };

    private final Horloge horloge = new Horloge() {
        @Override
        public Instant instant() {
            return MAINTENANT;
        }

        @Override
        public LocalDate aujourdhui() {
            return LocalDate.of(2026, 10, 6);
        }

        @Override
        public ZoneId fuseau() {
            return ZoneId.of("Europe/Paris");
        }
    };

    private final ConnecterPowensCasUsage cas = new ConnecterPowensCasUsage(powens, depot, emetteur, horloge);

    @Test
    void le_jeton_est_enregistre_avant_l_adresse_et_le_state_signe_est_emis() {
        String adresse = cas.connecter("10");

        assertThat(adresse).isEqualTo("https://powens/code-temporaire/signe(10)");
        assertThat(appels).containsExactly("creerUtilisateur", "jeton:10:jeton-permanent",
                "code:jeton-permanent", "state", "adresse");
        assertThat(stateEnregistre.sub()).isEqualTo("10");
        assertThat(stateEnregistre.nonce()).hasSize(32);
        assertThat(stateEnregistre.expiration()).isEqualTo(MAINTENANT.plus(ConnecterPowensCasUsage.VALIDITE_STATE));
    }

    @Test
    void chaque_connexion_emet_un_nonce_different() {
        cas.connecter("10");
        String premier = stateEnregistre.nonce();
        cas.connecter("10");

        assertThat(stateEnregistre.nonce()).isNotEqualTo(premier);
    }

    @Test
    void un_refus_de_creation_est_une_connexion_impossible_sans_rien_enregistrer() {
        panne = new PowensIndisponible("refus");

        assertThatThrownBy(() -> cas.connecter("10")).isInstanceOf(ConnexionPowensImpossible.class);
        assertThat(appels).containsExactly("creerUtilisateur");
    }

    @Test
    void une_reponse_sans_jeton_est_une_connexion_impossible() {
        for (String jeton : new String[] {null, " "}) {
            utilisateur = new UtilisateurPowens(7, jeton);
            appels.clear();

            assertThatThrownBy(() -> cas.connecter("10")).isInstanceOf(ConnexionPowensImpossible.class);
            assertThat(appels).containsExactly("creerUtilisateur");
        }
    }

    @Test
    void une_panne_au_code_temporaire_est_une_connexion_impossible_sans_adresse() {
        for (String vide : new String[] {null, ""}) {
            code = vide;
            appels.clear();

            assertThatThrownBy(() -> cas.connecter("10")).isInstanceOf(ConnexionPowensImpossible.class);
            assertThat(appels).doesNotContain("adresse", "state");
        }
    }
}
