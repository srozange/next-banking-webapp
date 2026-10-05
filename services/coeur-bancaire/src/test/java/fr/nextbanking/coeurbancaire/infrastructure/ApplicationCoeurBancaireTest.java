package fr.nextbanking.coeurbancaire.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.in.PointEntreeTemoin;
import fr.nextbanking.coeurbancaire.adapter.out.DepotTemoinsEnMemoire;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApplicationCoeurBancaire.class)
class ApplicationCoeurBancaireTest {

    @Autowired
    private PointEntreeTemoin pointEntree;

    @Autowired
    private DepotTemoinsEnMemoire depot;

    @Test
    void le_contexte_assemble_le_point_d_entree_jusqu_au_depot() {
        Temoin temoin = pointEntree.ecrire("premier témoin");

        assertThat(depot.tous()).contains(temoin);
    }
}
