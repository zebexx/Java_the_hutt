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
import com.scottlogic.hackathon.game.Route;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

//TODO: 
//Player usernames? xX_Noob_bot_Pwner69_Xx 
//Teleportation????? 
//Cheatcodes???? 
//was there a rule about not creating more class files? 
//improve exploration 
//fighting 
// - run away in bad fight 
// - run towards enemy if have more allies 
// - attack enemy spawn point if no enemy players are on it 
// - if home is destroyed 
//      - stop collecting food 
//      - group up and hunt enemies/enemy spawn point 
// - defend spawnpoint? 
// - blocking enemies from areas? eg blocking gap between water (depends on map) 
//destroy other spawnpoint 

public class ExampleBot extends Bot {

    private HashMap<Id, Direction> playerDirectionHashMap;
    private GameStateLogger.GameStateLoggerBuilder gameStateLoggerBuilder;
    private List<Position> nextPositions;
    private SpawnPoint home;
    private SpawnPoint enemy;

    public ExampleBot(String name) {
        super(name);
        gameStateLoggerBuilder = GameStateLogger.configure(getId());
    }

    private void moveRandomly(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            Id playerID = player.getId();
            if (isMyPlayer(player) && !playerDirectionHashMap.containsKey(playerID)) {
                ArrayList<Direction> diagonalDirections = new ArrayList<>();
                diagonalDirections.add(Direction.NORTHEAST);
                diagonalDirections.add(Direction.NORTHWEST);
                diagonalDirections.add(Direction.SOUTHEAST);
                diagonalDirections.add(Direction.SOUTHWEST);
                Random random = new Random();
                int rIndex = (random.nextInt(diagonalDirections.size()));
                Direction newDirection = diagonalDirections.get(rIndex);
                playerDirectionHashMap.put(playerID, newDirection);
            } else if (isMyPlayer(player)) {
                Direction oldDirection = playerDirectionHashMap.get(playerID);
                playerDirectionHashMap.put(playerID, oldDirection);
            }
        }
    }

    @Override
    public List<Move> makeMoves(final GameState gameState) {
        nextPositions = new ArrayList<>();
        removeDeadPlayers(gameState);
        findSpawnPoint(gameState);
        gameStateLoggerBuilder.withPlayers().withOutOfBounds().process(gameState);
        moveRandomly(gameState);
        avoidPlayers(gameState);
        collectFood(gameState);
        fighting(gameState);
        List<Move> moves = extractMoves(gameState);
        return moves;
    }

    @Override
    public void initialise(GameState gameState) {
        playerDirectionHashMap = new HashMap<>();
    
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
            } else if (player != null) {
                // Player needs to switch to another random direction to move
                List<Direction> allDirections = Direction.randomisedValues();
                allDirections.remove(direction);
                Direction newDirection = direction; // initialise newDirection
                for (Direction d : allDirections) {
                    if (direction.equals(d)) {
                        allDirections.remove(d);
                    } else if (canMove(gameState, player, d)) {
                        newDirection = d;
                        break;
                    }
                }
                moves.add(new MoveImpl(playerID, newDirection));
                playerDirectionHashMap.put(playerID, newDirection);
                Position newPosition = gameState.getMap().getNeighbour(player.getPosition(), newDirection);
                nextPositions.add(newPosition);
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
        ArrayList<Position> claimedFoodPositions = new ArrayList<>();
        for (Player player : gameState.getPlayers()) {
            if (isMyPlayer(player)) {
                Position closestFood = null;
                int closestDistanceToFood = 11;
                for (Collectable food : gameState.getCollectables()) {
                    int distanceToFood = gameState.getMap().distance(player.getPosition(), food.getPosition());
                    // try {
                    // Optional<Route> route = gameState.getMap().findRoute(player.getPosition(),
                    // food.getPosition(),
                    // gameState.getOutOfBoundsPositions());
                    // if (route.isPresent()) {
                    // Route r = route.get();
                    // int routeDistance = r.getLength();
                    // if (distanceToFood < 10 &&
                    // !(claimedFoodPositions.contains(food.getPosition()))) {
                    // if (closestDistanceToFood > distanceToFood && routeDistance < 21) {
                    // closestDistanceToFood = distanceToFood;
                    // closestFood = food.getPosition();
                    // }
                    // }
                    // }
                    // else {
                    // break;
                    // }
                    // }
                    // catch (StackOverflowError s) {
                    // }

                    if (distanceToFood < 10 && !(claimedFoodPositions.contains(food.getPosition()))) {
                        if (closestDistanceToFood > distanceToFood) {
                            closestDistanceToFood = distanceToFood;
                            closestFood = food.getPosition();
                        }
                    }
                }

                if (!(closestFood == null)) {
                    Optional<Direction> direction = gameState.getMap()
                            .directionsTowards(player.getPosition(), closestFood).findFirst();
                    // Optional<Route> route = gameState.getMap().findRoute(player.getPosition(),
                    // closestFood,
                    // gameState.getOutOfBoundsPositions());
                    if (direction.isPresent()) {
                        // Route r = route.get();
                        playerDirectionHashMap.put(player.getId(), direction.get());
                    }
                }
            }
        }
    }

    private void avoidPlayers(GameState gameState) {
        for (Player player1 : gameState.getPlayers()) {
            if (isMyPlayer(player1)) {
                Position closestPlayer = null;
                int closestDistanceToPlayer = 11;
                for (Player player2 : gameState.getPlayers()) {
                    if (isMyPlayer(player2)) {
                        int distanceToPlayer = gameState.getMap().distance(player1.getPosition(),
                                player2.getPosition());
                        if (player1.getId().equals(player2.getId())) {
                            break;
                        } else if (distanceToPlayer < 10 && distanceToPlayer < closestDistanceToPlayer) {
                            if (playerDirectionHashMap.get(player1.getId())
                                    .equals(playerDirectionHashMap.get(player2.getId()))) {
                                break;
                            }
                            closestDistanceToPlayer = gameState.getMap().distance(player1.getPosition(),
                                    player2.getPosition());
                            closestPlayer = player2.getPosition();
                        }
                    }
                }
                if (!(closestPlayer == null)) {
                    Optional<Direction> direction = gameState.getMap()
                            .directionsTowards(player1.getPosition(), closestPlayer).findFirst();
                    if (direction.isPresent()) {
                        playerDirectionHashMap.put(player1.getId(), direction.get().getOpposite());
                    }
                }
            }
        }
    }
    
    private boolean isMySpawn(SpawnPoint spawn) {
        return spawn.getOwner().equals(getId());
    }

    
    private void findSpawnPoint(GameState gameState) {
        Set<SpawnPoint> spawnPoints = gameState.getSpawnPoints();
        for (SpawnPoint spawnPoint : spawnPoints) {
            if (isMySpawn(spawnPoint)) {
                this.home = spawnPoint;
            } else {
                this.enemy = spawnPoint;
            }
        }

        if (this.enemy != null && !gameState.getSpawnPointAt(enemy.getPosition()).isPresent()) {
            enemy = null;
        }

        if (this.home != null && !gameState.getSpawnPointAt(home.getPosition()).isPresent()) {
            home = null;
        }
    }
    
    private void fighting(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            if (isMyPlayer(player)) {
                // destroys enemy spawnpoint
                if (enemy != null) {
                    if (gameState.getMap().distance(enemy.getPosition(), player.getPosition()) < 10) {
                        
                        Optional<Direction> direction = gameState.getMap().directionsTowards(player.getPosition(),
                                enemy.getPosition()).findFirst();
                        if (direction.isPresent()) {
                            playerDirectionHashMap.put(player.getId(), direction.get());
                        }
                    }
                }

                for (Player enemyP : gameState.getPlayers()) {
                    if (!isMyPlayer(enemyP)
                            && gameState.getMap().distance(enemyP.getPosition(), player.getPosition()) < 10) {
                        boolean alone = true;
                        int distanceToEnemy = gameState.getMap().distance(enemyP.getPosition(), player.getPosition());

                        for (Player friend : gameState.getPlayers()) {
                            if (isMyPlayer(friend) && gameState.getMap().distance(enemyP.getPosition(),
                                    friend.getPosition()) <= distanceToEnemy) {
                                alone = false;
                                break;
                            }
                        }

                        // TODO: make sure that both the player & the friend go towards the same enemy +
                        // case where there's more than one enemy
                        // check that they can both go towards the enemy (route)

                        if (alone) {
                            Optional<Direction> away = gameState.getMap()
                                    .directionsAwayFrom(player.getPosition(), enemyP.getPosition()).findFirst();
                            playerDirectionHashMap.put(player.getId(), away.get());
                        } else {
                            Optional<Direction> attackDirection = gameState.getMap()
                                    .directionsTowards(player.getPosition(), enemyP.getPosition()).findFirst();
                            playerDirectionHashMap.put(player.getId(), attackDirection.get());
                            
                        }
                    }
                }
            }
        }
    }
}
