package fr.nextbanking.bourse.application;

import fr.nextbanking.bourse.application.port.in.ConsulterHistorique;
import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.application.port.out.Horloge;
import fr.nextbanking.bourse.domain.HistoriqueCours;
import fr.nextbanking.bourse.domain.Periode;
import fr.nextbanking.bourse.domain.SymboleManquant;

/**
 * Demande la série de la période au fournisseur, puis garde les points depuis la date limite, triés par date
 * croissante ; la date du jour vient de l'Horloge (Europe/Paris).
 */
public class ConsulterHistoriqueCasUsage implements ConsulterHistorique {

    private final Cotations cotations;
    private final Horloge horloge;

    public ConsulterHistoriqueCasUsage(Cotations cotations, Horloge horloge) {
        this.cotations = cotations;
        this.horloge = horloge;
    }

    @Override
    public HistoriqueCours consulter(String symbole, String periode) {
        if (symbole == null || symbole.isBlank()) {
            throw new SymboleManquant();
        }
        String rogne = symbole.trim();
        Periode retenue = Periode.depuis(periode);
        return new HistoriqueCours(rogne, retenue,
                retenue.retenir(cotations.serie(rogne, retenue.serie()), horloge.aujourdhui()));
    }
}
