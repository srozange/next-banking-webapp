package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeSignal;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/** Données communes aux tests de revue : un client 7 avec deux comptes, un administrateur 99, un virement suspendu. */
final class RevueTestes {

    /** Soumission : le 5 novembre 2026 à 10:15 Paris ; butoir : le 6 à 10:15. */
    static final Instant SOUMIS = Instant.parse("2026-11-05T09:15:00Z");
    static final Instant BUTOIR = Instant.parse("2026-11-06T09:15:00Z");
    static final SeuilsFraude SEUILS =
            new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000"));
    static final Set<Role> ADMIN = Set.of(Role.ADMIN);
    static final String IP = "203.0.113.9";

    private RevueTestes() {
    }

    /** Mémoire avec les comptes 1 (20 000,00) et 2 (100,00) du client 7 ; l'horloge de la mémoire est à {@code SOUMIS}. */
    static MemoireCoeurBancaire memoire() {
        MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("100.00")));
        memoire.noms.put(7L, "Claire Martin");
        return memoire;
    }

    /** Virement suspendu 12 de 12 000,00 avec deux alertes OPEN et une alerte CONFIRMED. */
    static Virement suspendu(MemoireCoeurBancaire memoire) {
        Virement virement = new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("12000.00"), Motif.lire("loyer"), EtatVirement.SUSPENDU,
                StatutAffichage.PENDING, SOUMIS, BUTOIR, "Score cumulé 130 - signaux : LARGE_AMOUNT");
        memoire.virements.put(12L, virement);
        memoire.alertes.add(alerte(StatutAlerte.OPEN, TypeSignal.LARGE_AMOUNT));
        memoire.alertes.add(alerte(StatutAlerte.OPEN, TypeSignal.NEW_BENEFICIARY));
        memoire.alertes.add(alerte(StatutAlerte.CONFIRMED, TypeSignal.RAPID_SUCCESSION));
        return virement;
    }

    static AlerteFraude alerte(StatutAlerte statut, TypeSignal regle) {
        return new AlerteFraude(7, 12, regle, 40, "description", statut, SOUMIS);
    }
}
