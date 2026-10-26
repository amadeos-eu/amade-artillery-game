package eu.amadeos.games.artillery.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A tank on the battlefield: where it stands, how its barrel points, how much power the next shot has,
 * which weapon is loaded and how many of each are left, its health, and what it has done in this round.
 * {@code x} is the middle of its tracks, {@code y} the bottom of them, standing on the ground.
 */
public final class Tank {
    private final Seat seat;
    private double x;
    private double y;
    private double previousY;
    private double angle;
    private double power = 60;
    private Weapon weapon = Weapon.SHELL;
    private final Map<Weapon, Integer> stock = new EnumMap<>(Weapon.class);
    private int health = Field.HEALTH;
    private boolean alive = true;
    private boolean falling;
    private double fallFrom;
    private double vy;
    private int damageDealt;
    private int kills;
    private final List<double[]> lastShot = new ArrayList<>();

    Tank(Seat seat, double x, double y) {
        this.seat = seat;
        this.x = x;
        this.y = y;
        this.previousY = y;
        this.angle = x < Field.WIDTH / 2 ? 50 : 130;
        for (Weapon w : Weapon.values()) stock.put(w, w.stock());
    }

    Tank copy() {
        Tank t = new Tank(seat, x, y);
        t.previousY = previousY;
        t.angle = angle;
        t.power = power;
        t.weapon = weapon;
        t.stock.putAll(stock);
        t.health = health;
        t.alive = alive;
        t.falling = falling;
        t.fallFrom = fallFrom;
        t.vy = vy;
        t.damageDealt = damageDealt;
        t.kills = kills;
        for (double[] p : lastShot) t.lastShot.add(p.clone());
        return t;
    }

    public Seat seat() {
        return seat;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Where to draw it, between the last two steps. */
    public double y(double alpha) {
        return previousY + (y - previousY) * alpha;
    }

    /** The turret's pivot, where the barrel turns. */
    public double pivotX() {
        return x;
    }

    public double pivotY() {
        return y - Field.PIVOT;
    }

    /** The end of the barrel, where shells leave it. */
    public double muzzleX(double degrees) {
        return pivotX() + Math.cos(Math.toRadians(degrees)) * Field.BARREL;
    }

    public double muzzleY(double degrees) {
        return pivotY() - Math.sin(Math.toRadians(degrees)) * Field.BARREL;
    }

    /** Whether a point is inside the tank. */
    public boolean contains(double px, double py) {
        return Math.abs(px - x) <= Field.TANK_WIDTH / 2 && py <= y && py >= y - Field.TANK_HEIGHT;
    }

    public double centreY() {
        return y - Field.TANK_HEIGHT / 2;
    }

    /** Degrees, 0 to the right, 90 straight up, 180 to the left. */
    public double angle() {
        return angle;
    }

    public double power() {
        return power;
    }

    void aim(double angle, double power) {
        this.angle = Math.max(0, Math.min(180, angle));
        this.power = Math.max(0, Math.min(100, power));
    }

    public Weapon weapon() {
        return weapon;
    }

    /** How many of a weapon are left; -1 for no limit. */
    public int stock(Weapon w) {
        return stock.get(w);
    }

    public boolean has(Weapon w) {
        return stock.get(w) != 0;
    }

    /** Loads the next weapon there is still some of, forwards ({@code 1}) or backwards ({@code -1}). */
    void cycleWeapon(int direction) {
        Weapon[] all = Weapon.values();
        int i = weapon.ordinal();
        for (int n = 0; n < all.length; n++) {
            i = Math.floorMod(i + direction, all.length);
            if (has(all[i])) {
                weapon = all[i];
                return;
            }
        }
    }

    void select(Weapon w) {
        if (has(w)) weapon = w;
    }

    /** One of the loaded weapon is used up; when the last is gone, the plain shell is loaded. */
    void used() {
        int left = stock.get(weapon);
        if (left > 0) stock.put(weapon, left - 1);
        if (!has(weapon)) weapon = Weapon.SHELL;
    }

    public int health() {
        return health;
    }

    public boolean alive() {
        return alive;
    }

    /** Takes damage; returns whether this destroyed it. */
    boolean damage(int amount) {
        if (!alive || amount <= 0) return false;
        health = Math.max(0, health - amount);
        if (health == 0) {
            alive = false;
            return true;
        }
        return false;
    }

    public boolean falling() {
        return falling;
    }

    /** Falls towards {@code support} if the ground under it is gone; returns the depth of a fall that has just ended, else -1. */
    double fall(double support, double gravity, double seconds) {
        previousY = y;
        if (!falling) {
            if (support <= y + 0.01) return -1;
            falling = true;
            fallFrom = y;
            vy = 0;
        }
        vy += gravity * seconds;
        y += vy * seconds;
        if (y < support) return -1;
        y = support;
        falling = false;
        return y - fallFrom;
    }

    public int damageDealt() {
        return damageDealt;
    }

    void dealt(int damage) {
        damageDealt += damage;
    }

    public int kills() {
        return kills;
    }

    void killed() {
        kills++;
    }

    /** Points of the dotted line of this tank's last shot (the middle shell), x and y. */
    public List<double[]> lastShot() {
        return lastShot;
    }

    @Override
    public String toString() {
        return "Tank " + seat.number() + " at " + Math.round(x) + " (" + health + ")";
    }
}
