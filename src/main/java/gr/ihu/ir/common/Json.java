package gr.ihu.ir.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Ένας κοινός Jackson {@link ObjectMapper} για όλο το project.
 *
 * ΓΙΑΤΙ Jackson και όχι «χειροποίητο» JSON: το κείμενο των PDF περιέχει εισαγωγικά,
 * backslash, αλλαγές γραμμής κ.λπ. Η Jackson κάνει σωστό escaping (π.χ. το \n γίνεται
 * οι δύο χαρακτήρες '\' 'n'), άρα κάθε έγγραφο χωράει εγγυημένα σε ΜΙΑ γραμμή.
 *
 * Ο ObjectMapper είναι thread-safe μετά τη ρύθμισή του, γι' αυτό αρκεί ένας.
 */
public final class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            // Αν ένας client στείλει επιπλέον πεδία που δεν ξέρουμε, δεν «σκάμε».
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {
    }
}
