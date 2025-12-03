// BPlusTree.java
// B+ tree implementation (in-memory) with:
// - insert (add new parts)
// - update (modify description)
// - search by ID
// - displayNextTen / range scan
// - loadFromFile / saveToFile
// This implementation intentionally omits deletion (Member 2) per your request.

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BPlusTree {

    // B+ tree parameters per project spec
    public static final int INTERNAL_MIN = 2;   // minimum keys in internal after merges (not used here)
    public static final int INTERNAL_MAX = 4;   // allow 2..4 keys per index node
    public static final int LEAF_MAX = 16;      // each leaf can store up to 16 records
    public static final int LEAF_MIN = 8;       // minimal leaf size after deletion (not used since no delete)

    private Node root;

    // Stats
    public int totalSplits = 0;     // leaf + internal splits
    public int parentSplits = 0;    // internal splits (parent)
    public int totalFusions = 0;    // merges - will remain 0 since deletion not implemented
    public int parentFusions = 0;   // internal merges - also 0 here

    public BPlusTree() {
        root = new LeafNode();
    }

    // -------------------- SEARCH --------------------
    // find a PartRecord by ID; returns null if not found
    public PartRecord search(String id) {
        if (id == null) return null;
        LeafNode leaf = findLeaf(id);
        if (leaf == null) return null;
        return leaf.getRecord(id);
    }

    // -------------------- INSERT --------------------
    // Insert a new PartRecord; return false on duplicate or error
    public boolean insert(PartRecord rec) {
        if (rec == null || rec.id == null) return false;

        LeafNode leaf = findLeaf(rec.id);
        if (leaf.contains(rec.id)) {
            // duplicate ID
            return false;
        }

        leaf.insertSorted(rec);

        // if overflow, split leaf and propagate
        if (leaf.size() > LEAF_MAX) {
            splitLeaf(leaf);
        }
        return true;
    }

    // -------------------- UPDATE --------------------
    // update the description of an existing record; returns true on success
    public boolean update(String id, String newDesc) {
        if (id == null) return false;
        LeafNode leaf = findLeaf(id);
        if (leaf == null) return false;
        return leaf.UpdateRecord(id, newDesc);
    }

    // -------------------- FIND LEAF --------------------
    // traverse internal nodes until we reach a leaf
    private LeafNode findLeaf(String id) {
        Node node = root;
        if (node == null) {
            root = new LeafNode();
            return (LeafNode) root;
        }

        while (!node.isLeaf) {
            InternalNode in = (InternalNode) node;
            int i = 0;
            // keys are separators equal to first key of right child
            while (i < in.keys.size() && id.compareTo(in.keys.get(i)) >= 0) i++;
            node = in.children.get(i);
        }
        return (LeafNode) node;
    }

    // -------------------- SPLIT LEAF --------------------
    private void splitLeaf(LeafNode leaf) {
        int mid = leaf.size() / 2; // e.g., 17 -> 8 and 9

        LeafNode right = new LeafNode();
        // move right half to new node
        right.records.addAll(leaf.records.subList(mid, leaf.size()));
        // shrink left leaf
        leaf.records = new ArrayList<>(leaf.records.subList(0, mid));

        // link siblings (maintain leaf chain)
        right.next = leaf.next;
        if (leaf.next != null) leaf.next.prev = right;
        right.prev = leaf;
        leaf.next = right;

        // set parent reference
        right.parent = leaf.parent;

        // promoted key is the first key of the right node
        String promoteKey = right.records.get(0).id;

        // insert separator into parent (may create internal splits)
        insertIntoParent(leaf, promoteKey, right);

        // update statistics
        totalSplits++;
    }

    // -------------------- INSERT INTO PARENT --------------------
    private void insertIntoParent(Node left, String key, Node right) {
        // if left has no parent, create new root
        if (left.parent == null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(key);
            newRoot.children.add(left);
            newRoot.children.add(right);

            left.parent = newRoot;
            right.parent = newRoot;
            root = newRoot;

            parentSplits++;
            totalSplits++;
            return;
        }

        InternalNode parent = left.parent;
        // find index of left child inside parent
        int idx = parent.children.indexOf(left);
        // insert key at idx (separator) and right child at idx+1
        parent.keys.add(idx, key);
        parent.children.add(idx + 1, right);
        right.parent = parent;

        // if parent overflows, split internal node
        if (parent.keys.size() > INTERNAL_MAX) {
            splitInternal(parent);
        }
    }

    // -------------------- SPLIT INTERNAL --------------------
    private void splitInternal(InternalNode node) {
        int totalKeys = node.keys.size();
        int mid = totalKeys / 2; // promote node.keys[mid]
        String promote = node.keys.get(mid);

        InternalNode right = new InternalNode();
        // right keys = mid+1 .. end
        right.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        // right children = mid+1 .. end
        right.children.addAll(node.children.subList(mid + 1, node.children.size()));

        // fix parent pointers for right's children
        for (Node c : right.children) c.parent = right;

        // left keeps 0..mid-1 keys and 0..mid children
        node.keys = new ArrayList<>(node.keys.subList(0, mid));
        node.children = new ArrayList<>(node.children.subList(0, mid + 1));

        // insert promoted separator into parent
        insertIntoParent(node, promote, right);

        parentSplits++;
        totalSplits++;
    }

    // -------------------- RANGE SCAN / DISPLAY NEXT 10 --------------------
    // display next 10 records starting from given ID (inclusive)
    public void displayNextTen(String startID) {
        List<PartRecord> found = scanFrom(startID, 10);
        if (found.isEmpty()) {
            System.out.println("No records found starting from " + startID);
            return;
        }
        for (PartRecord r : found) System.out.println(r);
    }

    // collect 'count' records starting from startId (inclusive)
    public List<PartRecord> scanFrom(String startId, int count) {
        List<PartRecord> out = new ArrayList<>();
        LeafNode leaf = findLeaf(startId);
        if (leaf == null) return out;

        int idx = 0;
        while (idx < leaf.records.size() && leaf.records.get(idx).id.compareTo(startId) < 0) idx++;

        LeafNode cur = leaf;
        while (cur != null && out.size() < count) {
            while (idx < cur.records.size() && out.size() < count) {
                out.add(cur.records.get(idx++));
            }
            cur = cur.next;
            idx = 0;
        }
        return out;
    }

    // -------------------- I/O (load/save) --------------------
    // reads fixed-width file (columns 1-7 ID, 16-80 description) and inserts records into tree
    public void loadFromFile(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            // file may have simple "ID description" lines or fixed width.
            // Try to parse first token as ID and rest as description (robust to the sample file you provided).
            String trimmed = line.trim();
            // If there is a space near start (like AAA-077 ...), use first token as id.
            String id = null;
            String desc = "";
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length >= 1) {
                id = parts[0].trim();
                if (parts.length == 2) desc = parts[1].trim();
            }
            // Fallback: if line long and follows fixed-width columns, use substring
            if ((id == null || id.isEmpty()) && line.length() >= 7) {
                id = line.substring(0, Math.min(7, line.length())).trim();
                if (line.length() >= 16) desc = line.substring(15).trim();
            }
            if (id == null || id.isEmpty()) continue;
            // insert record (ignore duplicates during load)
            insert(new PartRecord(id, desc));
        }
    }

    // writes all records out by scanning left-to-right through leaf chain
    // Writes into same fixed-width-ish format: ID in columns 1-7, blanks 8-15, description starting at col 16
    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename))) {
            LeafNode leftmost = getLeftmostLeaf();
            LeafNode cur = leftmost;
            while (cur != null) {
                for (PartRecord r : cur.records) {
                    String idField = String.format("%-7s", r.id);
                    String descField = r.description == null ? "" : r.description;
                    if (descField.length() > 65) descField = descField.substring(0, 65);
                    // preserve spacing between columns 7 and 16
                    String line = idField + String.format("%8s", "") + String.format("%-65s", descField);
                    bw.write(line);
                    bw.newLine();
                }
                cur = cur.next;
            }
        }
    }

    private LeafNode getLeftmostLeaf() {
        Node node = root;
        while (!node.isLeaf) node = ((InternalNode) node).children.get(0);
        return (LeafNode) node;
    }

    // -------------------- UTIL / STATS --------------------
    public int computeDepth() {
        int depth = 0;
        Node node = root;
        while (node != null) {
            depth++;
            if (node.isLeaf) break;
            node = ((InternalNode) node).children.get(0);
        }
        return depth;
    }

    public int countAllRecords() {
        int count = 0;
        LeafNode cur = getLeftmostLeaf();
        while (cur != null) {
            count += cur.records.size();
            cur = cur.next;
        }
        return count;
    }

    // -------------------- DELETE (PUBLIC) --------------------
    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            System.out.println("Invalid ID.");
            return false;
        }

        LeafNode leaf = findLeaf(id);
        if (leaf == null) {
            System.out.println("Record not found.");
            return false;
        }

        PartRecord toDelete = leaf.getRecord(id);
        if (toDelete == null) {
            System.out.println("Record not found.");
            return false;
        }

        // Confirm deletion
        System.out.println("Found: " + toDelete);
        System.out.print("Are you sure you want to delete this record? (Y/N): ");
        Scanner sc = new Scanner(System.in);
        String ans = sc.nextLine().trim().toLowerCase();

        if (!(ans.equals("y") || ans.equals("yes"))) {
            System.out.println("Deletion cancelled.");
            return false;
        }

        // Perform deletion
        PartRecord removed = leaf.deleteRecord(id);
        if (removed == null) {
            System.out.println("Error: deletion failed.");
            return false;
        }

        System.out.println("Record '" + id + "' deleted successfully.");
        return true;
    }


    public void printStats() {
        System.out.println("Statistics:");
        System.out.println(" Total splits: " + totalSplits);
        System.out.println(" Parent splits: " + parentSplits);
        System.out.println(" Total fusions: " + totalFusions);
        System.out.println(" Parent fusions: " + parentFusions);
        System.out.println(" Tree depth: " + computeDepth());
        System.out.println(" Total records: " + countAllRecords());
    }
}
