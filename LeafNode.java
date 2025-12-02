import java.util.ArrayList;

public class LeafNode extends Node {
    public ArrayList<PartRecord> records;
    public LeafNode next;
    public LeafNode prev;

    public LeafNode() {
        this.records = new ArrayList<>();
        this.next = null;
        this.prev = null;
        this.parent = null;
        this.isLeaf = true;
    }

    public boolean contains(String id) {
        return records.stream().anyMatch(r -> r.id.equals(id));
    }

    public void insertSorted(PartRecord record) {
        int i = 0;
        while (i < records.size() && records.get(i).id.compareTo(record.id) < 0) i++;
        records.add(i, record);
    }

    public PartRecord getRecord(String id) {
        for (PartRecord r : records) if (r.id.equals(id)) return r;
        return null;
    }

    public boolean UpdateRecord(String id, String newDesc) {
        PartRecord r = getRecord(id);
        if (r == null) return false;
        r.UpdateDescription(newDesc);
        return true;
    }

    public int size() {
        return records.size();
    }
}
