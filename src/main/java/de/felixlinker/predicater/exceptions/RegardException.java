package de.felixlinker.predicater.exceptions;

public class RegardException extends IdentifiableException {

    public RegardException(String identifier) {
        super(identifier);
    }

    public RegardException(String identifier, String s) {
        super(identifier, s);
    }

    public RegardException(String identifier, Throwable cause) {
        super(identifier, cause);
    }

    public RegardException(String identifier, String s, Throwable cause) {
        super(identifier, s, cause);
    }
}
