package fr.lsmbo.rawfinder;

public enum Status {
    FULLY_ARCHIVED ("Fully archived"),
    PARTIALLY_ARCHIVED ("Partially archived"),
    NOT_ARCHIVED ("Not archived");
    private final String name;
    Status(String status) { name = status; }
    public String toString() { return this.name; }
}
