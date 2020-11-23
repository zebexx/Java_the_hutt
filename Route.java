package com.scottlogic.hackathon.game;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a route that can be taken through a map.
 */
public interface Route extends Iterable<Position> {

    /**
     * @return the number of steps required to traverse the route
     */
    int getLength();

    /**
     * @return the starting position of the route
     */
    Position getStart();

    /**
     * @return the ending position of the route
     */
    Position getDestination();

    /**
     * Gets the {@linkplain Direction} that the route moves away from the {@linkplain #getStart() starting position}.
     * If this route has zero length (i.e. is <em>just</em>  the starting position),
     * then an {@linkplain Optional#empty() empty Optional} will be returned.
     *
     * @return The first direction to move in
     */
    Optional<Direction> getFirstDirection();

    /**
     * Gets the sub-route starting from the second position along this route.
     * In other words, this imagines that one step has been taken along this route and returns the portion of the
     * route still to travel.
     * If this route has zero length (i.e. is <em>just</em>  the starting position),
     * then an {@linkplain Optional#empty() empty Optional} will be returned.
     *
     * @return The sub-route starting from the second position
     */
    Optional<Route> step();

    /**
     * @return An ordered {@linkplain Spliterator} over the directional steps in this route
     */
    Spliterator<Direction> directionSpliterator();

    /**
     * @return An {@linkplain Iterator} over the directional steps in this route
     */
    Iterator<Direction> directionIterator();

    /**
     * @return An ordered {@linkplain Stream} of the directional steps in this route
     */
    default Stream<Direction> streamDirections() {
        return StreamSupport.stream(directionSpliterator(), false);
    }

    /**
     * @return An ordered {@linkplain Stream} of the positions visited by this route
     */
    default Stream<Position> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Determines whether this route collides with (traverses) any position that is satisfied by the given predicate.
     *
     * @param obstacles A {@linkplain Predicate} determining whether positions are collision obstacles
     * @return {@code true} iff any of the positions along this route collide with the specified obstacles
     *
     * @see #collides(Collection)
     * @see #collides(Route)
     */
    default boolean collides(Predicate<? super Position> obstacles) {
        return stream().anyMatch(obstacles);
    }

    /**
     * Determines whether this route collides with (traverses) any of the specified obstacle positions.
     *
     * @param obstacles A {@linkplain Predicate} determining whether positions are collision obstacles
     * @return {@code true} iff any of the positions along this route collide with the specified obstacles
     *
     * @see #collides(Predicate)
     * @see #collides(Route)
     */
    default boolean collides(Collection<Position> obstacles) {
        return collides(obstacles::contains);
    }

    /**
     * Determines whether this route and the given route collide.
     * Two routes are said to collide <em>only</em> if they traverse the same position at the same 'time'.
     * Formally, two routes, {@code A} and {@code B} are said to <em>collide</em> if there is a position {@code x}
     * and an integer {@code k} such that {@code k}th position along both {@code A} and {@code B} is {@code x}.
     *
     * @param other The other route to check for collision with
     * @return {@code true} iff this route <em>collides</em> with the other, as defined above
     */
    default boolean collides(Route other) {
        final Iterator<Position> itThis=this.iterator();
        final Iterator<Position> itOther=other.iterator();
        while(itThis.hasNext() && itOther.hasNext()) {
            if(itThis.next().equals(itOther.next())) {
                return true;
            }
        }
        return false;
    }

}
