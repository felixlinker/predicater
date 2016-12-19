package de.felixlinker.predicater;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.net.URL;

/**
 * This class stores an undirected, loop-free graph with multiple types of edges. Each node can have metadata.
 * @param <T> Type of the nodes' metadata.
 */
public class Document<T> {

    private static final String META_ATTR = "doc.meta";
    private static final String STYLE_ATTR = "ui.stylesheet";
    private static final String RENDERER_ATTR = "org.graphstream.ui.renderer";
    private static final String RENDERER = "org.graphstream.ui.j2dviewer.J2DGraphRenderer";
    private static final URL STYLE_SHEET = Document.class.getClassLoader().getResource("graph-style.css");

    static {
        System.setProperty(RENDERER_ATTR, RENDERER);
    }

    final Graph g;

    final Graph displayGraph;

    private Viewer graphViewer;

    /**
     * Creates a window that displays the graph. Only one type of edges can be displayed at once.
     */
    public Document display() {
        if (this.graphViewer == null) {
            this.graphViewer = this.displayGraph.display();
        }

        return this;
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
        this.displayGraph = new SingleGraph(name + Integer.toString(name.hashCode()));
        this.displayGraph.setAttribute(STYLE_ATTR, "url(" + STYLE_SHEET.toString() + ")");
    }

    /**
     * Adds nodes to the graph.
     * @param nodeId Id of the node to add.
     * @param metaData Meta data to be assigned to the node.
     * @return This document for chain invocation.
     * @throws IdAlreadyInUseException Thrown if any of the given node's id already exists.
     */
    public Document addNode(String nodeId, T metaData) throws IdAlreadyInUseException {
        if (this.g.getNode(nodeId) != null) {
            throw new IdAlreadyInUseException();
        }

        this.g.addNode(nodeId).setAttribute(META_ATTR, metaData);
        this.displayGraph.addNode(nodeId).setAttribute(META_ATTR, metaData);

        return this;
    }

    /**
     * Removes a node from the document.
     * @param nodeId Id of the node to remove.
     * @return {@code true} if the node could be removed, false if it didn't existed.
     */
    public boolean removeNode(String nodeId) {
        try {
            if (this.g.removeNode(nodeId) != null) {
                this.displayGraph.removeNode(nodeId);
                return true;
            }

            return false;
        } catch (ElementNotFoundException e) {
            return false;
        }
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
        }

        e.setAttribute(predicate);
        if (predicate.equals(this.displayedPredicate)) {
            this.displayGraph.addEdge(edgeId, subject, object);
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
    public boolean removePredicate(String subject, String predicate, String object) throws IllegalArgumentException {
        Node from = this.g.getNode(subject),
                to = this.g.getNode(object);

        if (from == null || to == null) {
            throw new IllegalArgumentException();
        }

        String edgeId = composeEdgeId(subject, object);

        Edge e = this.g.getEdge(edgeId);
        if (e == null) {
            return false;
        }

        boolean wasPredicated = e.hasAttribute(predicate);
        e.removeAttribute(predicate);
        if (wasPredicated && predicate.equals(this.displayedPredicate)) {
            this.displayGraph.removeEdge(edgeId);
        }

        return wasPredicated;
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
        this.displayGraph.getEachEdge().forEach(edge -> {
            if (!this.g.getEdge(edge.getId()).hasAttribute(predicate)) {
                this.displayGraph.removeEdge(edge);
            }
        });

        for (Edge e: this.g.getEachEdge()) {
            if (e.hasAttribute(predicate) && this.displayGraph.getEdge(e.getId()) == null) {
                this.displayGraph.addEdge(e.getId(), e.getNode0().getId(), e.getNode1().getId());
            }
        }
        this.displayedPredicate = predicate;
    }

    /**
     * Returns the document's name set in constructor.
     * @return Document name.
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

        this.displayGraph.read(fileName);
        this.showPredicate(this.displayedPredicate);
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
