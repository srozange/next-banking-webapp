package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Horloge;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Retour de Powens : ordre de traitement, state à usage unique, messages, troncature à 20 transactions. */
@Tag("FR-014-01")
@Tag("FR-014-02")
@Tag("FR-014-03")
@Tag("FR-014-04")
@Tag("FR-014-05")
@Tag("FR-014-06")
@Tag("FR-014-07")
@Tag("FR-014-08")
@Tag("FR-012-02")
@Tag("FR-012-06")
@Tag("FR-012-07")
@Tag("FR-012-08")
@Tag("FR-012-09")
@Tag("FR-028-03")
@Tag("FR-028-04")
@Tag("FR-028-05")
@Tag("FR-028-06")
@Tag("BUG-040")
@Tag("BUG-044")
@Tag("BUG-089")
class TraiterRetourPowensTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-06T08:00:00Z");
    private static final StatePowens VALIDE = new StatePowens("10", "nonce-1", MAINTENANT.plusSeconds(600));
    private static final CompteExterne COMPTE_1 = new CompteExterne(101, "Courant", "checking",
            new BigDecimal("10.00"), "");
    private static final CompteExterne COMPTE_2 = new CompteExterne(102, "Livret", "savings", null, "");

    private final List<String> appelsPowens = new ArrayList<>();
    private final List<String> ecritures = new ArrayList<>();
    private final Set<String> noncesConsommes = new HashSet<>();
    private String jeton = "jeton-permanent";
    private List<CompteExterne> comptes = List.of(COMPTE_1, COMPTE_2);
    private List<TransactionExterne> transactions = transactions(25);
    private boolean comptesEnPanne;
    private boolean transactionsEnPanne;
    private List<TransactionExterne> transactionsEnregistrees;
    private Long selectionneEnregistre;

    private final Powens powens = new Powens() {
        @Override
        public UtilisateurPowens creerUtilisateur() {
            throw new AssertionError("non attendu");
        }

        @Override
        public String codeTemporaire(String jeton) {
            throw new AssertionError("non attendu");
        }

        @Override
        public String adresseConnexion(String code, String state) {
            throw new AssertionError("non attendu");
        }

        @Override
        public List<CompteExterne> comptes(String jeton) {
            appelsPowens.add("comptes:" + jeton);
            if (comptesEnPanne) {
                throw new PowensIndisponible("panne");
            }
            return comptes;
        }

        @Override
        public List<TransactionExterne> transactions(String jeton, long compteId) {
            appelsPowens.add("transactions:" + compteId);
            if (transactionsEnPanne) {
                throw new PowensIndisponible("panne");
            }
            return transactions;
        }
    };

    private final DepotEtatPowens depot = new DepotEtatPowens() {
        @Override
        public Optional<EtatPowens> lire(String sub) {
            return Optional.of(new EtatPowens(jeton, false, null));
        }

        @Override
        public void enregistrerJeton(String sub, String jeton) {
            throw new AssertionError("non attendu");
        }

        @Override
        public void enregistrerState(StatePowens state) {
            throw new AssertionError("non attendu");
        }

        @Override
        public boolean consommerState(StatePowens state, Instant maintenant) {
            return !state.estExpire(maintenant) && noncesConsommes.add(state.nonce());
        }

        @Override
        public void remettreAZero(String sub) {
            ecritures.add("zero:" + sub);
        }

        @Override
        public void poserMessage(String sub, String message) {
            ecritures.add("message:" + sub + ":" + message);
        }

        @Override
        public void enregistrerConnexion(String sub, List<CompteExterne> comptes, Long selectionne,
                List<TransactionExterne> transactions, String message) {
            ecritures.add("connexion:" + sub + ":" + comptes.size() + ":" + selectionne + ":" + message);
            transactionsEnregistrees = transactions;
            selectionneEnregistre = selectionne;
        }
    };

    private final EmetteurState emetteur = new EmetteurState() {
        @Override
        public String emettre(StatePowens state) {
            return "signe";
        }

        @Override
        public Optional<StatePowens> verifier(String texte) {
            return "valide".equals(texte) ? Optional.of(VALIDE) : Optional.empty();
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

    private final TraiterRetourPowensCasUsage cas = new TraiterRetourPowensCasUsage(powens, depot, emetteur, horloge);

    private static List<TransactionExterne> transactions(int nombre) {
        return IntStream.rangeClosed(1, nombre).mapToObj(i -> new TransactionExterne(i, LocalDate.of(2026, 10, 1),
                "Operation " + i, new BigDecimal("1.00"))).toList();
    }

    private static Map<String, String> parametres(String... paires) {
        Map<String, String> resultat = new LinkedHashMap<>();
        for (int i = 0; i < paires.length; i += 2) {
            resultat.put(paires[i], paires[i + 1]);
        }
        return resultat;
    }

    private void aucunEffet() {
        assertThat(ecritures).isEmpty();
        assertThat(appelsPowens).isEmpty();
    }

    @Test
    void un_state_absent_vide_ou_mal_signe_n_a_aucun_effet() {
        cas.traiter(parametres("connection_id", "1"));
        cas.traiter(parametres("connection_id", "1", "state", ""));
        cas.traiter(parametres("connection_id", "1", "state", "forge"));
        cas.traiter(parametres());

        aucunEffet();
    }

    @Test
    void un_state_expire_ou_deja_utilise_n_a_aucun_effet() {
        StatePowens expire = new StatePowens("10", "nonce-2", MAINTENANT);
        EmetteurState emetteurExpire = new EmetteurState() {
            @Override
            public String emettre(StatePowens state) {
                return "signe";
            }

            @Override
            public Optional<StatePowens> verifier(String texte) {
                return Optional.of(expire);
            }
        };
        new TraiterRetourPowensCasUsage(powens, depot, emetteurExpire, horloge)
                .traiter(parametres("connection_id", "1", "state", "x"));
        aucunEffet();

        noncesConsommes.add("nonce-1");
        cas.traiter(parametres("connection_id", "1", "state", "valide"));
        aucunEffet();
    }

    @Test
    void le_state_n_est_consommable_qu_une_fois() {
        cas.traiter(parametres("connection_id", "1", "state", "valide"));
        ecritures.clear();
        appelsPowens.clear();

        cas.traiter(parametres("connection_id", "1", "state", "valide"));

        aucunEffet();
    }

    @Test
    void error_remet_a_zero_puis_pose_le_message_meme_avec_connection_id_et_code() {
        cas.traiter(parametres("connection_id", "1", "code", "abc", "error", "user_cancelled", "state", "valide"));

        assertThat(ecritures).containsExactly("zero:10",
                "message:10:La connexion a votre banque n a pas abouti (user_cancelled). "
                        + "Veuillez recommencer la connexion.");
        assertThat(appelsPowens).isEmpty();
    }

    @Test
    void connection_id_lit_les_comptes_selectionne_le_premier_et_limite_a_vingt_transactions() {
        cas.traiter(parametres("connection_id", "1", "state", "valide"));

        assertThat(ecritures).containsExactly("connexion:10:2:101:null");
        assertThat(appelsPowens).containsExactly("comptes:jeton-permanent", "transactions:101");
        assertThat(transactionsEnregistrees).hasSize(20).isEqualTo(transactions(25).subList(0, 20));
    }

    @Test
    void moins_de_vingt_transactions_sont_gardees_telles_quelles() {
        transactions = transactions(2);

        cas.traiter(parametres("connection_id", "1", "state", "valide"));

        assertThat(transactionsEnregistrees).hasSize(2);
    }

    @Test
    void aucun_compte_est_connecte_avec_son_message() {
        comptes = List.of();

        cas.traiter(parametres("connection_id", "1", "state", "valide"));

        assertThat(ecritures).containsExactly("connexion:10:0:null:" + TraiterRetourPowensCasUsage.AUCUN_COMPTE);
        assertThat(appelsPowens).containsExactly("comptes:jeton-permanent");
        assertThat(selectionneEnregistre).isNull();
    }

    @Test
    void une_panne_aux_comptes_laisse_non_connecte_avec_le_message_d_indisponibilite() {
        comptesEnPanne = true;

        cas.traiter(parametres("connection_id", "1", "state", "valide"));

        assertThat(ecritures).containsExactly("message:10:" + TraiterRetourPowensCasUsage.POWENS_INDISPONIBLE);
    }

    @Test
    void une_panne_aux_transactions_garde_la_connexion_sans_transaction_avec_le_message() {
        transactionsEnPanne = true;

        cas.traiter(parametres("connection_id", "1", "state", "valide"));

        assertThat(ecritures).containsExactly(
                "connexion:10:2:101:" + TraiterRetourPowensCasUsage.POWENS_INDISPONIBLE);
        assertThat(transactionsEnregistrees).isEmpty();
    }

    @Test
    void sans_jeton_la_session_est_expiree_sans_appel_powens() {
        for (String absent : new String[] {null, " "}) {
            jeton = absent;
            ecritures.clear();
            noncesConsommes.clear();

            cas.traiter(parametres("connection_id", "1", "state", "valide"));

            assertThat(ecritures).containsExactly("message:10:" + TraiterRetourPowensCasUsage.SESSION_EXPIREE);
            assertThat(appelsPowens).isEmpty();
        }
    }

    @Test
    void code_seul_est_un_retour_incomplet_qui_liste_les_parametres_recus_sans_le_state() {
        cas.traiter(parametres("code", "abc", "state", "valide"));

        assertThat(ecritures).containsExactly("message:10:Callback Powens incomplet. Parametres recus: [code]");
        assertThat(appelsPowens).isEmpty();
    }

    @Test
    void les_parametres_vides_comptent_comme_absents_et_l_ordre_recu_est_garde() {
        cas.traiter(parametres("error", "", "connection_id", " ", "code", "", "state", "valide"));
        assertThat(ecritures).containsExactly("message:10:Callback Powens incomplet. Parametres recus: []");

        ecritures.clear();
        noncesConsommes.clear();
        cas.traiter(parametres("zeta", "1", "state", "valide", "alpha", "2"));
        assertThat(ecritures).containsExactly("message:10:Callback Powens incomplet. Parametres recus: [zeta, alpha]");
        assertThat(appelsPowens).isEmpty();
    }
}
