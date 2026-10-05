package fr.nextbanking.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ControleMotDePasseBaseTest {

    private final ControleMotDePasseBase controle = new ControleMotDePasseBase();

    @Test
    void accepte_un_mot_de_passe_renseigne() {
        controle.setEnvironment(new MockEnvironment().withProperty("spring.datasource.password", "secret"));

        assertThatCode(() -> controle.postProcessBeanFactory(null)).doesNotThrowAnyException();
    }

    @Test
    void refuse_un_mot_de_passe_absent() {
        controle.setEnvironment(new MockEnvironment());

        assertThatThrownBy(() -> controle.postProcessBeanFactory(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password");
    }

    @Test
    void refuse_un_mot_de_passe_vide() {
        controle.setEnvironment(new MockEnvironment().withProperty("spring.datasource.password", "  "));

        assertThatThrownBy(() -> controle.postProcessBeanFactory(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
