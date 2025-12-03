public class PartRecord {
    public String id;
    public String description;

    public PartRecord(String id, String description) {
        this.id = id;
        this.description = description;
    }

    // used for updating an existing record if needed
    public void UpdateDescription(String newDescription) {
        this.description = newDescription;
    }

    @Override
    public String toString() {
        // keep the formatting similar to the project spec: ID (7 chars) + description
        return String.format("%-7s  %s", id, description);
    }
}
