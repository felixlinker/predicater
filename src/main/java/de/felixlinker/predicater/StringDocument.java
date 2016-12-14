package de.felixlinker.predicater;

import org.apache.commons.lang3.tuple.Pair;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;

import java.util.Collection;

/**
 * An implementation of {@link Document} for strings. When the document is displayed, the metadata will be the node's label.
 * Metadata is of type {@link String}.
 */
public class StringDocument extends Document<String> {

    public StringDocument(String name) {
        super(name);
    }

    @Override
    public Document addNodes(Collection<Pair<String, String>> nodes) throws IdAlreadyInUseException {
        nodes.forEach(node -> {
            if (this.g.getNode(node.getKey()) != null) {
                throw new IdAlreadyInUseException();
            }
        });

        nodes.forEach(node -> {
            Node n = this.g.addNode(node.getKey());
            n.setAttribute(META_ATTR, node.getValue());
            n.setAttribute(LABEL_ATTR, node.getValue());
        });

        return this;
    }
}
