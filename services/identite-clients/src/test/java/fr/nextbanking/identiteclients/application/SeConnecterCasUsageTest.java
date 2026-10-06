package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.ActionAudit;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.ConnexionRefusee;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
import fr.nextbanking.identiteclients.domain.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SeConnecterCasUsageTest {

    private static final String BON = "bon-mot-de-passe";
    private static final String IP = "203.0.113.7";
    private static final Instant MAINTENANT = Instant.parse("2026-10-05T08:30:00Z");

    private final Map<String, Client> clients = new HashMap<>();
    private final List<String> comparaisons = new ArrayList<>();
    private final List<Client> jetonsEmis = new ArrayList<>();
    private final List<String> deroule = new ArrayList<>();
    private final List<EntreeAudit> consignees = new ArrayList<>();
    private final List<Long> publiees = new ArrayList<>();
    private boolean annulerApresLeTravail;

    private final DepotClients depot = new DepotClients() {
        @Override
        public Optional<Client> parIdentifiant(String identifiant) {
            deroule.add("recherche");
            return Optional.ofNullable(clients.get(identifiant));
        }

        @Override
        public Optional<Client> parId(long clientId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void changerActif(long clientId, boolean actif) {
            throw new UnsupportedOperationException();
        }
    };

    // Empreinte de test : "h:" + mot de passe ; une empreinte absente ne correspond jamais.
    private final MotsDePasse motsDePasse = (motDePasse, empreinte) -> {
        comparaisons.add(empreinte.orElse("factice"));
        return empreinte.map(e -> e.equals("h:" + motDePasse)).orElse(false);
    };

    private final EmetteurJeton emetteur = client -> {
        jetonsEmis.add(client);
        deroule.add("jeton");
        return "jeton-" + client.id();
    };

    private final JournalAudit journal = new JournalAudit() {
        @Override
        public long consigner(EntreeAudit entree) {
            deroule.add("consigner");
            consignees.add(entree);
            return 40L + consignees.size();
        }

        @Override
        public void publier(long idLigne) {
            deroule.add("publier " + idLigne);
            publiees.add(idLigne);
        }
    };

    // Double du port : trace l'ouverture et la validation ; peut annuler la transaction après le travail.
    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            deroule.add("debut");
            T resultat = travail.get();
            if (annulerApresLeTravail) {
                deroule.add("annulation");
                throw new IllegalStateException("annulation voulue");
            }
            deroule.add("validation");
            return resultat;
        }

        @Override
        public <T> T executerIndependamment(Supplier<T> travail) {
            deroule.add("debut independante");
            T resultat = travail.get();
            deroule.add("validation independante");
            return resultat;
        }
    };

    private final Horloge horloge = new Horloge() {
        @Override
        public Instant instant() {
            return MAINTENANT;
        }

        @Override
        public LocalDate aujourdhui() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ZoneId fuseau() {
            throw new UnsupportedOperationException();
        }
    };

    private final SeConnecterCasUsage casUsage =
            new SeConnecterCasUsage(depot, motsDePasse, emetteur, journal, transaction, horloge);

    SeConnecterCasUsageTest() {
        clients.put("user1", new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, "h:" + BON));
        clients.put("inactif", new Client(5L, "inactif", "Compte Désactivé", Role.CLIENT, false, "h:" + BON));
    }

    @Test
    @Tag("FR-001-04")
    void connecte_un_client_actif_aux_bons_identifiants() {
        SeConnecter.Connexion connexion = casUsage.seConnecter("user1", BON, IP);

        assertThat(connexion.clientId()).isEqualTo(1L);
        assertThat(connexion.jeton()).isEqualTo("jeton-1");
        assertThat(jetonsEmis).extracting(Client::identifiant).containsExactly("user1");
    }

    @Test
    @Tag("FR-001-04")
    void ne_montre_jamais_le_jeton() {
        assertThat(casUsage.seConnecter("user1", BON, IP).toString()).contains("clientId=1").doesNotContain("jeton-1");
    }

    @Test
    @Tag("FR-001-12")
    @Tag("FR-026-01")
    @Tag("FR-026-05")
    void une_connexion_reussie_est_auditee_login_avec_le_client_id_et_l_adresse_ip() {
        casUsage.seConnecter("user1", BON, IP);

        assertThat(consignees).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.LOGIN);
            assertThat(entree.utilisateur()).isEqualTo("1");
            assertThat(entree.detail()).isEqualTo("Connexion réussie");
            assertThat(entree.adresseIp()).isEqualTo(IP);
            assertThat(entree.horodatage()).isEqualTo(MAINTENANT);
            assertThat(entree.idEvenement()).isNotNull();
        });
        assertThat(deroule).containsExactly("debut", "recherche", "debut independante", "consigner",
                "validation independante", "validation", "publier 41", "jeton");
    }

    @Test
    @Tag("FR-001-12")
    @Tag("FR-026-01")
    void deux_connexions_ont_deux_identifiants_d_evenement() {
        casUsage.seConnecter("user1", BON, IP);
        casUsage.seConnecter("user1", BON, IP);

        assertThat(consignees).extracting(EntreeAudit::idEvenement).doesNotHaveDuplicates().hasSize(2);
    }

    @Test
    @Tag("FR-001-06")
    @Tag("FR-001-12")
    @Tag("FR-026-01")
    void refuse_un_mauvais_mot_de_passe_et_l_audite_login_failed_avec_l_identifiant_saisi() {
        assertRefus(() -> casUsage.seConnecter("user1", "mauvais", IP), ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);

        assertThat(jetonsEmis).isEmpty();
        assertTentativeEchouee("user1");
        assertThat(publiees).containsExactly(41L);
    }

    @Test
    @Tag("FR-001-06")
    @Tag("FR-001-12")
    @Tag("FR-026-01")
    void refuse_un_identifiant_inconnu_apres_avoir_quand_meme_compare_le_mot_de_passe() {
        assertRefus(() -> casUsage.seConnecter("retro-compte-inexistant", "retro-mauvais-mot-de-passe", IP),
                ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);

        assertThat(comparaisons).containsExactly("factice");
        assertThat(jetonsEmis).isEmpty();
        assertTentativeEchouee("retro-compte-inexistant");
    }

    @Test
    @Tag("FR-001-07")
    @Tag("FR-001-12")
    @Tag("BUG-001")
    void refuse_un_compte_desactive_aux_bons_identifiants_et_l_audite_login_failed() {
        assertRefus(() -> casUsage.seConnecter("inactif", BON, IP), ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);

        assertThat(comparaisons).containsExactly("h:" + BON);
        assertThat(jetonsEmis).isEmpty();
        assertTentativeEchouee("inactif");
    }

    @Test
    @Tag("FR-001-07")
    @Tag("BUG-001")
    void refuse_un_compte_desactive_au_mauvais_mot_de_passe() {
        assertRefus(() -> casUsage.seConnecter("inactif", "mauvais", IP), ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);
    }

    @Test
    @Tag("FR-001-03")
    @Tag("FR-001-12")
    void controle_les_champs_requis_avant_toute_recherche_et_sans_audit() {
        assertRefus(() -> casUsage.seConnecter(" ", BON, IP), ConnexionRefusee.Motif.IDENTIFIANT_REQUIS);
        assertRefus(() -> casUsage.seConnecter("user1", "", IP), ConnexionRefusee.Motif.MOT_DE_PASSE_REQUIS);

        assertThat(comparaisons).isEmpty();
        assertThat(deroule).isEmpty();
    }

    @Test
    @Tag("FR-026-04")
    void l_entree_d_audit_est_consignee_dans_une_transaction_independante_meme_si_la_connexion_est_annulee() {
        annulerApresLeTravail = true;

        assertThatThrownBy(() -> casUsage.seConnecter("user1", BON, IP)).hasMessage("annulation voulue");

        assertThat(consignees).singleElement().extracting(EntreeAudit::action).isEqualTo(ActionAudit.LOGIN);
        assertThat(deroule).containsExactly("debut", "recherche", "debut independante", "consigner",
                "validation independante", "annulation");
        assertThat(jetonsEmis).isEmpty();
    }

    @Test
    @Tag("FR-026-05")
    @Tag("BUG-083")
    void l_adresse_ip_resolue_par_l_adaptateur_est_reprise_telle_quelle() {
        casUsage.seConnecter("user1", BON, "2001:db8::7");
        assertRefus(() -> casUsage.seConnecter("user1", "mauvais", "198.51.100.4"),
                ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);

        assertThat(consignees).extracting(EntreeAudit::adresseIp).containsExactly("2001:db8::7", "198.51.100.4");
    }

    private void assertTentativeEchouee(String identifiantSaisi) {
        assertThat(consignees).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.LOGIN_FAILED);
            assertThat(entree.utilisateur()).isEqualTo(identifiantSaisi);
            assertThat(entree.detail()).isEqualTo("Tentative échouée");
            assertThat(entree.adresseIp()).isEqualTo(IP);
            assertThat(entree.horodatage()).isEqualTo(MAINTENANT);
        });
    }

    private static void assertRefus(Runnable connexion, ConnexionRefusee.Motif motif) {
        assertThatThrownBy(connexion::run).isInstanceOfSatisfying(ConnexionRefusee.class,
                refus -> assertThat(refus.motif()).isEqualTo(motif));
    }
}
