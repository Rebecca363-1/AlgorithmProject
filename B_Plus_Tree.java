import java.util.ArrayList;

public class B_Plus_Tree {

    private static final int LEAF_MAX = 16;
    private static final int INTERNAL_MAX = 4;

    public int leafSplits = 0;
    public int internalSplits = 0;

    private Node root = new LeafNode();

    // ============================================================
    // INSERT
    // ============================================================
    public boolean insert(PartRecord record) {
        LeafNode leaf = findLeaf(record.id);

        if (leaf.contains(record.id)) {
            System.out.println("Error: ID already exists.");
            return false;
        }

        leaf.insertSorted(record);

        if (leaf.size() > LEAF_MAX) {
            splitLeaf(leaf);
            leafSplits++;
        }

        return true;
    }

    // ============================================================
    // SEARCH
    // ============================================================
    public PartRecord search(String id) {
        LeafNode leaf = findLeaf(id);
        for (PartRecord r : leaf.records) {
            if (r.id.equals(id)) return r;
        }
        return null;
    }

    // ============================================================
    // UPDATE
    // ============================================================
    public boolean update(String id, String newDesc) {
        LeafNode leaf = findLeaf(id);
        PartRecord rec = leaf.getRecord(id);

        if (rec == null) {
            System.out.println("Record not found.");
            return false;
        }

        rec.UpdateDescription(newDesc);
        return true;
    }

    // ============================================================
    // FIND LEAF
    // ============================================================
    private LeafNode findLeaf(String id) {
        Node node = root;

        while (node instanceof InternalNode) {

            InternalNode in = (InternalNode) node;
            int i = 0;

            while (i < in.keys.size() && id.compareTo(in.keys.get(i)) >= 0) {
                i++;
            }

            node = in.children.get(i);
        }

        return (LeafNode) node;
    }

    // ============================================================
    // SPLIT LEAF
    // ============================================================
    private void splitLeaf(LeafNode leaf) {

        int mid = leaf.size() / 2;

        LeafNode right = new LeafNode();
        right.records.addAll(leaf.records.subList(mid, leaf.size()));
        leaf.records = new ArrayList<>(leaf.records.subList(0, mid));

        // link leaves
        right.next = leaf.next;
        if (leaf.next != null) leaf.next.prev = right;
        right.prev = leaf;
        leaf.next = right;

        String promoteKey = right.records.get(0).id;

        insertIntoParent(leaf, promoteKey, right);
    }

    // ============================================================
    // INSERT INTO PARENT
    // ============================================================
    private void insertIntoParent(Node left, String key, Node right) {

        if (left == root) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(key);
            newRoot.children.add(left);
            newRoot.children.add(right);

            root = newRoot;

            left.parent = newRoot;
            right.parent = newRoot;

            internalSplits++;
            return;
        }

        InternalNode parent = left.parent;

        int pos = parent.children.indexOf(left);

        parent.keys.add(pos, key);
        parent.children.add(pos + 1, right);
        right.parent = parent;

        if (parent.keys.size() > INTERNAL_MAX) {
            splitInternal(parent);
        }
    }

    // ============================================================
    // SPLIT INTERNAL
    // ============================================================
    private void splitInternal(InternalNode node) {

        int mid = node.keys.size() / 2;
        String promote = node.keys.get(mid);

        InternalNode right = new InternalNode();
        right.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        right.children.addAll(node.children.subList(mid + 1, node.children.size()));

        // fix parent pointers
        for (Node child : right.children) {
            child.parent = right;
        }

        node.keys = new ArrayList<>(node.keys.subList(0, mid));
        node.children = new ArrayList<>(node.children.subList(0, mid + 1));

        insertIntoParent(node, promote, right);

        internalSplits++;
    }

    // ============================================================
    // DISPLAY NEXT 10 RECORDS
    // ============================================================
    public void displayNextTen(String startID) {

        LeafNode leaf = findLeaf(startID);

        int index = 0;
        for (int i = 0; i < leaf.records.size(); i++) {
            if (leaf.records.get(i).id.compareTo(startID) >= 0) {
                index = i;
                break;
            }
        }

        int count = 0;

        for (int i = index; i < leaf.records.size() && count < 10; i++) {
            System.out.println(leaf.records.get(i));
            count++;
        }

        LeafNode next = leaf.next;

        while (count < 10 && next != null) {
            for (PartRecord r : next.records) {
                if (count >= 10) break;
                System.out.println(r);
                count++;
            }
            next = next.next;
        }

        if (count == 0) {
            System.out.println("No records found.");
        }
    }
}
