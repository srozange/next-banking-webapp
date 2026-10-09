package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.ConsulterPortefeuille;
import fr.nextbanking.identiteclients.application.port.out.PortefeuilleClients;
import fr.nextbanking.identiteclients.domain.ClientDuPortefeuille;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Cas d'usage : exige CONSEILLER ou ADMIN par {@link DroitsRoles}, puis lit les clients affectés à l'acteur, triés par
 * nom complet puis identifiant. Un ADMIN sans client affecté reçoit une liste vide.
 */
public class ConsulterPortefeuilleCasUsage implements ConsulterPortefeuille {

    private static final Comparator<ClientDuPortefeuille> ORDRE = Comparator
            .comparing(ClientDuPortefeuille::nomComplet)
            .thenComparingLong(ClientDuPortefeuille::clientId);

    private final PortefeuilleClients portefeuille;

    public ConsulterPortefeuilleCasUsage(PortefeuilleClients portefeuille) {
        this.portefeuille = portefeuille;
    }

    @Override
    public List<ClientDuPortefeuille> consulter(long acteurId, Set<Role> rolesActeur) {
        DroitsRoles.exigerConseillerOuAdmin(rolesActeur);
        return portefeuille.duConseiller(acteurId).stream().sorted(ORDRE).toList();
    }
}
