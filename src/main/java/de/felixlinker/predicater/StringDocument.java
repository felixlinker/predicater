package de.felixlinker.predicater;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;

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
    void setMetadata(Element element, String metadata) {
        super.setMetadata(element, metadata);
        element.addAttribute(LABEL_ATTR, metadata);
    }

    public void setNodeLabel(String nodeId, String label) throws IllegalArgumentException {
        Node node = super.g.getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException();
        }

        setMetadata(node, label);
    }

    public void setEdgeLabel(String from, String predicate, String to, String label) throws IllegalArgumentException {
        Edge edge = super.g.getEdge(getEdgeIdBetweenNodes(from, predicate, to));
        if (edge == null) {
            throw new IllegalArgumentException();
        }

        setMetadata(edge, label);
    }
}
