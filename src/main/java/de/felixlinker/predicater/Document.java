package de.felixlinker.predicater;

import de.felixlinker.predicater.exceptions.NodeException;
import de.felixlinker.predicater.exceptions.RegardException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;
import java.util.stream.Stream;

public class Document<T> {

    private HashMap<String, T> nodeMap = new HashMap<>();

    private void checkNode(String identifier, boolean checkForPresence) throws NodeException {
        if (this.nodeMap.containsKey(identifier) != checkForPresence) {
            throw new NodeException(identifier);
        }
    }

    private HashMap<String, List<Pair<String, String>>> regards = new HashMap<>();

    private void checkRegard(String regard, boolean checkForPresence) throws RegardException {
        if (this.regards.containsKey(regard) != checkForPresence) {
            throw new RegardException(regard);
        }
    }

    public Document addNodes(Pair<String, T>... nodes) throws KeyAlreadyExistsException {
        this.addNodes(Arrays.asList(nodes));
        return this;
    }

    public Document addNodes(Collection<Pair<String, T>> nodes) throws NodeException {
        LinkedList<Pair<String, T>> newNodes = new LinkedList<>();

        nodes.forEach(node -> {
            checkNode(node.getKey(), false);
            newNodes.add(node);
        });

        newNodes.forEach(node -> this.nodeMap.put(node.getKey(), node.getValue()));

        return this;
    }

    public Document addRegard(String regard) throws RegardException {
        checkRegard(regard, false);

        this.regards.put(regard, new LinkedList<>());

        return this;
    }

    public Document addPredicate(String subject, String predicate, String object) throws NodeException, RegardException {
        for (String identifier: new String[]{ subject, object }) {
           checkNode(identifier, true);
        }

        checkRegard(predicate, true);

        this.regards.get(predicate).add(new ImmutablePair<>(subject, object));

        return this;
    }

    public Stream<String> linksFrom(String subject, String predicate) throws NodeException, RegardException {
        checkNode(subject, true);
        checkRegard(predicate, true);

        return this.regards.get(predicate).stream()
                .filter(pair -> pair.getKey().equals(subject))
                .map(Pair::getKey);
    }

    public Stream<String> linksTo(String predicate, String object) throws NodeException, RegardException {
        checkRegard(predicate, true);
        checkNode(object, true);

        return this.regards.get(predicate).stream()
                .filter(pair -> pair.getValue().equals(object))
                .map(Pair::getValue);
    }

    public Stream<T> mapValues(Stream<String> identifierList) {
        return identifierList.map(this.nodeMap::get);
    }
}
