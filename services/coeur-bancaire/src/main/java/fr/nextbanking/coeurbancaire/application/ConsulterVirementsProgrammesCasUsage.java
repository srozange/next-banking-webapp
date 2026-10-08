package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterVirementsProgrammes;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cas d'usage : virements programmés du client authentifié (FR-004-03), par prochaine exécution croissante, avec le
 * numéro du compte source et la prochaine exécution en date du fuseau métier.
 */
public class ConsulterVirementsProgrammesCasUsage implements ConsulterVirementsProgrammes {

    private final VirementsProgrammes virements;
    private final Comptes comptes;
    private final Horloge horloge;

    public ConsulterVirementsProgrammesCasUsage(VirementsProgrammes virements, Comptes comptes, Horloge horloge) {
        this.virements = virements;
        this.comptes = comptes;
        this.horloge = horloge;
    }

    @Override
    public List<LigneVirementProgramme> duClient(long clientId) {
        Map<Long, String> numeros = new HashMap<>();
        for (Compte compte : comptes.duClient(clientId)) {
            numeros.put(compte.id(), compte.numero());
        }
        return virements.listerDuClient(clientId).stream()
                .map(v -> ligne(v, numeros.getOrDefault(v.compteSourceId(), ""), horloge)).toList();
    }

    static LigneVirementProgramme ligne(VirementProgramme v, String numeroSource, Horloge horloge) {
        return new LigneVirementProgramme(v.id(), v.compteSourceId(), numeroSource, v.numeroBeneficiaire(),
                v.libelleBeneficiaire(), v.montant(), v.frequence(), v.motif(),
                v.prochaineExecution().atZone(horloge.fuseau()).toLocalDate(), v.actif(), v.nbExecutions());
    }
}
