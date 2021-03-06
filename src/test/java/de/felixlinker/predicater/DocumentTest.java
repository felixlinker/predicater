package de.felixlinker.predicater;

import org.graphstream.graph.IdAlreadyInUseException;
import org.junit.Assert;
import org.junit.Test;

public class DocumentTest {

    private Document<String> genDoc(String name) {
        Document<String> d = new Document<>(name);
        d.addNode("A", "A").addNode("B", "B").addNode("C", "C");
        d.predicate("A", "isGreen", "B", "");
        d.predicate("B", "isBlue", "C", "");
        d.predicate("C", "isYellow", "A", "");

        return d;
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalPredicateTest() {
        Document<String> d = genDoc("illPredTest");
        d.predicate("D", "isGreen", "D", "");
    }

    @Test(expected = IdAlreadyInUseException.class)
    public void redundantNodeTest() {
        Document<String> d = genDoc("redNodeTest");
        d.addNode("A", "A");
    }

    @Test
    public void predicateTest() {
        Document<String> d = genDoc("predTest");
        Assert.assertTrue(d.isPredicated("A", "isGreen", "B"));
        Assert.assertTrue(d.isPredicated("B", "isBlue", "C"));
        Assert.assertTrue(d.isPredicated("C", "isYellow", "A"));
        d.unpredicate("A", "isGreen", "B");
        Assert.assertFalse(d.isPredicated("A", "isGreen", "B"));
        Assert.assertTrue(d.removeNode("A"));
        Assert.assertFalse(d.removeNode("A"));
    }
}
