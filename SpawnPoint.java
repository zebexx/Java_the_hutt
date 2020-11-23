package com.scottlogic.hackathon.game;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.Optional;


/**
 * A spawn point in the game.
 */
@Value
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SpawnPoint {
    /**
     * @return The unique id of the current spawn point.
     */
    @EqualsAndHashCode.Include
    private Id id;
    /**
     * @return The position of the current spawn point.
     */
    private Position position;
    /**
     * @return The unique id of the owner of the current spawn point.
     */
    private Id owner;
    @NonFinal
    @Getter(AccessLevel.NONE)
    private int queuedPlayers;

    public SpawnPoint(Id id, final Position position, final Id owner, final int initialPlayers) {
        this.id = id;
        this.position = position;
        this.owner = owner;
        this.queuedPlayers = initialPlayers;
    }

    /**
     * Queue a player for spawning.
     */
    public void queuePlayer() {
        queuedPlayers++;
    }

    public Optional<Player> createPlayerIfAble(Id id) {
        if (queuedPlayers == 0) {
            return Optional.empty();
        }

        queuedPlayers--;
        return Optional.of(
                new Player(id, owner, position));
    }
}
