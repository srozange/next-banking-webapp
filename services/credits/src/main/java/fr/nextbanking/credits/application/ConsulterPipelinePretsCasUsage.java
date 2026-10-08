package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.ConsulterPipelinePrets;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretAvecClient;
import fr.nextbanking.credits.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cas d'usage : pipeline des prêts EN_ATTENTE et historique de tous les prêts pour l'administrateur, avec le nom
 * complet du client lu dans la projection locale.
 */
public class ConsulterPipelinePretsCasUsage implements ConsulterPipelinePrets {

    private final PretRepository prets;
    private final ProjectionClients clients;

    public ConsulterPipelinePretsCasUsage(PretRepository prets, ProjectionClients clients) {
        this.prets = prets;
        this.clients = clients;
    }

    @Override
    public List<PretAvecClient> enAttente(Set<Role> roles) {
        DroitsAdministrateur.exigerAdmin(roles);
        return avecClients(prets.enAttente());
    }

    @Override
    public List<PretAvecClient> historique(Set<Role> roles) {
        DroitsAdministrateur.exigerAdmin(roles);
        return avecClients(prets.tous());
    }

    private List<PretAvecClient> avecClients(List<Pret> liste) {
        if (liste.isEmpty()) {
            return List.of();
        }
        Map<Long, String> noms = clients.nomsComplets(liste.stream().map(Pret::clientId).distinct().toList());
        return liste.stream().map(pret -> PretAvecClient.de(pret, noms.get(pret.clientId()))).toList();
    }
}
