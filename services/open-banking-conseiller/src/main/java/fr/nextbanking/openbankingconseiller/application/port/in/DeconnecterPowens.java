package fr.nextbanking.openbankingconseiller.application.port.in;

/** Cas d'usage : se déconnecter de Powens, tout l'état du client est remis à zéro (FEAT-012). */
public interface DeconnecterPowens {

    void deconnecter(String sub);
}
