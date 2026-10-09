package fr.nextbanking.documents.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DocumentsJdbcTest {

    @Test
    void l_empreinte_est_un_sha256_en_hexadecimal() {
        assertThat(DocumentsJdbc.sha256("abc".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
