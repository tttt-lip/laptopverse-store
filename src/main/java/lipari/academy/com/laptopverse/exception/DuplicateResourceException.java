package lipari.academy.com.laptopverse.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException usernameAlreadyInUse(String username) {
        return new DuplicateResourceException("Username già in uso: " + username);
    }

    public static DuplicateResourceException emailAlreadyInUse(String email) {
        return new DuplicateResourceException("Email già in uso: " + email);
    }
}
