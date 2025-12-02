import java.util.ArrayList;

public class InternalNode extends Node {
    public ArrayList<String> keys; // separator keys
    public ArrayList<Node> children; // size = keys.size()+1

    public InternalNode() {
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.parent = null;
        this.isLeaf = false;
    }
}
