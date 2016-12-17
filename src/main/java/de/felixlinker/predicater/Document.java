package de.felixlinker.predicater;

import org.apache.commons.lang3.tuple.Pair;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class stores an undirected, loop-free graph with multiple types of edges. Each node can have metadata.
 * @param <T> Type of the nodes' metadata.
 */
public class Document<T> {

    private static final String META_ATTR = "doc.meta";
    private static final String HIDE_ATTR = "ui.hide";

    final Graph g;

    private Viewer graphViewer;

    /**
     * Creates a window that displays the graph. Only one type of edges can be displayed at once.
     */
    public void display() {
        if (this.graphViewer == null) {
            this.graphViewer = this.g.display();
        }
    }

    /**
     * Closes the display windows of the graph.
     */
    public void close() {
        if (this.graphViewer != null) {
            this.graphViewer.close();
        }
    }

    private String displayedPredicate;

    public Document(String name) {
        this.g = new SingleGraph(name);
    }

    /**
     * Adds nodes to the graph.
     * @param nodes Each node is a pair as (id, metadata).
     * @return This document for chain invocation.
     * @throws IdAlreadyInUseException Thrown if any of the given node's id already exists.
     */
    public Document addNode(String nodeId, T metaData) throws IdAlreadyInUseException {
        if (this.g.getNode(nodeId) != null) {
            throw new IdAlreadyInUseException();
        }

        this.g.addNode(nodeId).setAttribute(META_ATTR, metaData);

        return this;
    }

    /**
     * Adds an edge between two nodes.
     * @param subject Node one's id.
     * @param predicate Edge type.
     * @param object Node two's id.
     * @return This document for chain invocation.
     * @throws IllegalArgumentException Thrown if any of the given nodes doesn't exist.
     */
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

    /**
     * Removes an edge between two nodes.
     * @param subject Node one's id.
     * @param predicate Edge type.
     * @param object Node two's id.
     * @return This document for chain invocation.
     * @throws IllegalArgumentException Thrown if any of the given nodes doesn't exist.
     */
    public Document removePredicate(String subject, String predicate, String object) throws IllegalArgumentException {
        Node from = this.g.getNode(subject),
                to = this.g.getNode(object);

        if (from == null || to == null) {
            throw new IllegalArgumentException();
        }

        String edgeId = composeEdgeId(subject, object);

        Edge e = this.g.getEdge(edgeId);
        if (e != null) {
            e.removeAttribute(predicate);
            if (predicate.equals(this.displayedPredicate)) {
                e.addAttribute(HIDE_ATTR);
            }
        }

        return this;
    }

    /**
     * Checks whether there is an edge of given type between the nodes.
     * @param subject Node one's id.
     * @param predicate Edge type.
     * @param object Node two's id.
     * @return This document for chain invocation.
     */
    public boolean isPredicated(String subject, String predicate, String object) {
        Edge e = this.g.getEdge(composeEdgeId(subject, object));

        return e != null && (Boolean) e.getAttribute(predicate);
    }

    /**
     * Sets the graph display to show edges of given type.
     * @param predicate Edge type to show.
     */
    public void showPredicate(String predicate) {
        for (Edge e: this.g.getEachEdge()) {
            if (e.hasAttribute(predicate)) {
                e.removeAttribute(HIDE_ATTR);
            } else {
                e.setAttribute(HIDE_ATTR);
            }
        }
        this.displayedPredicate = predicate;
    }

    /**
     * Returns the document's name set in constructor.
     * @return
     */
    public String getName() {
        return this.g.getId();
    }

    /**
     * Fills the document with a graph of the given file (extension should be .dgs).
     * @param fileName File to read from.
     * @throws IOException
     * @throws GraphParseException
     */
    public void read(String fileName) throws IOException, GraphParseException {
        this.g.read(fileName);
    }

    /**
     * Writes the graph into a given file (extension should be .dgs).
     * @param fileName File to write to.
     * @throws IOException
     */
    public void write(String fileName) throws IOException {
        this.g.write(fileName);
    }

    private static String composeEdgeId(String node1Id, String node2Id) {
        if (node1Id.compareTo(node2Id) > 0) {
            return node1Id + node2Id;
        } else {
            return node2Id + node1Id;
        }
    }
}
