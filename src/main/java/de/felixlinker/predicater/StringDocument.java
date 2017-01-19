package de.felixlinker.predicater;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;

/**
 * An implementation of {@link Document} for strings. When the document is displayed, the metadata will be the node's label.
 * Metadata is of type {@link String}. Nodes and edges will have the metdata-value as label.
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

    /**
     * Sets the node label.
     * @param nodeId Node to label.
     * @param label New node label.
     * @throws IllegalArgumentException Thrown if the node does not exist.
     */
    public void setNodeLabel(String nodeId, String label) throws IllegalArgumentException {
        Node node = super.g.getNode(nodeId);
        if (node == null) {
            throw new IllegalArgumentException();
        }

        setMetadata(node, label);
    }

    /**
     * Sets the edge label.
     * @param from Node the edge starts.
     * @param predicate Edge type.
     * @param to Node the edge ends
     * @param label New edge label.
     * @throws IllegalArgumentException Thrown if the edge does not exist.
     */
    public void setEdgeLabel(String from, String predicate, String to, String label) throws IllegalArgumentException {
        Edge edge = super.g.getEdge(getEdgeIdBetweenNodes(from, predicate, to));
        if (edge == null) {
            throw new IllegalArgumentException();
        }

        setMetadata(edge, label);
    }
}
