package eu.amadeos.games.artillery.domain;

/** Who drives a tank: a person at the keys, or the computer at one of three levels. */
public enum Control {
    PERSON, EASY, NORMAL, HARD;

    public boolean computer() {
        return this != PERSON;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
