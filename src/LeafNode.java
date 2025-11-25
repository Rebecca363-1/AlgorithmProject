import java.util.ArrayList;

public class LeafNode {

    // list the sorted records
    private ArrayList<PartRecord> records;

    // points to next node
    public LeafNode next;

    // constructor initializes empty record list and next pointer
    public LeafNode() {
        this.records = new ArrayList<>();
        this.next = null;
    }

    // checks if a record already exists with this Id on the leaf
    public boolean contains(String id) {
        return records.stream().anyMatch(r -> r.id.equals(id));
    }

    // inserts a Part Record in sorted order based on ID + finds the correct sorting position
    public void insertSorted(PartRecord record) {
        int i = 0;

        // finds the correct index based on lexicographic order of IDs
        while (i < records.size() &&
                records.get(i).id.compareTo(record.id) < 0) {
            i++;
        }

        // inserts the record to the correct index
        records.add(i, record);
    }

    public int size() {
        return records.size();
    }

    public PartRecord getRecord(String id) {
        for (PartRecord r : records) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }
    // will update the description of a record in current leaf
    public boolean UpdateRecord(String id, String newDescription) {
        PartRecord record = getRecord(id);

        if (record == null) {
            System.out.println("Error: Can't upadte. This Record has not been Found");
                    return false;
        }

        record.UpdateDescription(newDescription);
        return true;
    }
}
