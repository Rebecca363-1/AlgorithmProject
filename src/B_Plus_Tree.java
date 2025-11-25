import java.util.ArrayList;

public class B_Plus_Tree {

    // The maximum number of records that are allowed in a leaf node
    private static final int LEAF_MAX = 16;

    // Diagnostic counter for overflow test (split)
    public int leafSplits = 0;

    // Reference to root
    private LeafNode root = new LeafNode();

    // Insert a PartRecord into the B+Tree
    public boolean insert(PartRecord record) {
        LeafNode leaf = findLeaf(record.id);

        // Check for any duplicates record
        if (leaf.contains(record.id)) {
            System.out.println("Error: This Part ID already exists.");
            return false;
        }

        // Insert into leaf in a sorted order
        leaf.insertSorted(record);

        // Check overflow and split if needed
        if (leaf.size() > LEAF_MAX) {
            splitLeaf(leaf);
            leafSplits++;
        }
        return true;
    }
// makes update to an exsisting recoeds ID
    public boolean update(String id, String newDescription) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        LeafNode leaf = findLeaf(id);
        boolean success = leaf.UpdateRecord(id, newDescription);

        if (success) {
            System.out.println("Updated Successfully.");

        }
        return success;
    }


    // Finds the leaf node where the record should be inserted

    private LeafNode findLeaf(String id) {
        return root; // For now it will always returns root
    }

    // Splitting logic if a split is needed
    private void splitLeaf(LeafNode leaf) {
        System.out.println("Leaf split occurred.");
    }
}
