package com.contestantbots.team;

import com.contestantbots.util.GameStateLogger;
import com.scottlogic.hackathon.game.Bot;
import com.scottlogic.hackathon.game.Direction;
import com.scottlogic.hackathon.game.GameState;
import com.scottlogic.hackathon.game.Move;
import com.scottlogic.hackathon.game.MoveImpl;
import com.scottlogic.hackathon.game.Player;
import com.scottlogic.hackathon.game.Id;
import com.scottlogic.hackathon.game.Position;
import com.scottlogic.hackathon.game.Collectable;
import com.scottlogic.hackathon.game.SpawnPoint;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

//TODO:
//pick a random direction when setting out & stick to it until finding food or out of bounds etc
//make players avoid collisions
//make players go in another direction if there's no food around
//decide to go to the nearest food object that isn't out of bounds
//stop multiple players from going to the same food item
//fighting


public class ExampleBot extends Bot {

    private HashMap<Id,Direction> playerDirectionHashMap;
    private GameStateLogger.GameStateLoggerBuilder gameStateLoggerBuilder;
    private List<Position> nextPositions;
    private SpawnPoint home;

    public ExampleBot(String name) {
        super(name);
        gameStateLoggerBuilder = GameStateLogger.configure(getId());
        home = gameState.getSpawnPoints();
    }

    private void moveRandomly(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            Id playerID = player.getId();
            if (isMyPlayer(player) && player.getPosition().equals()) {
                playerDirectionHashMap.put(playerID, Direction.random());
            }
        }
    }

    @Override
    public List<Move> makeMoves(final GameState gameState) {
        nextPositions = new ArrayList<>();
        removeDeadPlayers(gameState);
        gameStateLoggerBuilder.withPlayers().withOutOfBounds().process(gameState);
        moveRandomly(gameState);
        collectFood(gameState);
        List<Move> moves = extractMoves(gameState);
        return moves;
    }

    @Override
    public void initialise(GameState gameState) {
        playerDirectionHashMap = new HashMap<>();
        //spawnpoint
    }

    private List<Move> extractMoves(GameState gameState) {
        List<Move> moves = new ArrayList<>();
        for (Entry<Id, Direction> item : playerDirectionHashMap.entrySet()) {
            Id playerID = item.getKey();
            Direction direction = item.getValue();
            Player player = findPlayerByID(gameState, playerID);
            if (player != null && canMove(gameState, player, direction)) {
                moves.add(new MoveImpl(playerID, direction));
                Position newPosition = gameState.getMap().getNeighbour(player.getPosition(), direction);
                nextPositions.add(newPosition);
            } else if (player != null && canMove(gameState, player, direction.getOpposite())) {
                // Player needs to switch to opposite direction to move
                moves.add(new MoveImpl(playerID, direction.getOpposite()));
                Position newPosition = gameState.getMap().getNeighbour(player.getPosition(), direction.getOpposite());
                nextPositions.add(newPosition);
            } else if (player != null && !nextPositions.isEmpty()) {
                // Player cannot move
                Position oldPosition = nextPositions.get(nextPositions.size()-1);
                nextPositions.add(oldPosition);
            }
        }
        return moves;
    }

    private void removeDeadPlayers(GameState gameState) {
        // Remove dead players from the HashMap
        for (Player p : gameState.getRemovedPlayers()) {
            playerDirectionHashMap.remove(p.getId());
        }
    }
    
    private boolean canMove(final GameState gameState, final Player player, final Direction direction) {
        Set<Position> outOfBounds = gameState.getOutOfBoundsPositions();
        Position newPosition = gameState.getMap().getNeighbour(player.getPosition(), direction);
        if (!nextPositions.contains(newPosition) && !outOfBounds.contains(newPosition)) {
            return true;
        } else {
            return false;
        }
    }
    
    private Player findPlayerByID(GameState gameState, Id id) {
        for (Player player : gameState.getPlayers()) {
            if (player.getId().equals(id)) {
                return player;
            }
        }
        return null;
    }
    
    private boolean isMyPlayer(Player player) {
        return player.getOwner().equals(getId());
    }
    
    private void collectFood(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            if (isMyPlayer(player)) {
                for (Collectable food : gameState.getCollectables()) {
                    int distanceToFood = gameState.getMap().distance(player.getPosition(), food.getPosition());
                    if (distanceToFood < 10) {
                        Optional<Direction> direction = gameState.getMap()
                                .directionsTowards(player.getPosition(), food.getPosition()).findFirst();
                        if (direction.isPresent()) {
                            playerDirectionHashMap.put(player.getId(), direction.get());
                        }
                    }
                }
            }
        }
    }

    private void avoidPlayers(GameState gameState) {
        for(Player player1 : gameState.getPlayers()) {
            if (isMyPlayer(player1)) {
                for (Player player2 : gameState.getPlayers()) {
                    int distanceToPlayer = gameState.getMap().distance(player1.getPosition(), player2.getPosition());
                    int closestDistanceToPlayer = 11;
                    Position closestPlayerPosition = null;
                    if (distanceToPlayer < 10 && distanceToPlayer<closestDistanceToPlayer) {
                        closestDistanceToPlayer = distanceToPlayer;
                        closestPlayerPosition = player2.getPosition();

                    }
                }
                if(!closestPlayerPosition.isNull()) {
                    Optional<Direction> direction = gameState.getMap().directionTowards(
                            player1.getPosition(), player2.getPosition()).findFirst();
                    if (direction.isPresent()) {
                        playerDirectionHashMap.put(player1.getId(), direction.get());
                    }
                }
            }
        }
    }
    private boolean isMySpawnPoint(SpawnPoint spawnPoint) {
        return true;
    }
}