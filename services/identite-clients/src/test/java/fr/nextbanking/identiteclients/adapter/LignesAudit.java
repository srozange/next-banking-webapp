package fr.nextbanking.identiteclients.adapter;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture de la table {@code outbox_audit} dans les tests d'intégration.
 */
public final class LignesAudit {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Ligne lue : clé du message, enveloppe JSON et état de publication. */
    public record Ligne(long id, String cle, JsonNode enveloppe, boolean publie) {

        public JsonNode donnees() {
            return enveloppe.get("donnees");
        }
    }

    private LignesAudit() {
    }

    /** Dernier identifiant de ligne (0 si la table est vide). */
    public static long dernierId(JdbcClient jdbc) {
        return jdbc.sql("select coalesce(max(id), 0) from outbox_audit").query(Long.class).single();
    }

    /** Lignes écrites après l'identifiant donné, dans l'ordre. */
    public static List<Ligne> depuis(JdbcClient jdbc, long id) {
        return jdbc.sql("select id, cle, charge, publie from outbox_audit where id > :id order by id")
                .param("id", id)
                .query((ligne, numero) -> new Ligne(ligne.getLong("id"), ligne.getString("cle"),
                        JSON.readTree(ligne.getString("charge")), ligne.getBoolean("publie")))
                .list();
    }
}
