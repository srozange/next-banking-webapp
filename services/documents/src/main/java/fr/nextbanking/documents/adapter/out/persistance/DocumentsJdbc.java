package fr.nextbanking.documents.adapter.out.persistance;

import fr.nextbanking.documents.application.port.out.Documents;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.TypeDocument;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code documents}. Une seule instruction crée la ligne (client, type, paramètres) à la
 * première génération puis incrémente son compteur de téléchargements, sans course entre deux téléchargements
 * simultanés. L'empreinte du contenu est un SHA-256 (le legacy employait MD5).
 */
@Component
public class DocumentsJdbc implements Documents {

    private final JdbcClient jdbc;

    public DocumentsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void compterTelechargement(long clientId, TypeDocument type, String parametres, DocumentPdf document,
            Instant instant) {
        jdbc.sql("""
                insert into documents (customer_id, document_type, filename, mime_type, file_size_bytes,
                                       generated_at, download_count, checksum, parametres)
                values (:clientId, :type, :nomFichier, 'application/pdf', :taille, :instant, 1, :empreinte,
                        :parametres)
                on conflict (customer_id, document_type, parametres)
                do update set download_count = documents.download_count + 1""")
                .param("clientId", clientId)
                .param("type", type.name())
                .param("nomFichier", document.nomFichier())
                .param("taille", document.contenu().length)
                .param("instant", java.sql.Timestamp.from(instant))
                .param("empreinte", sha256(document.contenu()))
                .param("parametres", parametres)
                .update();
    }

    static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est garanti par toute JVM", e);
        }
    }
}
