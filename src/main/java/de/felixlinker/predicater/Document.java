package de.felixlinker.predicater;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * This class stores an undirected, loop-free graph with multiple types of edges. Each node can have metadata.
 * @param <T> Type of the nodes' metadata.
 */
public class Document<T> {

    private static final String META_ATTR = "doc.meta";
    private static final String STYLE_ATTR = "ui.stylesheet";
    private static final String PRED_ATTR = "predicater.edgetypes";
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

        String edgeId = subject + object;
        Edge edge = this.g.getEdge(edgeId);
        if (edge == null) {
            edge = this.g.addEdge(edgeId, from, to, true);
        }

        List<String> predicates = getPredicatesList(edge);
        if (!predicates.contains(predicate)) {
            predicates.add(predicate);

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
    public Document removePredicate(String subject, String predicate, String object) throws IllegalArgumentException {
        Node from = this.g.getNode(subject),
                to = this.g.getNode(object);

        if (from == null || to == null) {
            throw new IllegalArgumentException();
        }

        String edgeId = subject + object;

        Edge edge = this.g.getEdge(edgeId);
        if (edge == null) {
            return this;
        }

        boolean removed = getPredicatesList(edge).remove(predicate);
        if (removed && predicate.equals(this.displayedPredicate)) {
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
        Edge edge = this.g.getEdge(subject + object);

        return edge != null && getPredicatesList(edge).contains(predicate);
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
            if (!getPredicatesList(this.g.getEdge(edge.getId())).contains(predicate)) {
                this.displayGraph.removeEdge(edge);
            }
        });

        this.g.getEachEdge().forEach(edge -> {
            if (getPredicatesList(edge).contains(predicate) && this.displayGraph.getEdge(edge.getId()) == null) {
                this.displayGraph.addEdge(edge.getId(), edge.getNode0().getId(), edge.getNode1().getId(), true);
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
        this.g.getEachEdge().forEach(edge -> predicates.addAll(getPredicatesList(edge)));
        return predicates;
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

    private static List<String> getPredicatesList(Edge edge) {
        List<String> predicateList = edge.getAttribute(PRED_ATTR, List.class);
        if (predicateList != null) {
            return predicateList;
        }

        String[] predicatesArray = (String[]) edge.getArray(PRED_ATTR);
        if (predicatesArray != null) {
            predicateList = Arrays.asList(predicatesArray);
        } else {
            String predicate = edge.getAttribute(PRED_ATTR, String.class);
            predicateList = new LinkedList<>();
            if (predicate != null) {
                predicateList.add(predicate);
            }
        }

        edge.addAttribute(PRED_ATTR, predicateList);
        return predicateList;
    }
}
