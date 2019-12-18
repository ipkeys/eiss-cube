package eiss.jwt;

public class ExpiredTokenException extends Exception {
    ExpiredTokenException(String reason) {
        super(reason);
    }
}
