package tonelotti.dev.meujiu.common.helpers;

import java.util.Locale;

public final class HelperEmail {

    private HelperEmail() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
