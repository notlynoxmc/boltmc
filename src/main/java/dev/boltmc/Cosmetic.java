package dev.boltmc;

/** The cosmetic slots Bolt Mc offers. Every one of them has its own on/off switch and color. */
public enum Cosmetic {
    AURA("Aura", "Colored particles drifting around you.", "#55C8FF", Slot.NONE),
    WINGS("Angel Wings", "Small 3D feathered wings on your back.", "#FFFFFF", Slot.NONE),
    HALO("Halo", "Glowing ring floating above your head.", "#FFD23D", Slot.NONE),
    CROWN("Crown", "A golden crown with a red gem.", "#FFC933", Slot.HAT),
    CAP("Cap", "A baseball cap with a brim.", "#2F6BFF", Slot.HAT),
    PROPELLER("Propeller Hat", "A cap with a spinning propeller.", "#FF5A5A", Slot.HAT),
    TOP_HAT("Top Hat", "A fancy top hat with a red band.", "#262626", Slot.HAT),
    CAT_EARS("Cat Ears", "Cute pointy cat ears on your head.", "#FF9A3D", Slot.HAT);

    /** Cosmetics in the same non-NONE slot are exclusive: turning one on turns the others off. */
    public enum Slot { NONE, HAT }

    public final String label;
    public final String desc;
    public final String defaultColor;
    public final Slot slot;

    Cosmetic(String label, String desc, String defaultColor, Slot slot) {
        this.label = label;
        this.desc = desc;
        this.defaultColor = defaultColor;
        this.slot = slot;
    }
}
