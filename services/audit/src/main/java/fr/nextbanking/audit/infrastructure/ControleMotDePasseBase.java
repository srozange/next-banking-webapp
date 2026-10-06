package fr.nextbanking.audit.infrastructure;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Refuse le démarrage sans mot de passe de base ({@code spring.datasource.password}), avant la création
 * de toute {@code DataSource}.
 */
public class ControleMotDePasseBase implements BeanFactoryPostProcessor, EnvironmentAware {

    static final String PROPRIETE = "spring.datasource.password";

    private Environment environnement;

    @Override
    public void setEnvironment(Environment environnement) {
        this.environnement = environnement;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory fabrique) {
        verifier(environnement.getProperty(PROPRIETE));
    }

    static void verifier(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalStateException("La propriété " + PROPRIETE
                    + " est obligatoire (variable d'environnement du mot de passe de la base).");
        }
    }
}
