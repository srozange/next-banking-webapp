package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.ConsulterPortefeuille;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.domain.ClientEtSesPrets;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cas d'usage : exige CONSEILLER ou ADMIN par {@link DroitsAdministrateur}, lit dans la projection locale les clients
 * non supprimés affectés à l'acteur, puis leurs prêts en une seule lecture, dans l'ordre du dépôt (date de début
 * décroissante, identifiant décroissant). Un client sans prêt figure avec une liste vide ; un ADMIN sans client
 * affecté reçoit une liste vide.
 */
public class ConsulterPortefeuilleCasUsage implements ConsulterPortefeuille {

    private final ProjectionClients projection;
    private final PretRepository prets;

    public ConsulterPortefeuilleCasUsage(ProjectionClients projection, PretRepository prets) {
        this.projection = projection;
        this.prets = prets;
    }

    @Override
    public List<ClientEtSesPrets> consulter(long acteurId, Set<Role> rolesActeur) {
        DroitsAdministrateur.exigerConseillerOuAdmin(rolesActeur);
        List<Long> clients = projection.clientsDuConseiller(acteurId).stream().sorted().toList();
        if (clients.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Pret>> parClient = prets.desClients(clients).stream()
                .collect(Collectors.groupingBy(Pret::clientId));
        return clients.stream()
                .map(clientId -> new ClientEtSesPrets(clientId, parClient.getOrDefault(clientId, List.of())))
                .toList();
    }
}
