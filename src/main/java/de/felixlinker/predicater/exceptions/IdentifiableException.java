package de.felixlinker.predicater.exceptions;

public abstract class IdentifiableException extends RuntimeException {

    private final String identifier;

    public String getIdentifier() {
        return this.identifier;
    }

    public IdentifiableException(String identifier) {
        this(identifier, "", null);
    }

    public IdentifiableException(String identifier, String s) {
        this(identifier, s, null);
    }

    public IdentifiableException(String identifier, Throwable cause) {
        this(identifier, "", cause);
    }

    public IdentifiableException(String identifier, String s, Throwable cause) {
        super(s, cause);
        this.identifier = identifier;
    }
}
