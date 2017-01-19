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

Commands can  be concatenated in each layer. You can't mix commands of two layers. For example `-c mydoc -o mydoc` will create and open a new document.
`-a A A B B -l A x B -d x` will create two nodes `A` and `B`, create an edge of type `x` between them and display the edge.
The order of commands does not matter. The predicater will automatically execute commands in a predfined order. Therefore `-d x -l A x B -a A A B B` will do exactly the same as the example before.
The order in which commands will be executed matches the order in which commands are listed below. For example `--hide` will be executed before `--show`.

### First-layer commands

In the first layer there are follwoing commands available:
- `--create DOCNAME` alias `-c`: Creates a document named `DOCNAME`.
- `--open DOCNAME` alias `-o`: Opens a document named `DOCNAME`.
- `--read DOCNAME PATH` alias `-r`: Reads a document from file stated in `PATH` and names the document `DOCNAME`.
- `--write DOCNAME PATH` alias `-w`: Write the document named `DOCNAME` to the file stated in `PATH`.

### Second-layer commands

In the second layer ther are following commands available:
- `--remove-node [ID]+` alias `--remove` `-r`: Removes all stated nodes with the id being `ID` from the document.
- `--add-node [ID VALUE]+` alias `--add` `-a`: Adds all stated nodes to the document.
Each node can then be referenced via `ID` whereas `VALUE` will be displayed in the renderer.
- `--unlink-nodes [ID1 EDGE ID2]+` alias `--unlink` `-u`: Removes the edge of type `EDGE` between the nodes with the ids `ID1` and `ID2`.
- `--link-nodes [ID1 EDGE ID2]+` alias `--link` `-l`: Creates an edge of type `EDGE` between the nodes with the ids `ID1` and `ID2`.
If `EDGE` is of format `TYPE:LABEL` `TYPE` will be the edge type and `LABEL` will label the new edge.
If there is more than one `:` everything behind the second colon will be ignored.
- `--hide [EDGE]+` alias `-h`: Hides all edges of type `EDGE`.
- `--display [EDGE]+` alias `-d`: Display all edges of type `EDGE`.
- `--label-node [ID LABEL]+` alias `-ln`: Labels the node with the id being `ID` with `LABEL`. 
- `--label-edge [ID1 EDGE ID2 LABEL]+` alias `-le`: Labels the edge of type `EDGE` between the nodes with the ids being `ID1` and `ID2` with `LABEL`.
- `--edge-types` alias `--types` `-t`: Displays all list of all available edge types.
- `--exit` alias `-x`: Closes the current document and returns to first-layer.
