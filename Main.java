import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        BPlusTree tree = new BPlusTree();
        String filename = "partfile.txt"; // default
        if (args.length >= 1) filename = args[0];

        System.out.println("Loading " + filename + " ...");
        try {
            tree.loadFromFile(filename);
        } catch (IOException e) {
            System.out.println("Warning: could not load file: " + e.getMessage());
        }
        System.out.println("Loaded " + tree.countAllRecords() + " records.");
        System.out.println();

        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.println("\nMenu:");
            System.out.println("1) Search part by ID");
            System.out.println("2) Add (Insert) new part");
            System.out.println("3) Update part description");
            System.out.println("4) Delete part");
            System.out.println("5) Display next 10 parts (from ID)");
            System.out.println("6) Save to file");
            System.out.println("7) Print stats");
            System.out.println("8) Exit");
            System.out.print("Choose option: ");
            String opt = sc.nextLine().trim();

            switch (opt) {
                case "1":
                    System.out.print("Enter Part ID: ");
                    String sId = sc.nextLine().trim();
                    PartRecord found = tree.search(sId);
                    if (found == null) System.out.println("Not found.");
                    else System.out.println(found);
                    break;

                case "2":
                    System.out.print("New Part ID: ");
                    String newId = sc.nextLine().trim();
                    if (newId.length() == 0) { System.out.println("Invalid ID."); break; }
                    System.out.print("Description: ");
                    String desc = sc.nextLine().trim();
                    boolean ok = tree.insert(new PartRecord(newId, desc));
                    if (!ok) System.out.println("Insert failed: duplicate or error.");
                    else System.out.println("Inserted.");
                    break;

                case "3":
                    System.out.print("Part ID to update: ");
                    String uId = sc.nextLine().trim();

                    PartRecord recToUpdate = tree.search(uId);
                    if (recToUpdate == null) {
                        System.out.println("Not found.");
                        break;
                    }

                    System.out.println("Old: " + recToUpdate);
                    System.out.print("New description: ");
                    String newDescVal = sc.nextLine().trim();

                    recToUpdate.UpdateDescription(newDescVal);
                    System.out.println("Updated.");
                    break;


                case "4":
                    System.out.print("Part ID to delete: ");
                    String dId = sc.nextLine().trim();
                    PartRecord toDelete = tree.search(dId);
                    if (toDelete == null) { System.out.println("Not found."); break; }
                    System.out.print("Confirm delete " + dId + " (y/N): ");
                    String confirm = sc.nextLine().trim().toLowerCase();
                    if (confirm.equals("y") || confirm.equals("yes")) {
                        boolean del = tree.delete(dId);
                        if (del) System.out.println("Deleted.");
                        else System.out.println("Delete failed.");
                    } else {
                        System.out.println("Cancelled.");
                    }
                    break;

                case "5":
                    System.out.print("Start Part ID: ");
                    String start = sc.nextLine().trim();
                    tree.displayNextTen(start);
                    break;

                case "6":
                    System.out.print("Save filename (enter for default '" + filename + "'): ");
                    String out = sc.nextLine().trim();
                    if (out.isEmpty()) out = filename;
                    try {
                        tree.saveToFile(out);
                        System.out.println("Saved to " + out);
                    } catch (IOException e) {
                        System.out.println("Save failed: " + e.getMessage());
                    }
                    break;

                case "7":
                    tree.printStats();
                    break;

                case "8":
                    System.out.print("Save changes before exit? (y/N): ");
                    String s = sc.nextLine().trim().toLowerCase();
                    if (s.equals("y") || s.equals("yes")) {
                        try {
                            tree.saveToFile(filename);
                            System.out.println("Saved.");
                        } catch (IOException e) {
                            System.out.println("Save failed: " + e.getMessage());
                        }
                    }
                    System.out.println("Exiting.");
                    running = false;
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }

        sc.close();
    }
}
