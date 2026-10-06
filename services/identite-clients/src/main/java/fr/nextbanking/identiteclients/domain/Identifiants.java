package fr.nextbanking.identiteclients.domain;

/**
 * Identifiants saisis à la connexion. L'identifiant est contrôlé avant le mot de passe ; un identifiant vide,
 * blanc ou absent et un mot de passe vide ou absent sont refusés. Le mot de passe n'apparaît jamais dans
 * {@link #toString()}.
 */
public record Identifiants(String identifiant, String motDePasse) {

    public Identifiants {
        if (identifiant == null || identifiant.isBlank()) {
            throw new ConnexionRefusee(ConnexionRefusee.Motif.IDENTIFIANT_REQUIS);
        }
        if (motDePasse == null || motDePasse.isEmpty()) {
            throw new ConnexionRefusee(ConnexionRefusee.Motif.MOT_DE_PASSE_REQUIS);
        }
    }

    @Override
    public String toString() {
        return "Identifiants[identifiant=" + identifiant + ", motDePasse=****]";
    }
}
