import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BPlusTree {
    public static final int INTERNAL_MIN = 2;
    public static final int INTERNAL_MAX = 4;
    public static final int LEAF_MAX = 16;
    public static final int LEAF_MIN = 8;

    private Node root;

    // Stats
    public int totalSplits = 0;
    public int parentSplits = 0;
    public int totalFusions = 0;
    public int parentFusions = 0;

    public BPlusTree() {
        root = new LeafNode();
    }

    // -------------------- SEARCH --------------------
    public PartRecord search(String id) {
        LeafNode leaf = findLeaf(id);
        if (leaf == null) return null;
        return leaf.getRecord(id);
    }

    // -------------------- INSERT --------------------
    public boolean insert(PartRecord rec) {
        if (rec == null || rec.id == null) return false;
        LeafNode leaf = findLeaf(rec.id);
        if (leaf.contains(rec.id)) {
            return false; // duplicate
        }
        leaf.insertSorted(rec);
        if (leaf.size() > LEAF_MAX) {
            splitLeaf(leaf);
        }
        return true;
    }

    private LeafNode findLeaf(String id) {
        Node node = root;
        while (!node.isLeaf) {
            InternalNode in = (InternalNode) node;
            int i = 0;
            // keys are separators equal to first key of right child
            while (i < in.keys.size() && id.compareTo(in.keys.get(i)) >= 0) i++;
            node = in.children.get(i);
        }
        return (LeafNode) node;
    }

    private void splitLeaf(LeafNode leaf) {
        int total = leaf.records.size();
        int mid = total / 2; // e.g., 17 -> 8 and 9
        LeafNode right = new LeafNode();

        // move right half to new node
        for (int i = mid; i < leaf.records.size(); i++) {
            right.records.add(leaf.records.get(i));
        }
        // remove moved records from left
        for (int i = leaf.records.size() - 1; i >= mid; i--) {
            leaf.records.remove(i);
        }

        // link siblings
        right.next = leaf.next;
        if (leaf.next != null) leaf.next.prev = right;
        leaf.next = right;
        right.prev = leaf;

        // set parents
        right.parent = leaf.parent;

        String promoteKey = right.records.get(0).id;
        insertIntoParent(leaf, promoteKey, right);

        totalSplits++;
    }

    private void insertIntoParent(Node left, String key, Node right) {
        if (left.parent == null) {
            // create new root
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
        // find index of left in parent's children
        int idx = parent.children.indexOf(left);
        parent.children.add(idx + 1, right);
        parent.keys.add(idx, key);
        right.parent = parent;

        if (parent.keys.size() > INTERNAL_MAX) splitInternal(parent);
    }

    private void splitInternal(InternalNode node) {
        int totalKeys = node.keys.size();
        int mid = totalKeys / 2; // promote node.keys[mid]
        String promote = node.keys.get(mid);

        InternalNode right = new InternalNode();
        // right keys = mid+1 .. end
        for (int i = mid + 1; i < node.keys.size(); i++) right.keys.add(node.keys.get(i));
        // right children = mid+1 .. end
        for (int i = mid + 1; i < node.children.size(); i++) {
            Node c = node.children.get(i);
            right.children.add(c);
            c.parent = right;
        }

        // left keeps 0..mid-1 keys and 0..mid children
        ArrayList<String> leftKeys = new ArrayList<>();
        ArrayList<Node> leftChildren = new ArrayList<>();
        for (int i = 0; i < mid; i++) leftKeys.add(node.keys.get(i));
        for (int i = 0; i <= mid; i++) leftChildren.add(node.children.get(i));

        node.keys = leftKeys;
        node.children = leftChildren;

        // insert promote into parent
        insertIntoParent(node, promote, right);
        totalSplits++;
        parentSplits++;
    }

    // -------------------- DELETE --------------------
    public boolean delete(String id) {
        LeafNode leaf = findLeaf(id);
        if (leaf == null || !leaf.contains(id)) return false;

        // remove record
        for (int i = 0; i < leaf.records.size(); i++) {
            if (leaf.records.get(i).id.equals(id)) {
                leaf.records.remove(i);
                break;
            }
        }

        // if root and empty now -> handle
        if (leaf == root && leaf.records.size() == 0) {
            root = new LeafNode();
            return true;
        }

        if (leaf != root && leaf.records.size() < LEAF_MIN) {
            handleLeafUnderflow(leaf);
        }
        return true;
    }

    private void handleLeafUnderflow(LeafNode leaf) {
        InternalNode parent = leaf.parent;
        if (parent == null) return; // root leaf

        // find siblings
        int idx = parent.children.indexOf(leaf);
        LeafNode leftSibling = null, rightSibling = null;
        if (idx - 1 >= 0) leftSibling = (LeafNode) parent.children.get(idx - 1);
        if (idx + 1 < parent.children.size()) rightSibling = (LeafNode) parent.children.get(idx + 1);

        // Try borrow from left
        if (leftSibling != null && leftSibling.records.size() > LEAF_MIN) {
            // move last of left to front of leaf
            PartRecord borrowed = leftSibling.records.remove(leftSibling.records.size() - 1);
            leaf.records.add(0, borrowed);
            // Update parent key: separator at idx-1 becomes leaf.records[0].id
            parent.keys.set(idx - 1, leaf.records.get(0).id);
            return;
        }

        // Try borrow from right
        if (rightSibling != null && rightSibling.records.size() > LEAF_MIN) {
            PartRecord borrowed = rightSibling.records.remove(0);
            leaf.records.add(borrowed);
            // Update parent key at idx to rightSibling.records[0].id (if exists)
            if (rightSibling.records.size() > 0)
                parent.keys.set(idx, rightSibling.records.get(0).id);
            return;
        }

        // Otherwise fuse (merge)
        if (leftSibling != null) {
            // merge left + leaf
            for (PartRecord r : leaf.records) leftSibling.records.add(r);
            leftSibling.next = leaf.next;
            if (leaf.next != null) leaf.next.prev = leftSibling;
            // remove child and separator key at idx-1
            parent.children.remove(idx);
            parent.keys.remove(idx - 1);
            totalFusions++;
        } else if (rightSibling != null) {
            // merge leaf + right
            for (PartRecord r : rightSibling.records) leaf.records.add(r);
            leaf.next = rightSibling.next;
            if (rightSibling.next != null) rightSibling.next.prev = leaf;
            // remove right sibling from parent and key at idx
            parent.children.remove(idx + 1);
            parent.keys.remove(idx);
            totalFusions++;
        }

        // parent may underflow
        if (parent == root && parent.keys.size() == 0) {
            // shrink root
            root = parent.children.get(0);
            root.parent = null;
            parentFusions++;
            return;
        }
        if (parent != root && parent.keys.size() < INTERNAL_MIN) {
            handleInternalUnderflow(parent);
        }
    }

    private void handleInternalUnderflow(InternalNode node) {
        InternalNode parent = node.parent;
        if (parent == null) return;

        int idx = parent.children.indexOf(node);
        InternalNode leftSibling = null, rightSibling = null;
        if (idx - 1 >= 0) leftSibling = (InternalNode) parent.children.get(idx - 1);
        if (idx + 1 < parent.children.size()) rightSibling = (InternalNode) parent.children.get(idx + 1);

        // Borrow from left
        if (leftSibling != null && leftSibling.keys.size() > INTERNAL_MIN) {
            // move last key of left up to parent, parent separator down to node
            String borrowedKey = leftSibling.keys.remove(leftSibling.keys.size() - 1);
            Node borrowedChild = leftSibling.children.remove(leftSibling.children.size() - 1);

            String parentSep = parent.keys.get(idx - 1);
            parent.keys.set(idx - 1, borrowedKey);

            node.keys.add(0, parentSep);
            node.children.add(0, borrowedChild);
            borrowedChild.parent = node;
            return;
        }

        // Borrow from right
        if (rightSibling != null && rightSibling.keys.size() > INTERNAL_MIN) {
            String borrowedKey = rightSibling.keys.remove(0);
            Node borrowedChild = rightSibling.children.remove(0);

            String parentSep = parent.keys.get(idx);
            parent.keys.set(idx, borrowedKey);

            node.keys.add(parentSep);
            node.children.add(borrowedChild);
            borrowedChild.parent = node;
            return;
        }

        // Merge with left or right
        if (leftSibling != null) {
            // merge left + node
            String sep = parent.keys.remove(idx - 1);
            // left.keys + sep + node.keys
            leftSibling.keys.add(sep);
            leftSibling.keys.addAll(node.keys);
            leftSibling.children.addAll(node.children);
            for (Node child : node.children) child.parent = leftSibling;
            parent.children.remove(idx);
            totalFusions++;
        } else if (rightSibling != null) {
            String sep = parent.keys.remove(idx);
            node.keys.add(sep);
            node.keys.addAll(rightSibling.keys);
            node.children.addAll(rightSibling.children);
            for (Node child : rightSibling.children) child.parent = node;
            parent.children.remove(idx + 1);
            totalFusions++;
        }

        if (parent == root && parent.keys.size() == 0) {
            root = parent.children.get(0);
            root.parent = null;
            parentFusions++;
            return;
        }
        if (parent != root && parent.keys.size() < INTERNAL_MIN) {
            handleInternalUnderflow(parent);
        }
    }

    // -------------------- RANGE SCAN --------------------
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

    // -------------------- I/O --------------------
    public void loadFromFile(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String id = line.length() >= 7 ? line.substring(0, 7).trim() : line.trim();
            String desc = line.length() >= 16 ? line.substring(15).trim() : "";
            if (id.isEmpty()) continue;
            insert(new PartRecord(id, desc));
        }
    }

    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename))) {
            LeafNode leftmost = getLeftmostLeaf();
            LeafNode cur = leftmost;
            while (cur != null) {
                for (PartRecord r : cur.records) {
                    String idField = String.format("%-7s", r.id);
                    String descField = r.description == null ? "" : r.description;
                    if (descField.length() > 65) descField = descField.substring(0, 65);
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

    // -------------------- UTIL --------------------
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

    // CLI helpers
    public void displayNextTen(String startId) {
        List<PartRecord> list = scanFrom(startId, 10);
        if (list.isEmpty()) {
            System.out.println("No records found starting from " + startId);
            return;
        }
        for (PartRecord r : list) System.out.println(r);
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
