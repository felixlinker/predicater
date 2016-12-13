package de.felixlinker.predicater;

import org.apache.commons.lang3.tuple.Pair;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.Arrays;
import java.util.Collection;

public class Document<T> {

    private static final String META_ATTR = "doc.meta";
    private static final String HIDE_ATTR = "ui.hide";

    private final Graph g;

    private String displayedPredicate;

    /*private boolean strict = true;

    public void setStrict(boolean strict) {
        this.g.setStrict(strict);
        this.g.setAutoCreate(!strict);
        this.strict = strict;
    }

    public boolean getStrict() {
        return this.strict;
    }*/

    public Document(String name) {
        this.g = new SingleGraph(name);
        this.g.display();
    }

    public Document addNodes(Pair<String, T>... nodes) /*throws KeyAlreadyExistsException*/ {
        this.addNodes(Arrays.asList(nodes));
        return this;
    }

    public Document addNodes(Collection<Pair<String, T>> nodes) throws IdAlreadyInUseException {
//        if (this.strict) {
            nodes.forEach(node -> {
                if (this.g.getNode(node.getKey()) == null) {
                    throw new IdAlreadyInUseException();
                }
            });
//        }

        nodes.forEach(node -> this.g.addNode(node.getKey()).setAttribute(META_ATTR, node.getValue()));

        return this;
    }

    public Document addPredicate(String subject, String predicate, String object) throws IllegalArgumentException {
        Node from = this.g.getNode(subject),
                to = this.g.getNode(object);

        if (from == null || to == null) {
            throw new IllegalArgumentException();
        }

        String edgeId = composeEdgeId(subject, object);
        Edge e = this.g.getEdge(edgeId);
        if (e == null) {
            e = this.g.addEdge(edgeId, from, to);
            e.setAttribute(HIDE_ATTR);
        }

        e.setAttribute(predicate);
        if (predicate.equals(this.displayedPredicate)) {
            e.removeAttribute(HIDE_ATTR);
        }

        return this;
    }

    public boolean isPredicated(String subject, String predicate, String object) {
        Edge e = this.g.getEdge(composeEdgeId(subject, object));

        return e != null && (Boolean) e.getAttribute(predicate);
    }

    public void display(String predicate) {
        for (Edge e: this.g.getEachEdge()) {
            if (e.getAttribute(predicate)) {
                e.removeAttribute(HIDE_ATTR);
            } else {
                e.setAttribute(HIDE_ATTR);
            }
        }
    }

    private static String composeEdgeId(String node1Id, String node2Id) {
        if (node1Id.compareTo(node2Id) > 0) {
            return node1Id + node2Id;
        } else {
            return node2Id + node1Id;
        }
    }
}
