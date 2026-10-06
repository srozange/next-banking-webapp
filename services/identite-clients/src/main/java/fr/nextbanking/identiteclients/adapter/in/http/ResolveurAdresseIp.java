package fr.nextbanking.identiteclients.adapter.in.http;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Résout l'adresse IP d'origine d'une requête, une seule fois, dans l'adaptateur entrant HTTP : le premier élément
 * de {@code X-Forwarded-For} (rogné) n'est retenu que si l'adresse distante appartient à une plage de confiance
 * ({@code nb.reseau.mandataires-de-confiance}, CIDR séparés par des virgules) et s'il est un littéral IPv4 ou IPv6 ;
 * sinon l'adresse distante est retenue. Aucune résolution DNS. Une plage absente ou illisible empêche le démarrage.
 */
@Component
public class ResolveurAdresseIp {

    static final String PROPRIETE = "nb.reseau.mandataires-de-confiance";
    static final String EN_TETE = "X-Forwarded-For";

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final Pattern IPV4 = Pattern.compile("^" + OCTET + "(\\." + OCTET + "){3}$");
    // Premier caractère hexadécimal ou « : » : InetAddress traite alors la chaîne comme un littéral, jamais comme un
    // nom d'hôte (« .: », par exemple, partirait en requête DNS).
    private static final Pattern CARACTERES_IPV6 = Pattern.compile("^[0-9a-fA-F:][0-9a-fA-F:.]*$");

    private final List<Plage> plages;

    @Autowired
    public ResolveurAdresseIp(@Value("${" + PROPRIETE + ":}") String plages) {
        this.plages = lirePlages(plages);
    }

    /** Adresse d'origine de la requête. */
    public String resoudre(HttpServletRequest requete) {
        return resoudre(requete.getRemoteAddr(), requete.getHeader(EN_TETE));
    }

    /** Adresse d'origine, à partir de l'adresse distante et de l'en-tête {@code X-Forwarded-For} (éventuellement absent). */
    String resoudre(String adresseDistante, String enTete) {
        if (enTete == null || !deConfiance(adresseDistante)) {
            return adresseDistante;
        }
        String premier = enTete.split(",", -1)[0].trim();
        return litteral(premier).isPresent() ? premier : adresseDistante;
    }

    private boolean deConfiance(String adresseDistante) {
        Optional<byte[]> octets = litteral(adresseDistante);
        return octets.isPresent() && plages.stream().anyMatch(plage -> plage.contient(octets.get()));
    }

    static List<Plage> lirePlages(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("La propriété " + PROPRIETE
                    + " est obligatoire (variable d'environnement NB_RESEAU_MANDATAIRES_DE_CONFIANCE).");
        }
        List<Plage> plages = new ArrayList<>();
        for (String element : valeur.split(",", -1)) {
            plages.add(lirePlage(element.trim()));
        }
        return List.copyOf(plages);
    }

    private static Plage lirePlage(String cidr) {
        int barre = cidr.indexOf('/');
        Optional<byte[]> adresse = barre < 0 ? Optional.empty() : litteral(cidr.substring(0, barre));
        if (adresse.isEmpty() || !cidr.substring(barre + 1).matches("\\d{1,3}")) {
            throw plageIllisible(cidr);
        }
        int prefixe = Integer.parseInt(cidr.substring(barre + 1));
        if (prefixe > adresse.get().length * 8) {
            throw plageIllisible(cidr);
        }
        return new Plage(adresse.get(), prefixe);
    }

    private static IllegalStateException plageIllisible(String cidr) {
        return new IllegalStateException("La propriété " + PROPRIETE + " contient une plage CIDR illisible : " + cidr);
    }

    /**
     * Octets d'un littéral IPv4 (4 octets) ou IPv6 (16 octets, ou 4 pour une adresse IPv4 transposée) ; vide pour
     * toute autre chaîne. {@link InetAddress#getByName} n'est appelé que sur une chaîne qui ne peut pas être un nom
     * d'hôte (au moins un « : », premier caractère hexadécimal ou « : ») : aucune requête DNS n'est possible.
     */
    static Optional<byte[]> litteral(String texte) {
        if (texte == null) {
            return Optional.empty();
        }
        if (!IPV4.matcher(texte).matches()
                && !(texte.indexOf(':') >= 0 && CARACTERES_IPV6.matcher(texte).matches())) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(texte).getAddress());
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    /** Plage CIDR : adresse de base et longueur du préfixe, en bits. */
    record Plage(byte[] base, int prefixe) {

        /** Vrai si l'adresse, de même famille, partage les {@code prefixe} premiers bits de la base. */
        boolean contient(byte[] adresse) {
            if (adresse.length != base.length) {
                return false;
            }
            for (int bit = 0; bit < prefixe; bit++) {
                int masque = 0x80 >>> (bit % 8);
                if ((adresse[bit / 8] & masque) != (base[bit / 8] & masque)) {
                    return false;
                }
            }
            return true;
        }
    }
}
