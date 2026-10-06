package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.SituationPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Sélection, rafraîchissement et déconnexion, avec un dépôt et un Powens en mémoire. */
@Tag("FR-028-07")
@Tag("FR-028-08")
@Tag("FR-028-09")
@Tag("FR-028-10")
@Tag("FR-014-07")
@Tag("FR-014-08")
@Tag("BUG-043")
@Tag("BUG-089")
class GestionComptesExternesTest {

    private static final String SUB = "10";

    private EtatPowens etat;
    private List<CompteExterne> comptes;
    private Long selection;
    private List<TransactionExterne> transactions;
    private int ecritures;

    private List<CompteExterne> lusParPowens;
    private List<TransactionExterne> transactionsPowens;
    private RuntimeException pannePowens;
    private final List<String> appelsPowens = new ArrayList<>();

    private final DepotEtatPowens depotEtat = new DepotEtatPowens() {
        @Override
        public Optional<EtatPowens> lire(String sub) {
            return Optional.ofNullable(etat);
        }

        @Override
        public void enregistrerJeton(String sub, String jeton) {
            throw new AssertionError();
        }

        @Override
        public void enregistrerState(StatePowens state) {
            throw new AssertionError();
        }

        @Override
        public boolean consommerState(StatePowens state, Instant maintenant) {
            throw new AssertionError();
        }

        @Override
        public void remettreAZero(String sub) {
            ecritures++;
            etat = null;
            comptes = List.of();
            selection = null;
            transactions = List.of();
        }

        @Override
        public void poserMessage(String sub, String message) {
            throw new AssertionError();
        }

        @Override
        public void enregistrerConnexion(String sub, List<CompteExterne> c, Long s, List<TransactionExterne> t,
                String message) {
            ecritures++;
            etat = new EtatPowens(etat.jeton(), true, message);
            comptes = c;
            selection = s;
            transactions = t;
        }
    };

    private final DepotComptesExternes depotComptes = new DepotComptesExternes() {
        @Override
        public List<CompteExterne> comptes(String sub) {
            return comptes;
        }

        @Override
        public Optional<Long> compteSelectionne(String sub) {
            return Optional.ofNullable(selection);
        }

        @Override
        public List<TransactionExterne> transactions(String sub) {
            return transactions;
        }

        @Override
        public boolean appartient(String sub, long compteId) {
            return comptes.stream().anyMatch(c -> c.id() == compteId);
        }

        @Override
        public void enregistrerSelection(String sub, long compteId, List<TransactionExterne> t) {
            ecritures++;
            selection = compteId;
            transactions = t;
        }
    };

    private final Powens powens = new Powens() {
        @Override
        public UtilisateurPowens creerUtilisateur() {
            throw new AssertionError();
        }

        @Override
        public String codeTemporaire(String jeton) {
            throw new AssertionError();
        }

        @Override
        public String adresseConnexion(String code, String state) {
            throw new AssertionError();
        }

        @Override
        public List<CompteExterne> comptes(String jeton) {
            appelsPowens.add("comptes");
            if (pannePowens != null) {
                throw pannePowens;
            }
            return lusParPowens;
        }

        @Override
        public List<TransactionExterne> transactions(String jeton, long compteId) {
            appelsPowens.add("transactions:" + compteId);
            if (pannePowens != null) {
                throw pannePowens;
            }
            return transactionsPowens;
        }
    };

    private final ConsulterEtatPowensCasUsage consulter = new ConsulterEtatPowensCasUsage(depotEtat, depotComptes);

    private static CompteExterne compte(long id) {
        return new CompteExterne(id, "c" + id, "checking", new BigDecimal("10.00"), null);
    }

    private static TransactionExterne transaction(long id) {
        return new TransactionExterne(id, LocalDate.of(2026, 10, 1), "t" + id, new BigDecimal("1.00"));
    }

    @BeforeEach
    void connecte() {
        etat = new EtatPowens("jeton", true, "message en cours");
        comptes = List.of(compte(101), compte(102));
        selection = 101L;
        transactions = List.of(transaction(1));
        lusParPowens = List.of(compte(101), compte(102));
        transactionsPowens = List.of(transaction(7));
    }

    @Test
    void la_selection_lit_vingt_transactions_au_plus_et_garde_le_message() {
        transactionsPowens = IntStream.rangeClosed(1, 25).mapToObj(GestionComptesExternesTest::transaction).toList();

        SituationPowens situation = new SelectionnerCompteExterneCasUsage(powens, depotEtat, depotComptes, consulter)
                .selectionner(SUB, 102);

        assertThat(situation.selectionne()).isEqualTo(102L);
        assertThat(situation.transactions()).hasSize(20);
        assertThat(situation.etat().message()).isEqualTo("message en cours");
        assertThat(appelsPowens).containsExactly("transactions:102");
    }

    @Test
    void la_selection_d_un_compte_inconnu_leve_une_erreur_sans_appel_ni_ecriture() {
        assertThatThrownBy(() -> new SelectionnerCompteExterneCasUsage(powens, depotEtat, depotComptes, consulter)
                .selectionner(SUB, 999)).isInstanceOf(CompteExterneIntrouvable.class);

        assertThat(appelsPowens).isEmpty();
        assertThat(ecritures).isZero();
    }

    @Test
    void la_selection_en_panne_laisse_l_etat_inchange() {
        pannePowens = new PowensIndisponible("statut 500");

        assertThatThrownBy(() -> new SelectionnerCompteExterneCasUsage(powens, depotEtat, depotComptes, consulter)
                .selectionner(SUB, 102)).isInstanceOf(PowensIndisponible.class);

        assertThat(ecritures).isZero();
        assertThat(selection).isEqualTo(101L);
        assertThat(transactions).hasSize(1);
    }

    @Test
    void le_rafraichissement_garde_le_compte_selectionne_s_il_existe_et_efface_le_message() {
        selection = 102L;

        SituationPowens situation = new RafraichirComptesExternesCasUsage(powens, depotEtat, depotComptes, consulter)
                .rafraichir(SUB);

        assertThat(situation.selectionne()).isEqualTo(102L);
        assertThat(situation.etat().message()).isNull();
        assertThat(situation.transactions()).extracting(TransactionExterne::id).containsExactly(7L);
        assertThat(appelsPowens).containsExactly("comptes", "transactions:102");
    }

    @Test
    void le_rafraichissement_prend_le_premier_compte_si_la_selection_a_disparu() {
        selection = 999L;
        lusParPowens = List.of(compte(201), compte(202));

        SituationPowens situation = new RafraichirComptesExternesCasUsage(powens, depotEtat, depotComptes, consulter)
                .rafraichir(SUB);

        assertThat(situation.selectionne()).isEqualTo(201L);
        assertThat(situation.comptes()).hasSize(2);
    }

    @Test
    void le_rafraichissement_sans_compte_pose_le_message_aucun_compte() {
        lusParPowens = List.of();

        SituationPowens situation = new RafraichirComptesExternesCasUsage(powens, depotEtat, depotComptes, consulter)
                .rafraichir(SUB);

        assertThat(situation.selectionne()).isNull();
        assertThat(situation.etat().message()).isEqualTo(TraiterRetourPowensCasUsage.AUCUN_COMPTE);
    }

    @Test
    void le_rafraichissement_en_panne_laisse_l_etat_inchange() {
        pannePowens = new PowensIndisponible("statut 503");

        assertThatThrownBy(() -> new RafraichirComptesExternesCasUsage(powens, depotEtat, depotComptes, consulter)
                .rafraichir(SUB)).isInstanceOf(PowensIndisponible.class);

        assertThat(ecritures).isZero();
        assertThat(comptes).hasSize(2);
    }

    @Test
    void la_panne_des_transactions_au_rafraichissement_n_ecrit_rien() {
        pannePowens = null;
        transactionsPowens = null;
        Powens pannePartielle = new Powens() {
            @Override
            public UtilisateurPowens creerUtilisateur() {
                throw new AssertionError();
            }

            @Override
            public String codeTemporaire(String jeton) {
                throw new AssertionError();
            }

            @Override
            public String adresseConnexion(String code, String state) {
                throw new AssertionError();
            }

            @Override
            public List<CompteExterne> comptes(String jeton) {
                return List.of(compte(301));
            }

            @Override
            public List<TransactionExterne> transactions(String jeton, long compteId) {
                throw new PowensIndisponible("statut 404");
            }
        };

        assertThatThrownBy(() -> new RafraichirComptesExternesCasUsage(pannePartielle, depotEtat, depotComptes,
                consulter).rafraichir(SUB)).isInstanceOf(PowensIndisponible.class);

        assertThat(ecritures).isZero();
        assertThat(comptes).extracting(CompteExterne::id).containsExactly(101L, 102L);
    }

    @Test
    void le_rafraichissement_sans_jeton_leve_une_erreur_sans_appel_powens() {
        for (EtatPowens sansJeton : new EtatPowens[] {null, new EtatPowens(null, false, null),
                new EtatPowens(" ", false, null)}) {
            etat = sansJeton;

            assertThatThrownBy(() -> new RafraichirComptesExternesCasUsage(powens, depotEtat, depotComptes,
                    consulter).rafraichir(SUB)).isInstanceOf(PowensNonConnecte.class);
        }
        assertThat(appelsPowens).isEmpty();
    }

    @Test
    void la_selection_sans_jeton_leve_une_erreur_sans_appel_powens() {
        for (EtatPowens sansJeton : new EtatPowens[] {null, new EtatPowens(null, true, null),
                new EtatPowens(" ", true, null)}) {
            etat = sansJeton;

            assertThatThrownBy(() -> new SelectionnerCompteExterneCasUsage(powens, depotEtat, depotComptes,
                    consulter).selectionner(SUB, 102)).isInstanceOf(PowensNonConnecte.class);
        }
        assertThat(appelsPowens).isEmpty();
        assertThat(ecritures).isZero();
    }

    @Test
    void le_rafraichissement_sans_selection_prend_le_premier_compte() {
        selection = null;

        SituationPowens situation = new RafraichirComptesExternesCasUsage(powens, depotEtat, depotComptes, consulter)
                .rafraichir(SUB);

        assertThat(situation.selectionne()).isEqualTo(101L);
    }

    @Test
    void la_deconnexion_remet_tout_a_zero() {
        new DeconnecterPowensCasUsage(depotEtat).deconnecter(SUB);

        SituationPowens situation = consulter.consulter(SUB);
        assertThat(situation).isEqualTo(SituationPowens.vide());
        assertThat(appelsPowens).isEmpty();
    }
}
