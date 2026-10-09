package fr.nextbanking.identiteclients.domain;

import java.util.List;

/**
 * Indicateurs globaux de l'administration (FR-016-02) : nombre de clients, de clients actifs, de conseillers (actifs
 * ou non) et d'utilisateurs de tous rôles.
 */
public record IndicateursUtilisateurs(long totalClients, long clientsActifs, long conseillers,
        long totalUtilisateurs) {

    /** Indicateurs calculés sur les utilisateurs donnés (déjà limités aux non supprimés). */
    public static IndicateursUtilisateurs de(List<FicheUtilisateur> utilisateurs) {
        long clients = utilisateurs.stream().filter(FicheUtilisateur::estClient).count();
        long actifs = utilisateurs.stream().filter(fiche -> fiche.estClient() && fiche.actif()).count();
        long conseillers = utilisateurs.stream().filter(FicheUtilisateur::estConseiller).count();
        return new IndicateursUtilisateurs(clients, actifs, conseillers, utilisateurs.size());
    }
}
