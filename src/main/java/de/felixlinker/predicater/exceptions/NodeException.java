package de.felixlinker.predicater.exceptions;

public class NodeException extends IdentifiableException {

    public NodeException(String identifier) {
        super(identifier);
    }

    public NodeException(String identifier, String s) {
        super(identifier, s);
    }

    public NodeException(String identifier, Throwable cause) {
        super(identifier, cause);
    }

    public NodeException(String identifier, String s, Throwable cause) {
        super(identifier, s, cause);
    }
}
