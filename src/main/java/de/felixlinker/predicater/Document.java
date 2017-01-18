package de.felixlinker.predicater;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class stores an undirected, loop-free graph with multiple types of edges. Each node can have metadata.
 * @param <T> Type of the nodes' metadata.
 */
public class Document<T> {

    private static final String META_ATTR = "doc.meta";
    private static final String STYLE_ATTR = "ui.stylesheet";
    private static final String PRED_ATTR = "predicater.edgetype";
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
        this.g = new MultiGraph(name);
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
    public Document predicate(String subject, String predicate, String object) throws IllegalArgumentException {
        Node from = this.g.getNode(subject),
                to = this.g.getNode(object);

        if (from == null || to == null) {
            throw new IllegalArgumentException();
        }

        String edgeId = getEdgeIdBetweenNodes(subject, predicate, object);
        Edge edge = this.g.getEdge(edgeId);
        if (edge == null) {
            edge = this.g.addEdge(edgeId, from, to, true);
            edge.addAttribute(PRED_ATTR, predicate);

            if (predicate.equals(this.displayedPredicate)) {
                this.displayGraph.addEdge(edgeId, subject, object, true);
            }
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
    public Document unpredicate(String subject, String predicate, String object) throws IllegalArgumentException {

        if (this.g.getNode(subject) == null || this.g.getNode(object) == null) {
            throw new IllegalArgumentException();
        }

        String edgeId = getEdgeIdBetweenNodes(subject, predicate, object);

        Edge removed = this.g.removeEdge(edgeId);
        if (removed != null && predicate.equals(this.displayedPredicate)) {
            this.displayGraph.removeEdge(edgeId);
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
        return this.g.getEdge(getEdgeIdBetweenNodes(subject, predicate, object)) != null;
    }

    /**
     * Sets the graph display to show edges of given type.
     * @param predicate Edge type to show.
     */
    public void showPredicate(String predicate) {
        if (predicate == null) {
            return;
        }

        this.displayGraph.getEachEdge().forEach(edge -> {

            if (!this.g.getEdge(edge.getId()).getAttribute(PRED_ATTR, String.class).equals(predicate)) {
                this.displayGraph.removeEdge(edge);
            }
        });

        this.g.getEachEdge().forEach(edge -> {
            if (edge.getAttribute(PRED_ATTR, String.class).equals(predicate) && this.displayGraph.getEdge(edge.getId()) == null) {

                Edge newEdge = this.displayGraph.addEdge(edge.getId(), edge.getNode0().getId(), edge.getNode1().getId(), true);

                Map<String, Object> attributeMap = edge.getAttributeKeySet().stream()
                        .collect(Collectors.toMap(
                                x -> x,
                                x -> edge.getAttribute(x, Object.class)
                        ));
                newEdge.addAttributes(attributeMap);
            }
        });

        this.displayedPredicate = predicate;
    }

    /**
     * Returns the document's name set in constructor.
     * @return Document name.
     */
    public String getName() {
        return this.g.getId();
    }

    public Set<String> getPredicates() {
        HashSet<String> predicates = new HashSet<>();
        this.g.getEachEdge().forEach(edge -> predicates.add(edge.getAttribute(PRED_ATTR, String.class)));
        return predicates;
    }

    void mirrorElement(Edge edge) {
        Edge otherEdge = this.displayGraph.getEdge(edge.getId());
        if (otherEdge == null) {
            otherEdge = this.displayGraph.addEdge(edge.getId(), edge.getNode0().getId(), edge.getNode1(), true);
        }

        mirrorAttributes(edge, otherEdge);
    }

    void mirrorElement(Node node) {
        Node otherNode = this.displayGraph.getNode(node.getId());
        if (otherNode == null) {
            otherNode = this.displayGraph.addNode(node.getId());
        }

        mirrorAttributes(node, otherNode);
    }

    void mirrorAttributes(Element from, Element to) {
        Map<String, Object> attributeMap = from.getAttributeKeySet().stream()
                .collect(Collectors.toMap(
                        attributeKey -> attributeKey,
                        attributeKey -> from.getAttribute(attributeKey, Object.class)
                ));

        to.addAttributes(attributeMap);
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
        this.displayGraph.getEachEdge().forEach(this.displayGraph::removeEdge);
        this.showPredicate(this.getPredicates().stream().findFirst().get());
    }

    /**
     * Writes the graph into a given file (extension should be .dgs).
     * @param fileName File to write to.
     * @throws IOException
     */
    public void write(String fileName) throws IOException {
        this.g.write(fileName);
    }

    static String getEdgeIdBetweenNodes(String fromNode, String predicate, String toNode) {
        return fromNode + "::" + predicate + "::" + toNode;
    }
}
