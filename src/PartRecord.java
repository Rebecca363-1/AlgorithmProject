public class PartRecord {
        public String id;
        public String description;

        public PartRecord(String id, String description) {
            this.id = id;
            this.description = description;

    }
    // used for updating a exsisting record if needed
    public void UpdateDescription(String newDescription) {
            this.description = newDescription;
    }
}
