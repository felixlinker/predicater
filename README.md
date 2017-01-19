# predicater

This project provides a CLI along with a renderer to allow you to easily take notes graph-style.
You can create documents which hold nodes and edges of different type.
Each node has a value which will be displayed an id by which you can reference it.
You can connect nodes with edges. Each edge type holds edges completely independent of each other.
Only one edge type can be displayed at a time.

## Usage

When you have the predicater up and running there are two layers of commands.
The first layer serves as the menu. There you can create and open documents.

The second layers works with the document you have oppened. It will be accessed automatically once you've opened a document.

### First-layer commands

In the first layer there are follwoing commands available:
- `--create DOCNAME` alias `-c`: Creates a document named `DOCNAME`.
- `--open DOCNAME` alias `-o`: Opens a document named `DOCNAME`.
- `--read DOCNAME PATH` alias `-r`: Reads a document from file stated in `PATH` and names the document `DOCNAME`.
- `--write DOCNAME PATH` alias `-w`: Write the document named `DOCNAME` to the file stated in `PATH`.

### Second-layer commands

In the second layer ther are following commands available:
- `--add-node [ID VALUE]+` alias `--add` `-a`: Adds all stated nodes to the document.
Each node can then be referenced via `ID` whereas `VALUE` will be displayed in the renderer.
- `--remove-node [ID]+` alias `--remove` `-r`: Removes all stated nodes with the id being `ID` from the document.
- `--link-nodes [ID1 EDGE ID2]+` alias `--link` `-l`: Creates an edge of type `EDGE` between the nodes with the ids `ID1` and `ID2`.
- `--unlink-nodes [ID1 EDGE ID2]+` alias `--unlink` `-u`: Removes the edge of type `EDGE` between the nodes with the ids `ID1` and `ID2`.
- `--display EDGE` alias `-d`: Display all edges of type `EDGE`.
- `--edge-types` alias `--types` `-t`: Displays all list of all available edge types. **NOTE: not implemented yet!**