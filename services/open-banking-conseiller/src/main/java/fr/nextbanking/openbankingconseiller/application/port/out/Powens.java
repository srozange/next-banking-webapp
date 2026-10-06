package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.application.PowensIndisponible;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.util.List;

/**
 * Port sortant vers Powens. Toute panne lève {@link PowensIndisponible}, jamais une liste vide. Tous les appels autres
 * que la création de l'utilisateur s'authentifient avec le jeton permanent.
 */
public interface Powens {

    /** Crée un utilisateur anonyme avec l'identifiant et le secret client (le jeton de la réponse peut manquer). */
    UtilisateurPowens creerUtilisateur();

    /** Code temporaire de connexion, obtenu avec le jeton permanent. */
    String codeTemporaire(String jeton);

    /** Adresse de la page web de Powens (ou, en simulé, du retour) portant le code et le {@code state}. */
    String adresseConnexion(String code, String state);

    /** Comptes externes de l'utilisateur. */
    List<CompteExterne> comptes(String jeton);

    /** Vingt transactions au plus du compte. */
    List<TransactionExterne> transactions(String jeton, long compteId);
}
