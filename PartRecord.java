public class PartRecord {
    public String id;
    public String description;

    public PartRecord(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public void UpdateDescription(String newDescription) {
        this.description = newDescription;
    }

    @Override
    public String toString() {
        return String.format("%-7s  %s", id, description);
    }
}
