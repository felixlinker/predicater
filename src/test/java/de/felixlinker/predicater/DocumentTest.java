package de.felixlinker.predicater;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graphstream.graph.IdAlreadyInUseException;
import org.junit.Assert;
import org.junit.Test;

public class DocumentTest {

    private Document<String> genDoc(String name) {
        Document<String> d = new Document<>(name);
        d.addNodes(new ImmutablePair<>("A", "A"), new ImmutablePair<>("B", "B"), new ImmutablePair<>("C", "C"));
        d.addPredicate("A", "isGreen", "B");
        d.addPredicate("B", "isBlue", "C");
        d.addPredicate("C", "isYellow", "A");

        return d;
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalPredicateTest() {
        Document<String> d = genDoc("illPredTest");
        d.addPredicate("D", "isGreen", "D");
    }

    @Test(expected = IdAlreadyInUseException.class)
    public void redundantNodeTest() {
        Document<String> d = genDoc("redNodeTest");
        d.addNodes(new ImmutablePair<>("A", "A"));
    }

    @Test
    public void predicateTest() {
        Document<String> d = genDoc("predTest");
        Assert.assertTrue(d.isPredicated("A", "isGreen", "B"));
        Assert.assertTrue(d.isPredicated("B", "isBlue", "C"));
        Assert.assertTrue(d.isPredicated("C", "isYellow", "A"));
    }
}
