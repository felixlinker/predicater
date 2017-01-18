package de.felixlinker.predicater;

import org.graphstream.graph.Edge;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;

/**
 * An implementation of {@link Document} for strings. When the document is displayed, the metadata will be the node's label.
 * Metadata is of type {@link String}.
 */
public class StringDocument extends Document<String> {

    private static final String LABEL_ATTR = "ui.label";
    private static final String EDGE_LABEL_REGEX = ":";

    public StringDocument(String name) {
        super(name);
    }

    @Override
    public Document addNode(String nodeId, String metaData) throws IdAlreadyInUseException {
        super.addNode(nodeId, metaData);
        this.g.getNode(nodeId).setAttribute(LABEL_ATTR, metaData);
        this.displayGraph.getNode(nodeId).setAttribute(LABEL_ATTR, metaData);
        return this;
    }

    @Override
    public Document addPredicate(String subject, String predicate, String object) throws IllegalArgumentException {
        String[] split = predicate.split(EDGE_LABEL_REGEX);
        String label = null;
        if (split.length > 1) {
            predicate = split[0];
            label = split[1];
        }

        super.addPredicate(subject, predicate, object);

        if (label != null) {
            labelEdge(subject, object, label);
        }

        return this;
    }

    public void labelNode(String nodeId, String label) {
        Node node = super.g.getEdge(nodeId);
        if (node == null) {
            return;
        }

        node.addAttribute(LABEL_ATTR, label);
    }

    public void labelEdge(String node1, String node2, String label) {
        Edge edge = super.g.getEdge(getEdgeIdBetweenNodes(node1, node2));
        if (edge == null) {
            return;
        }

        edge.addAttribute(LABEL_ATTR, label);
    }
}
