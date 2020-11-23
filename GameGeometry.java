package com.scottlogic.hackathon.game;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A definition of an area in which games take place, and its geographic logic - for instance, whether it loops at the
 * edges. This includes information and methods relating to the map's overall size and shape, but not the locations of
 * {@linkplain GameState#getOutOfBoundsPositions() obstacles}, {@linkplain GameState#getCollectables() collectables},
 * {@linkplain GameState#getPlayers() players}, or {@linkplain GameState#getSpawnPoints() spawn points}.
 * Information about these elements must be obtained from the {@linkplain GameState},
 * and, unlike this class, will be limited by what your players can see.
 */
@JsonDeserialize(as = LoopingQuadsGameGeometry.class)
public interface GameGeometry {

    /**
     * @return The width of the map
     */
    int getWidth();

    /**
     * @return The height of the map
     */
    int getHeight();

    /**
     * Creates new {@linkplain Position} whose coordinates are <em>equivalent</em> to those given.
     * Depending on the geometry of the map, the coordinates of the resulting Position object may not be identical
     * to those given. For example, if the map's x-coordinates "wrap around" (like longitude on a map of the Earth),
     * and an x-coordinate greater than the maximum is specified, it will be converted to the equivalent value less
     * than the maximum.
     *
     * @param x The (horizontal) x-coordinate of the position to create
     * @param y The (vertical) y-coordinate of the position to create
     * @return A position with equivalent coordinates
     */
    Position getPosition(int x, int y);

    /**
     * Returns all distinct {@linkplain Position}s that can exist within this geometry.
     */
    default Stream<Position> getAllPositions() {
        return IntStream.rangeClosed(0, getWidth())
            .mapToObj(x -> IntStream.rangeClosed(0, getHeight())
                .mapToObj(y -> getPosition(x, y)))
            .flatMap(Function.identity());
    }

    /**
     * Calculates the position on the current map that is displaced by the specified distance and direction from the
     * given position.
     * @param from The position from which to calculate the relative position
     * @param direction The direction of the position to calculate from the <b>from</b> position
     * @param distance The distance of the position to calculate from the <b>from</b> position
     * @return The calculated relative position
     */
    Position getRelativePosition(Position from, Direction direction, int distance);

    /**
     * Determines the position that is next to the given position in the specified direction.
     * @param from The position to find the neighbour of
     * @param direction The direction of the neighbour to find from the <b>from</b> position
     * @return The calculated neighbour
     */
    default Position getNeighbour(Position from, Direction direction) {
        return getRelativePosition(from, direction, 1);
    }

    /**
     * Determines the absolute horizontal distance between two positions.
     * This is defined as smallest number of phases it would take for a player to move from one position until
     * it had the same x-coordinate as the other position,
     * assuming there are no {@linkplain GameState#getOutOfBoundsPositions() obstacles} in the way.
     *
     * @param a One of the positions to find the distance between
     * @param b The other of the positions to find the distance between
     * @return The horizontal distance in number of phases
     */
    int xDistance(Position a, Position b);

    /**
     * Determines the absolute vertical distance between two positions.
     * This is defined as smallest number of phases it would take for a player to move from one position until
     * it had the same y-coordinate as the other position,
     * assuming there are no {@linkplain GameState#getOutOfBoundsPositions() obstacles} in the way.
     *
     * @param a One of the positions to find the distance between
     * @param b The other of the positions to find the distance between
     * @return The vertical distance in number of phases
     */
    int yDistance(Position a, Position b);

    /**
     * Determines the distance between two positions.
     * This is defined as smallest number of phases it would take for a player to move from one position to the other,
     * assuming there are no {@linkplain GameState#getOutOfBoundsPositions() obstacles} between them.
     *
     * @param a One of the positions to find the distance between
     * @param b The other of the positions to find the distance between
     * @return The distance, in number of phases
     */
    default int distance(Position a, Position b) {
        int dx = xDistance(a, b);
        int dy = yDistance(a, b);

        return dx>dy ? dx : dy;
    }

    /**
     * Generates all {@linkplain Direction}s from one position that are towards another.
     * A direction is defined as <em>towards</em> the target position if moving a single step in that direction reduces
     * the {@linkplain #distance(Position, Position) distance} to the target position. It does not take account of
     * any obstacles that may be in the way.
     * <p>
     * Note that the returned stream will often contain more than one direction,
     * as several moves would result in a reduced distance to the target. It may also:
     * <ul>
     *     <li>Be empty - if the target and start point are the same</li>
     *     <li>Contain all directions - if the map is square and the target is on the exact opposite side</li>
     * </ul>
     *
     * @param from The starting position to determine the direction to move from
     * @param towards The target position to reduce the distance to
     * @return A stream of all Directions that are towards the target
     */
    default Stream<Direction> directionsTowards(Position from, Position towards) {
        int distance = distance(from, towards);

        return Stream.of(Direction.values())
                .filter(d -> distance(getNeighbour(from, d), towards) < distance);
    }

    /**
     * Generates all {@linkplain Direction}s from one position that are away from another.
     * A direction is defined as <em>away from</em> the avoided position if moving a single step in that direction
     * increases the {@linkplain #distance(Position, Position) distance} to the avoided position.
     * It does not take account of any obstacles that may be in the way.
     * <p>
     * Note that the returned stream will often contain more than one direction,
     * as several moves would result in an increased distance to the avoided position. It may also:
     * <ul>
     *     <li>Contain all directions - if the target and start point are the same</li>
     *     <li>Be empty - if the map is square and the target is on the exact opposite side</li>
     * </ul>
     *
     * @param from The starting position to determine the direction to move from
     * @param awayFrom The position to move away from (increase the distance to)
     * @return A stream of all Directions that are away from the target
     */
    default Stream<Direction> directionsAwayFrom(Position from, Position awayFrom) {
        int distance = distance(from, awayFrom);

        return Stream.of(Direction.values())
                .filter(d -> distance(getNeighbour(from, d), awayFrom) > distance);
    }

    /** Returns a stream of all positions within a specified distance (inclusively) of an origin position */
    default Stream<Position> getSurroundingPositions(final Position position, final int distance) {
        return IntStream.rangeClosed(position.getX() - distance, position.getX() + distance)
            .mapToObj(x -> IntStream.rangeClosed(position.getY() - distance, position.getY() + distance)
                .mapToObj(y -> getPosition(x, y)))
            .flatMap(Function.identity());
    }

    /**
     * Creates a {@linkplain Route} through this map, starting from the given position and taking a single step
     * in each of the given directions.
     *
     * @param start The starting position of the route
     * @param route The sequence of steps determining the route
     * @return The resulting route
     */
    default Route route(Position start, List<Direction> route) {
        Position dest = start;
        for(Direction dir: route) {
            dest = getNeighbour(dest, dir);
        }
        return new RouteImpl(this, start, dest, route);
    }

    /**
     * Creates a {@linkplain Route} through this map, starting from the given position and taking the given number of
     * steps in the given direction.
     *
     * @param start The starting position of the route
     * @param direction The direction to move in from the starting position
     * @param length The numper of steps to move
     * @return The resulting route
     */
    default Route straightLineRoute(Position start, Direction direction, int length) {
        return new RouteImpl(this, start, getRelativePosition(start, direction, length), direction, length);
    }

    /**
     * Finds (one of) the shortest route between the two given {@linkplain Position positions},
     * avoiding any positions that match the given predicate.
     * This method uses the <a href="https://en.wikipedia.org/wiki/A*_search_algorithm">A* search algorithm</a>.
     * <p>
     * It is possible that no route will be found,
     * if the set of positions that must be avoided forms an unbroken barrier between the start and target positions.
     * In this situation, and empty {@linkplain Optional} will be returned.
     *
     * @param from The Position to find a route from
     * @param to The Position to find a route to
     * @param avoid A {@linkplain Predicate} specifying which positions to avoid.
     *              The found route is guaranteed not to include any positions for which this returns {@code true}.
     * @return An Optional containing an ordered list of the directions to move in to traverse the found route,
     *         or {@linkplain Optional#empty() empty} if no route could be found
     *
     * @see #findRoute(Position, Position, Collection)
     */
    default Optional<Route> findRoute(Position from, Position to, Predicate<Position> avoid) {
        return Graph.findRoute(this, from, to, avoid);
    }

    /**
     * Finds (one of) the shortest route between the two given {@linkplain Position positions},
     * avoiding any positions in the specified collection.
     * This method uses the <a href="https://en.wikipedia.org/wiki/A*_search_algorithm">A* search algorithm</a>.
     * <p>
     * It is possible that no route will be found,
     * if the set of positions that must be avoided forms an unbroken barrier between the start and target positions.
     * In this situation, and empty {@linkplain Optional} will be returned.
     *
     * @param from The Position to find a route from
     * @param to The Position to find a route to
     * @param avoid A {@linkplain Collection} of positions to avoid.
     *              The found route is guaranteed not to include any positions contained here.
     * @return An Optional containing an ordered list of the directions to move in to traverse the found route,
     *         or {@linkplain Optional#empty() empty} if no route could be found
     *
     * @see #findRoute(Position, Position, Predicate)
     */
    default Optional<Route> findRoute(Position from, Position to, Collection<Position> avoid) {
        return findRoute(from, to, avoid::contains);
    }
}
