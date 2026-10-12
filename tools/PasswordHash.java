import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Run using the application dependency classpath, as documented in README. */
class PasswordHash {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) throw new IllegalStateException("Run in an interactive terminal; do not pass passwords as command arguments.");
        char[] password = console.readPassword("Password: ");
        if (password == null || password.length < 12) throw new IllegalArgumentException("Use at least 12 characters.");
        try { console.printf("%s%n", new BCryptPasswordEncoder(12).encode(new String(password))); }
        finally { Arrays.fill(password, '\0'); }
    }
}
