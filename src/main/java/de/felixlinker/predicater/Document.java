package de.felixlinker.predicater;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.AttributeSink;
import org.graphstream.stream.ElementSink;
import org.graphstream.stream.GraphParseException;
import org.graphstream.ui.view.Viewer;

import java.io.IOException;
import java.net.URL;
import java.util.*;
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

    private static final boolean STRICT_MODE = false;
    private static final boolean AUTO_CREATE = false;

    /**
     * Set the renderer to allow advanced rendering features.
     */
    static {
        System.setProperty(RENDERER_ATTR, RENDERER);
    }

    /**
     * The graph that actual stores all nodes and edges.
     */
    final Graph g;

    /**
     * The graph that reflects all nodes and those edges that will be displayed.
     */
    private final Graph displayGraph;

    /**
     * Holds the viewer that displays the graph.
     */
    private Viewer graphViewer;

    boolean edgesAreDirected = true;

    /**
     * Creates a window that displays the graph. Only one type of edges can be displayed at once.
     */
    public Document display() {
        if (this.graphViewer == null) {
            this.graphViewer = this.displayGraph.display();
        }

        return this;
    }

    private final HashSet<String> displayedPredicates = new HashSet<>();

    /**
     * Creates a document with given name.
     * @param name Unique name for the document.
     */
    public Document(String name) {
        this.g = new MultiGraph(name, STRICT_MODE, AUTO_CREATE);
        this.g.addElementSink(new DisplayGraphElementSink());
        this.g.addAttributeSink(new DisplayGraphAttributeSink());

        this.displayGraph = new MultiGraph(name + Integer.toString(name.hashCode()), STRICT_MODE, AUTO_CREATE);
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

        Node newNode = this.g.addNode(nodeId);
        setMetadata(newNode, metaData);

        return this;
    }

    /**
     * Removes a node from the document.
     * @param nodeId Id of the node to remove.
     * @return {@code true} if the node could be removed, false if it didn't existed.
     */
    public boolean removeNode(String nodeId) {
        return this.g.removeNode(nodeId) != null;
    }

    /**
     * Adds an edge between two nodes.
     * @param subject Node one's id.
     * @param predicate Edge type.
     * @param object Node two's id.
     * @return This document for chain invocation.
     * @throws IllegalArgumentException Thrown if any of the given nodes doesn't exist.
     * @throws IdAlreadyInUseException Thrown if an edge between given nodes and of given type already exists.
     */
    public Document predicate(String subject, String predicate, String object, T metaData) throws IllegalArgumentException, IdAlreadyInUseException {
        Node from = this.g.getNode(subject),
                to = this.g.getNode(object);

        if (from == null || to == null) {
            throw new IllegalArgumentException("At least one of the given nodes does not exist.");
        }

        String edgeId = getEdgeIdBetweenNodes(subject, predicate, object);
        if (this.g.getEdge(edgeId) != null) {
            throw new IdAlreadyInUseException("This edge already exists");
        }

        Edge edge = this.g.addEdge(edgeId, from, to, this.edgesAreDirected);
        edge.addAttribute(PRED_ATTR, predicate);
        setMetadata(edge, metaData);

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
            throw new IllegalArgumentException("At least one of the given nodes does not exist.");
        }

        this.g.removeEdge(getEdgeIdBetweenNodes(subject, predicate, object));

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
     * Edges of given type will be displayed.
     * @param predicate Edge type to show.
     */
    public void showPredicate(String predicate) {
        if (predicate == null || displayedPredicates.contains(predicate)) {
            return;
        }

        this.displayedPredicates.add(predicate);

        this.g.getEachEdge().forEach(edge -> {
            if (edge.getAttribute(PRED_ATTR, String.class).equals(predicate)) {

                Edge newEdge = this.displayGraph.addEdge(edge.getId(), edge.getNode0().getId(), edge.getNode1().getId(), this.edgesAreDirected);

                Map<String, Object> attributeMap = edge.getAttributeKeySet().stream()
                        .collect(Collectors.toMap(
                                x -> x,
                                x -> edge.getAttribute(x, Object.class)
                        ));
                newEdge.addAttributes(attributeMap);
            }
        });
    }

    /**
     * Edges of given type will be hidden.
     * @param predicate Edge type to hide.
     */
    public void hidePredicate(String predicate) {
        if (predicate == null || !this.displayedPredicates.contains(predicate)) {
            return;
        }

        this.displayedPredicates.remove(predicate);

        Iterator<Edge> iterator = this.displayGraph.getEdgeIterator();
        while (iterator.hasNext()) {
            Edge edge = iterator.next();
            if (edge.getAttribute(PRED_ATTR, String.class).equals(predicate)) {
                iterator.remove();
            }
        }
    }

    /**
     * Returns the document's name set in constructor.
     * @return Document name.
     */
    public String getName() {
        return this.g.getId();
    }

    /**
     * Returns a set of all edge types.
     * @return All edge types.
     */
    public Set<String> getPredicates() {
        HashSet<String> predicates = new HashSet<>();
        this.g.getEachEdge().forEach(edge -> predicates.add(edge.getAttribute(PRED_ATTR, String.class)));
        return predicates;
    }

    /**
     * Sets the metadata to an element of the graph.
     * @param element Element ot set metadata to.
     * @param metadata Metadata.
     */
    void setMetadata(Element element, T metadata) {
        element.addAttribute(META_ATTR, metadata);
    }

    /**
     * Fills the document with a graph of the given file (extension should be .dgs).
     * @param fileName File to read from.
     * @throws IOException See {@link MultiGraph#read(String)}.
     * @throws GraphParseException See {@link MultiGraph#read(String)}.
     */
    public void read(String fileName) throws IOException, GraphParseException {
        this.g.read(fileName);
    }

    /**
     * Writes the graph into a given file (extension should be .dgs).
     * @param fileName File to write to.
     * @throws IOException See {@link MultiGraph#write(String)}.
     */
    public void write(String fileName) throws IOException {
        this.g.write(fileName);
    }

    /**
     * Returns an edge id for stated edge.
     * @param fromNode Node the edge starts.
     * @param predicate Edge type.
     * @param toNode Node the edge ends.
     * @return Edge id to use in {@link #g} and {@link #displayGraph}.
     */
    static String getEdgeIdBetweenNodes(String fromNode, String predicate, String toNode) {
        return fromNode + "::" + predicate + "::" + toNode;
    }

    /**
     * This class mirrors all activity {@link #g} to {@link #displayGraph} when necessary.
     */
    private class DisplayGraphElementSink implements ElementSink {

        @Override
        public void nodeAdded(String sourceId, long timeId, String nodeId) {
            displayGraph.addNode(nodeId);
        }

        @Override
        public void nodeRemoved(String sourceId, long timeId, String nodeId) {
            displayGraph.removeNode(nodeId);
        }

        @Override
        public void edgeAdded(String sourceId, long timeId, String edgeId, String fromNodeId, String toNodeId, boolean directed) {
            // When an edge is added it won't have any attributes. We only want to add edges that have tha attribute PRED_ATTR set to displayedPredicate. Therefore we don't add edges here.
        }

        @Override
        public void edgeRemoved(String sourceId, long timeId, String edgeId) {
            displayGraph.removeEdge(edgeId);
        }

        @Override
        public void graphCleared(String sourceId, long timeId) {
            displayGraph.clear();
        }

        @Override
        public void stepBegins(String sourceId, long timeId, double step) {}
    }

    /**
     * This class mirrors all activity on {@link #g} to {@link #displayGraph} when necessary.
     */
    private class DisplayGraphAttributeSink implements AttributeSink {

        @Override
        public void graphAttributeAdded(String sourceId, long timeId, String attribute, Object value) {
            displayGraph.addAttribute(attribute, value);
        }

        @Override
        public void graphAttributeChanged(String sourceId, long timeId, String attribute, Object oldValue, Object newValue) {
            displayGraph.addAttribute(attribute, newValue);
        }

        @Override
        public void graphAttributeRemoved(String sourceId, long timeId, String attribute) {
            displayGraph.removeAttribute(attribute);
        }

        @Override
        public void nodeAttributeAdded(String sourceId, long timeId, String nodeId, String attribute, Object value) {
            displayGraph.getNode(nodeId).addAttribute(attribute, value);
        }

        @Override
        public void nodeAttributeChanged(String sourceId, long timeId, String nodeId, String attribute, Object oldValue, Object newValue) {
            displayGraph.getNode(nodeId).addAttribute(attribute, newValue);
        }

        @Override
        public void nodeAttributeRemoved(String sourceId, long timeId, String nodeId, String attribute) {
            displayGraph.getNode(nodeId).removeAttribute(attribute);
        }

        @Override
        public void edgeAttributeAdded(String sourceId, long timeId, String edgeId, String attribute, Object value) {
            Edge edge = displayGraph.getEdge(edgeId);

            if (edge == null) {
                if (attribute.equals(PRED_ATTR) && displayedPredicates.contains(value)) {
                    Edge orgEdge = g.getEdge(edgeId);
                    edge = displayGraph.addEdge(edgeId, orgEdge.getSourceNode().getId(), orgEdge.getTargetNode().getId(), orgEdge.isDirected());
                } else {
                    return;
                }
            }

            edge.addAttribute(attribute, value);
        }

        @Override
        public void edgeAttributeChanged(String sourceId, long timeId, String edgeId, String attribute, Object oldValue, Object newValue) {
            Edge edge = displayGraph.getEdge(edgeId);
            if (edge == null) {
                if (attribute.equals(PRED_ATTR) && displayedPredicates.contains(newValue)) {
                    Edge orgEdge = g.getEdge(edgeId);
                    edge = displayGraph.addEdge(edgeId, orgEdge.getSourceNode().getId(), orgEdge.getTargetNode().getId(), orgEdge.isDirected());
                } else {
                    return;
                }
            } else if (attribute.equals(PRED_ATTR) && !displayedPredicates.contains(newValue)) {
                displayGraph.removeEdge(edgeId);
                return;
            }

            edge.addAttribute(attribute, newValue);
        }

        @Override
        public void edgeAttributeRemoved(String sourceId, long timeId, String edgeId, String attribute) {
            Edge edge = displayGraph.getEdge(edgeId);
            if (edge == null) {
                return;
            } else if (attribute.equals(PRED_ATTR)) {
                displayGraph.removeEdge(edgeId);
                return;
            }

            edge.removeAttribute(attribute);
        }
    }
}
