public class B_Plus_Tree {
  // The maximun # of records that are allowed in a leaf node
    private static final int LEAF_MAX = 16;

    // the diagnostic counter for overflow test (split)
    public int leafSplits = 0;


    // inserting records for part catalog
    public boolean insert(PartRecord Record) {
        LeafNode leaf = findLeaf(Record.id);

       // Checks for any duplicate parts
        if (leaf.contains(Record.id)) {
            System.out.println("Error: This Part ID already exists.");
            return false;
        }

        // Goes by sorted order to insert the node
        leaf.insertSorted(Record);

        // If there is a leaf overflow in B+- tree it will split
        if (leaf.size()> LEAF_MAX) {
            splitLeaf(leaf);
            leafSplits++;
        }
        return true;

    }

}
