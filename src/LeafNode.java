

import java.util.ArrayList;

public class LeafNode extends Node {

    // list the sorted records stored inside this leaf
    public ArrayList<PartRecord> records;

    // points to next leaf node in the linked leaf chain
    public LeafNode next;

    // points to previous leaf node
    public LeafNode prev;

    // constructor initializes empty record list and pointers
    public LeafNode() {
        this.records = new ArrayList<>();
        this.next = null;
        this.prev = null;
        this.parent = null;
        this.isLeaf = true;
    }

    // checks if a record already exists with this ID in this leaf
    public boolean contains(String id) {
        return records.stream().anyMatch(r -> r.id.equals(id));
    }

    // inserts a PartRecord into this leaf in sorted order based on the Part ID
    public void insertSorted(PartRecord record) {
        int i = 0;

        // finds correct index based on lexicographic order of the record's ID
        while (i < records.size() && records.get(i).id.compareTo(record.id) < 0) {
            i++;
        }

        // inserts at correct sorted position
        records.add(i, record);
    }

    // returns the number of records currently stored in this leaf
    public int size() {
        return records.size();
    }

    // retrieves an existing record from this leaf by ID (returns null if not found)
    public PartRecord getRecord(String id) {
        for (PartRecord r : records) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    // updates the description of a record stored inside this leaf
    public boolean UpdateRecord(String id, String newDescription) {
        PartRecord r = getRecord(id);

        if (r == null) {
            System.out.println("Error: Can't update. This Record has not been Found");
            return false;
        }

        r.UpdateDescription(newDescription);
        return true;
    }
    // ---------------- DELETE record from this leaf ----------------
    public PartRecord deleteRecord(String id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id.equals(id)) {
                return records.remove(i);   // remove and return deleted record
            }
        }
        return null; // ID not found
    }

}
