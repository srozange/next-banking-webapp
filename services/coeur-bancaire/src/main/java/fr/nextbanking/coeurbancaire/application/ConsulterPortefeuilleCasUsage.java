package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterPortefeuille;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.domain.ClientEtSesComptes;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cas d'usage : exige CONSEILLER ou ADMIN par {@link DecisionAdministrateur}, lit dans la projection locale les clients
 * non supprimés affectés à l'acteur, puis leurs comptes en une seule lecture. Un client sans compte figure avec une
 * liste vide ; un ADMIN sans client affecté reçoit une liste vide.
 */
public class ConsulterPortefeuilleCasUsage implements ConsulterPortefeuille {

    private final ProjectionClients projection;
    private final Comptes comptes;

    public ConsulterPortefeuilleCasUsage(ProjectionClients projection, Comptes comptes) {
        this.projection = projection;
        this.comptes = comptes;
    }

    @Override
    public List<ClientEtSesComptes> consulter(long acteurId, Set<Role> rolesActeur) {
        DecisionAdministrateur.exigerConseillerOuAdmin(rolesActeur);
        List<Long> clients = projection.clientsDuConseiller(acteurId).stream().sorted().toList();
        if (clients.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Compte>> parClient = comptes.desClients(clients).stream()
                .sorted(Comparator.comparingLong(Compte::id))
                .collect(Collectors.groupingBy(Compte::clientId));
        return clients.stream()
                .map(clientId -> new ClientEtSesComptes(clientId, parClient.getOrDefault(clientId, List.of())))
                .toList();
    }
}
