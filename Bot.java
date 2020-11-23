package com.scottlogic.hackathon.game;

import lombok.Getter;

import java.util.List;
import java.util.Objects;


/**
 * Represents the overall strategy of an individual contestant in a game.
 * A Bot's primary function is to determine a set of {@linkplain Move Moves} to make at each stage in the game.
 * It does this by implementing the {@link #makeMoves(GameState)} method.
 */
public abstract class Bot {

    @Getter
    private final Id id;
    @Getter
    private final String displayName;

    /**
     * Super-constructor.
     * @param displayName The human-readable name for the bot
     */
    protected Bot(String displayName) {
        id = UniqueIdGenerator.INSTANCE.next();
        this.displayName = Objects.requireNonNull(displayName);
    }

    protected Bot(Id id, String displayName) {
        this.id =  Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
    }

    /**
     * Sets up any initial fields or data to be used by this bot later on in the game.
     * This method is called once at the beginning of the game, before the first call to {@link #makeMoves(GameState)}.
     * The given {@linkplain GameState} will be incomplete,
     * defined by what this bot's first Player can 'see' from its starting position.
     * <p>
     * The default implementation of this method does nothing. Subclasses are free to override it.
     *
     * @param initialGameState This bot's 'view' of the initial state of the game
     */
    public void initialise(final GameState initialGameState) {
    }

    /**
     * Determines the set of moves the bot's {@linkplain Player Players} should make in response to the given state.
     * The given {@linkplain GameState} will typically be incomplete,
     * defined by what this bot's Players can 'see' from their current positions.
     * <p>
     * The resulting list of moves must:
     * <ul>
     *     <li>Only refer to this bot's Players</li>
     *     <li>Not refer to the same Player multiple time</li>
     *     <li>Not be {@code null}</li>
     *     <li>Not contain any {@code null} values</li>
     * </ul>
     * If any of these criteria are not met, then this bot may be disqualified from the game,
     * forfeiting it to other contestants.
     * <p>
     * The list <em>may</em> be empty, in which case all of this bot's Players will remain stationary.
     *
     * @param gameState This bot's 'view' of the current state of the game
     * @return The moves this bot's players should make on the next phase
     *
     * @see #initialise(GameState)
     */
    public abstract List<Move> makeMoves(GameState gameState);




}
