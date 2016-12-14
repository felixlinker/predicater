package de.felixlinker.predicater;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class App {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private final List<StringDocument> documents = new LinkedList<>();

    private StringDocument activeDocument;

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    private Class<? extends Runnable> beanClass = MainWorker.class;

    private boolean stop = false;

    private void run() {
        while (!this.stop) {
            String[] args;
            try {
                args = reader.readLine().split(" ");
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                continue;
            }

            Runnable instantiatedBean;
            try {
                instantiatedBean = beanClass.getConstructor(App.class).newInstance(this);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error(e.getMessage(), e);
                continue;
            }

            try {
                new CmdLineParser(instantiatedBean).parseArgument(args);
            } catch (CmdLineException e) {
                LOGGER.error(e.getMessage(), e);
                continue;
            }

            try {
                instantiatedBean.run();
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static void main(String[] args) {
        new App().run();
    }

    public class MainWorker implements Runnable {

        public MainWorker() {}

        @Option(name = "-c", aliases = {"--close"}, forbids = {"-s"})
        public void addDocument(String documentName) {
            documents.add(new StringDocument(documentName));
        }

        @Option(name = "-x", aliases = {"--exit"})
        private boolean stopOption = false;

        @Option(name = "-o", aliases = {"--open"},forbids = {"-s"})
        public void open(String documentName) throws IllegalArgumentException {
            Optional<StringDocument> documentOptional = documents.stream()
                    .filter(doc -> doc.getName().equals(documentName))
                    .findFirst();

            if (documentOptional.isPresent()) {
                activeDocument = documentOptional.get();
                beanClass = DocumentWorker.class;
            } else {
                LOGGER.error("No object matched the given name", new IllegalArgumentException());
            }
        }

        @Override
        public void run() {
            if (this.stopOption) {
                documents.forEach(Document::close);
                stop = true;
            }
        }
    }

    public class DocumentWorker implements Runnable {

        public DocumentWorker() {}

        @Option(name = "-a", aliases = {"--add"}, handler = StringArrayOptionHandler.class)
        private String[] addNodes;

        @Option(name = "-l", aliases = {"--link"}, handler = StringArrayOptionHandler.class)
        private String[] link;

        @Option(name = "-u", aliases = {"--unlink"}, handler = StringArrayOptionHandler.class)
        private String[] unlink;

        @Option(name = "-x", aliases = {"--exit"})
        private boolean exit = false;

        @Option(name = "-d", aliases = {"--display"})
        private void display(String predicate) {
            activeDocument.display(predicate);
        }

        @Override
        public void run() {
            if (addNodes != null) {
                List<Pair<String, String>> nodeList = new LinkedList<>();
                for (int i = 0; i + 1 < this.addNodes.length; i += 2) {
                    nodeList.add(new ImmutablePair<>(addNodes[i], addNodes[i + 1]));
                }

                activeDocument.addNodes(nodeList);
            }

            if (this.link != null) {
                if (this.link.length < 3) {
                    throw new IllegalArgumentException("-link param needs 3 parameters");
                }

                activeDocument.addPredicate(this.link[0], this.link[1], this.link[2]);
            }

            if (this.unlink != null) {
                throw new NotImplementedException();
            }

            if (this.exit) {
                beanClass = MainWorker.class;
            }
        }
    }
}
