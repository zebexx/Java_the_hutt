package com.scottlogic.hackathon.game;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The directions a player can move.
 */
public enum Direction {
    NORTH(    true,  false, false, false),
    NORTHEAST(true,  false, true,  false),
    EAST(     false, false, true,  false),
    SOUTHEAST(false, true,  true,  false),
    SOUTH(    false, true,  false, false),
    SOUTHWEST(false, true,  false, true ),
    WEST(     false, false, false, true ),
    NORTHWEST(true,  false, false, true );

    private final boolean northward, southward, eastward, westward;

    Direction(boolean northward, boolean southward, boolean eastward, boolean westward) {
        this.northward = northward;
        this.southward = southward;
        this.eastward = eastward;
        this.westward = westward;
    }

    /**
     * Determines whether moving in this direction will result in being further North.
     * @return {@code true} iff moving in this direction will result in being further North.
     */
    public boolean isNorthward() {
        return northward;
    }

    /**
     * Determines whether moving in this direction will result in being further South.
     * @return {@code true} iff moving in this direction will result in being further South.
     */
    public boolean isSouthward() {
        return southward;
    }

    /**
     * Determines whether moving in this direction will result in being further East.
     * @return {@code true} iff moving in this direction will result in being further East.
     */
    public boolean isEastward() {
        return eastward;
    }

    /**
     * Determines whether moving in this direction will result in being further West.
     * @return {@code true} iff moving in this direction will result in being further West.
     */
    public boolean isWestward() {
        return westward;
    }

    /**
     * Gets the opposite direction to this one.
     * @return The opposite direction
     */
    public Direction getOpposite() {
        Direction[] vals = values();
        int opp = vals.length / 2;
        assert opp*2 == vals.length;
        return vals[(ordinal()+opp) % vals.length];
    }

    /**
     * @return A random direction
     */
    public static Direction random() {
        Direction[] vals = values();
        return vals[ThreadLocalRandom.current().nextInt(vals.length)];
    }

    public static List<Direction> randomisedValues() {
        final List<Direction> directions = Arrays.asList(values());
        Collections.shuffle(directions);
        return directions;
    }

}
