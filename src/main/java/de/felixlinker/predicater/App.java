package de.felixlinker.predicater;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.graphstream.stream.GraphParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

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

//        this.documents.forEach(Document::close);
    }

    public static void main(String[] args) {
        new App().run();
    }

    public class MainWorker implements Runnable {

        @Option(name = "-c", aliases = {"--close"})
        private void addDocument(String documentName) {
            StringDocument newDocument = new StringDocument(documentName);
            documents.add(newDocument);
            newDocument.display();
        }

        @Option(name = "-x", aliases = {"--exit"})
        private boolean stopOption = false;

        @Option(name = "-o", aliases = {"--open"})
        private void open(String documentName) throws IllegalArgumentException {
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

        @Option(name = "-r", aliases = {"--read"}, handler = StringArrayOptionHandler.class)
        private String[] read;

        @Option(name = "-w", aliases = {"--write"}, handler = StringArrayOptionHandler.class)
        private String[] write;

        @Override
        public void run() {
            if (this.stopOption) {
                stop = true;
            }

            if (read != null) {
                for (int i = 0; i + 1 < this.read.length; i += 2) {
                    try {
                        StringDocument newDocument = new StringDocument(this.read[i]);
                        newDocument.read(this.read[i + 1]);
                        documents.add(newDocument);
                        newDocument.display();
                    } catch (IOException | GraphParseException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }

            if (write != null) {
                for (int i = 0; i + 1 < this.write.length; i += 2) {
                    final int index = i;
                    StringDocument writeDocument = documents.stream()
                            .filter(document -> document.getName().equals(this.write[index]))
                            .findFirst()
                            .orElse(null);

                    if (writeDocument == null) {
                        throw new IllegalArgumentException("no document of given name exists");
                    }

                    try {
                        writeDocument.write(this.write[index + 1]);
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    public class DocumentWorker implements Runnable {

        @Option(name = "-a", aliases = {"--add"}, handler = StringArrayOptionHandler.class)
        private String[] addNodes;

        @Option(name = "-l", aliases = {"--link"}, handler = StringArrayOptionHandler.class)
        private String[] link;

        @Option(name = "-u", aliases = {"--unlink"}, handler = StringArrayOptionHandler.class)
        private String[] unlink;

        @Option(name = "-x", aliases = {"--exit"})
        private boolean exit = false;

        @Option(name = "-d", aliases = {"--showPredicate"})
        private void display(String predicate) {
            activeDocument.showPredicate(predicate);
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
                for (int i = 0; i + 2 < this.link.length; i += 3) {
                    activeDocument.addPredicate(this.link[i], this.link[i + 1], this.link[i + 2]);
                }
            }

            if (this.unlink != null) {
                for (int i = 0; i + 2 < this.link.length; i += 3) {
                    activeDocument.removePredicate(this.link[i], this.link[i + 1], this.link[i + 2]);
                }
            }

            if (this.exit) {
                beanClass = MainWorker.class;
            }
        }
    }
}
