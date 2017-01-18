package de.felixlinker.predicater;

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
import java.util.Set;

/**
 * CLI App to support {@link StringDocument} usage.
 */
public class App {

    private static final Logger LOGGER = LogManager.getLogger(App.class);
    private static final String EDGE_LABEL_REGEX = ":";

    private final List<StringDocument> documents = new LinkedList<>();

    private StringDocument activeDocument;

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * The class for the {@link CmdLineParser} to work on.
     * After the parser has read all arguments, {@link Runnable#run()} will be invoked on the instantiated bean.
     */
    private Class<? extends Runnable> beanClass = MainWorker.class;

    private boolean stop = false;

    /**
     * This method functions as the main loop.
     */
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

    /**
     * Worker class to be filled by {@link CmdLineParser}. Functions as first stage.
     */
    public class MainWorker implements Runnable {

        /**
         * Adds a document to the pool.
         * @param documentName Name of the document to add.
         */
        @Option(name = "-c", aliases = {"--create"})
        private void addDocument(String documentName) {
            StringDocument newDocument = new StringDocument(documentName);
            documents.add(newDocument);
            newDocument.display();
        }

        /**
         * Stores the option to halt the whole App.
         */
        @Option(name = "-x", aliases = {"--exit"})
        private boolean stopOption = false;

        /**
         * Opens the given document and switches to {@link DocumentWorker}.
         * @param documentName Document to open.
         * @throws IllegalArgumentException
         */
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

        /**
         * Arguments to read documents from files.
         */
        @Option(name = "-r", aliases = {"--read"}, handler = StringArrayOptionHandler.class)
        private String[] read;

        /**
         * Arguments to write documents to files.
         */
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

    /**
     * Worker class to be filled by {@link CmdLineParser}. Works on a document that has been opened by the {@link MainWorker}.
     */
    public class DocumentWorker implements Runnable {

        /**
         * Arguments to add nodes to the document.
         */
        @Option(name = "-a", aliases = {"--add", "--add-node"}, handler = StringArrayOptionHandler.class)
        private String[] addNodes;

        /**
         * Arguments to remove nodes from the document.
         */
        @Option(name = "-r", aliases = {"--remove", "--remove-node"}, handler = StringArrayOptionHandler.class)
        private String[] removeNodes;

        /**
         * Arguments to add edges to the document.
         */
        @Option(name = "-l", aliases = {"--link", "--link-nodes"}, handler = StringArrayOptionHandler.class)
        private String[] link;

        /**
         * Arguments to removes edges from the document.
         */
        @Option(name = "-u", aliases = {"--unlink, --unlink-nodes"}, handler = StringArrayOptionHandler.class)
        private String[] unlink;

        @Option(name = "-ln", aliases = {"--label-node"}, handler = StringArrayOptionHandler.class)
        private String[] nodeLabels;

        @Option(name = "-le", aliases = {"--label-edge"}, handler = StringArrayOptionHandler.class)
        private String[] edgeLabels;

        /**
         * Arguments to exit this worker.
         */
        @Option(name = "-x", aliases = {"--exit"})
        private boolean exit = false;

        /**
         * Argument to display edges of a specific type.
         * @param predicate Edge type to display.
         */
        @Option(name = "-d", aliases = {"--display"})
        private void display(String predicate) {
            activeDocument.showPredicate(predicate);
        }

        @Option(name = "-h", aliases = {"--hide"})
        private void hide(String predicate) {
            activeDocument.hidePredicate(predicate);
        }

        @Option(name = "-t", aliases = {"--edge-types", "--types"})
        private boolean doListEdgeTypes = false;

        @Override
        public void run() {
            if (this.addNodes != null) {
                for (int i = 0; i + 1 < this.addNodes.length; i += 2) {
                    activeDocument.addNode(this.addNodes[i], this.addNodes[i + 1]);
                }
            }

            if (this.removeNodes != null) {
                for (String nodeId: this.removeNodes) {
                    activeDocument.removeNode(nodeId);
                }
            }

            if (this.link != null) {
                for (int i = 0; i + 2 < this.link.length; i += 3) {
                    String[] split = this.link[i + 1].split(EDGE_LABEL_REGEX);
                    String metadata = "";
                    if (split.length > 1) {
                        metadata = split[1];
                    }

                    activeDocument.predicate(this.link[i], split[0], this.link[i + 2], metadata);
                }
            }

            if (this.unlink != null) {
                for (int i = 0; i + 2 < this.unlink.length; i += 3) {
                    activeDocument.unpredicate(this.unlink[i], this.unlink[i + 1], this.unlink[i + 2]);
                }
            }

            if (this.nodeLabels != null) {
                for (int i = 0; i + 1 < this.nodeLabels.length; i += 2) {
                    activeDocument.setNodeLabel(this.nodeLabels[i], this.nodeLabels[i + 1]);
                }
            }

            if (this.edgeLabels != null) {
                for (int i = 0; i + 3 < this.edgeLabels.length; i += 4) {
                    activeDocument.setEdgeLabel(this.edgeLabels[i], this.edgeLabels[i + 1], this.edgeLabels[i + 2], this.edgeLabels[i + 3]);
                }
            }

            if (this.doListEdgeTypes) {
                this.printEdgeTypes();
            }

            if (this.exit) {
                beanClass = MainWorker.class;
            }
        }

        private void printEdgeTypes() {
            Set<String> predicates = activeDocument.getPredicates();
            StringBuilder builder = new StringBuilder()
                    .append("Following predicates are available:");
            predicates
                    .forEach(predicate -> {
                        builder
                                .append('\n')
                                .append(predicate);
                    });

            LOGGER.info(builder);
        }
    }
}
