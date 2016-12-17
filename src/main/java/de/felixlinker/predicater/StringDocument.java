package de.felixlinker.predicater;

import org.graphstream.graph.IdAlreadyInUseException;

/**
 * An implementation of {@link Document} for strings. When the document is displayed, the metadata will be the node's label.
 * Metadata is of type {@link String}.
 */
public class StringDocument extends Document<String> {

    private static final String LABEL_ATTR = "ui.label";

    public StringDocument(String name) {
        super(name);
    }

    @Override
    public Document addNode(String nodeId, String metaData) throws IdAlreadyInUseException {
        super.addNode(nodeId, metaData);
        this.g.getNode(nodeId).setAttribute(LABEL_ATTR, metaData);
        return this;
    }
}
