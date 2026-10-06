package fr.nextbanking.identiteclients.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée du journal d'audit : identifiant d'événement, utilisateur ({@code clientId} en texte pour une connexion
 * réussie, identifiant saisi pour une tentative échouée), code d'action, adresse IP d'origine (absente pour une
 * action du système) et horodatage lu sur le port Horloge. Le détail découle du code d'action.
 */
public record EntreeAudit(UUID idEvenement, String utilisateur, ActionAudit action, String adresseIp,
        Instant horodatage, String detail) {

    /**
     * Longueur maximale de l'identifiant saisi repris dans une tentative échouée : celle de {@code customers.username}.
     * L'identifiant d'un compte réel reste intact ; un identifiant démesuré (plusieurs Mo) deviendrait sinon la clé et
     * la charge d'un message plus gros que {@code max.request.size}, jamais publiable, qui bloquerait le relais
     * de l'outbox d'audit et toutes les entrées suivantes.
     */
    public static final int LONGUEUR_MAX_IDENTIFIANT_SAISI = 255;

    /** Entrée dont le détail découle du code d'action. */
    public EntreeAudit(UUID idEvenement, String utilisateur, ActionAudit action, String adresseIp,
            Instant horodatage) {
        this(idEvenement, utilisateur, action, adresseIp, horodatage, action == null ? null : action.detail());
    }

    public EntreeAudit {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement d'audit est obligatoire.");
        }
        if (utilisateur == null || utilisateur.isBlank()) {
            throw new IllegalArgumentException("L'utilisateur de l'entrée d'audit est obligatoire.");
        }
        if (action == null) {
            throw new IllegalArgumentException("Le code d'action de l'entrée d'audit est obligatoire.");
        }
        if (horodatage == null) {
            throw new IllegalArgumentException("L'horodatage de l'entrée d'audit est obligatoire.");
        }
    }

    /** Désactivation d'un client par un administrateur : utilisateur = identifiant de l'administrateur. */
    public static EntreeAudit desactivationClient(long administrateurId, long clientId, String adresseIp,
            UUID idEvenement, Instant horodatage) {
        return new EntreeAudit(idEvenement, Long.toString(administrateurId), ActionAudit.ADMIN_DEACTIVATE, adresseIp,
                horodatage, ActionAudit.ADMIN_DEACTIVATE.detail(clientId));
    }

    /** Activation d'un client par un administrateur : utilisateur = identifiant de l'administrateur. */
    public static EntreeAudit activationClient(long administrateurId, long clientId, String adresseIp,
            UUID idEvenement, Instant horodatage) {
        return new EntreeAudit(idEvenement, Long.toString(administrateurId), ActionAudit.ADMIN_ACTIVATE, adresseIp,
                horodatage, ActionAudit.ADMIN_ACTIVATE.detail(clientId));
    }

    /** Connexion réussie : l'utilisateur est l'identifiant numérique du client, en texte. */
    public static EntreeAudit connexionReussie(long clientId, String adresseIp, UUID idEvenement, Instant horodatage) {
        return new EntreeAudit(idEvenement, Long.toString(clientId), ActionAudit.LOGIN, adresseIp, horodatage);
    }

    /** Déconnexion : l'utilisateur est l'identifiant numérique du client, en texte. */
    public static EntreeAudit deconnexion(long clientId, String adresseIp, UUID idEvenement, Instant horodatage) {
        return new EntreeAudit(idEvenement, Long.toString(clientId), ActionAudit.LOGOUT, adresseIp, horodatage);
    }

    /**
     * Tentative échouée : l'utilisateur est l'identifiant saisi ; au-delà de {@link #LONGUEUR_MAX_IDENTIFIANT_SAISI}
     * caractères, il est rogné puis tronqué à cette longueur.
     */
    public static EntreeAudit tentativeEchouee(String identifiantSaisi, String adresseIp, UUID idEvenement,
            Instant horodatage) {
        String utilisateur = identifiantSaisi;
        if (utilisateur != null && utilisateur.length() > LONGUEUR_MAX_IDENTIFIANT_SAISI) {
            // Rogné avant la troncature : des blancs en tête ne doivent pas laisser un utilisateur blanc.
            String rogne = utilisateur.strip();
            utilisateur = rogne.length() > LONGUEUR_MAX_IDENTIFIANT_SAISI
                    ? rogne.substring(0, LONGUEUR_MAX_IDENTIFIANT_SAISI)
                    : rogne;
        }
        return new EntreeAudit(idEvenement, utilisateur, ActionAudit.LOGIN_FAILED, adresseIp, horodatage);
    }
}
