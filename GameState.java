package com.scottlogic.hackathon.game;

import lombok.Value;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * A 'view' of the state of a game.
 * Instances of this are often not complete,
 * instead corresponding to what a particular {@linkplain Bot}'s {@linkplain Player Players} can 'see'.
 */
@Value
public class GameState {
    /**
     * @return The current phase of the game. The phase starts at 0 and simply counts up
     * during the game.
     */
    int phase;
    /**
     * @return The game's map.
     */
    GameGeometry map;
    Set<Position> outOfBoundsPositions;
    Set<Player> players;
    Set<Player> removedPlayers;
    Set<SpawnPoint> spawnPoints;
    Set<SpawnPoint> removedSpawnPoints;
    Set<Collectable> collectables;

    /**
     * @return The active players that are in the current game.
     */
    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * @return The dead players that were removed after the previous phase.
     */
    public Set<Player> getRemovedPlayers() {
        return Collections.unmodifiableSet(removedPlayers);
    }

    /**
     * @return The active spawn points in the current game.
     */
    public Set<SpawnPoint> getSpawnPoints() {
        return Collections.unmodifiableSet(spawnPoints);
    }

    /**
     * @return The destroyed spawn points that were removed after the previous phase.
     */
    public Set<SpawnPoint> getRemovedSpawnPoints() {
        return Collections.unmodifiableSet(removedSpawnPoints);
    }

    /**
     * @return The collectable items that are in the current game.
     */
    public Set<Collectable> getCollectables() {
        return Collections.unmodifiableSet(collectables);
    }

    /**
     * @return The out of bounds positions for the current game.
     */
    public Set<Position> getOutOfBoundsPositions() {
        return Collections.unmodifiableSet(outOfBoundsPositions);
    }


    /**
     * Checks whether the given position is out of bounds.
     * @param position The position to check
     * @return {@code true} iff the position is out of bounds
     */
    public boolean isOutOfBounds(Position position) {
        return getOutOfBoundsPositions().contains(position);
    }

    /**
     * Gets the {@linkplain Player} at the given position, if there is one.
     * @param position The position to get the player at
     * @return The player at the requested position,
     *         or an {@linkplain Optional#empty() empty Optional} if there is none
     */
    public Optional<Player> getPlayerAt(Position position) {
        return getPlayers().parallelStream()
                .filter(p -> p.getPosition().equals(position))
                .findAny();
    }

    /**
     * Gets the {@linkplain SpawnPoint} at the given position, if there is one.
     * @param position The position to get the SpawnPoint at
     * @return The SpawnPoint at the requested position,
     * or an {@linkplain Optional#empty() empty Optional} if there is none
     */
    public Optional<SpawnPoint> getSpawnPointAt(Position position) {
        return getSpawnPoints().parallelStream()
                .filter(p -> p.getPosition().equals(position))
                .findAny();
    }

    /**
     * Gets the {@linkplain Collectable} at the given position, if there is one.
     * @param position The position to get the Collectable at
     * @return The Collectable at the requested position,
     * or an {@linkplain Optional#empty() empty Optional} if there is none
     */
    public Optional<Collectable> getCollectableAt(Position position) {
        return getCollectables().parallelStream()
                .filter(p -> p.getPosition().equals(position))
                .findAny();
    }

    /**
     * Determines if the given position is accessible (i.e. not {@linkplain #isOutOfBounds(Position) out of bounds})
     * and doesn't contain a player, spawn point, or collectable.
     * @param position The position to check
     * @return {@code true} iff the givien position is empty
     */
    public boolean isEmpty(Position position) {
        return !(isOutOfBounds(position)
                || getPlayerAt(position).isPresent()
                || getSpawnPointAt(position).isPresent()
                || getCollectableAt(position).isPresent());
    }

    public boolean isOwnerInactive(Id ownerId) {
        return getPlayers().stream().noneMatch(p -> p.getOwner().equals(ownerId));
    }

}
