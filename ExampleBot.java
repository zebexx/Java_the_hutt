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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

//TODO: 
//Player usernames? xX_Noob_bot_Pwner69_Xx 
//Teleportation????? 
//Cheatcodes???? 
//was there a rule about not creating more class files? 

//Improve exploration 
// - I think avoid teammates might be causing more problems than it solves, players get stuck in an area just avoiding allies and not doing anything
// - Maybe change avoid players to only apply when they are far enough away from spawnpoint.
// - Change extractMoves to only give diagonal directions.
// - Improve moveRandomly to distribute directions more evenly (bad to for players to randomly all go in the same direction) maybe just for first move
// - Route
//      - Implement Route class, make sure we're avoiding Stackoverflow errors. Not using route when near water etc
//      - Maybe add route to Hashmap somehow so that we dont have to calculate a new route every turn and players can follow a route to completion 
// - Maybe dont go near water if players can see it, not just dont walk into the water if its on the nextPosition

//Fighting 
// - run away in bad fight 
// - run towards enemy if we have more allies 
// - attack enemy spawn point if no enemy players are on it 
// - if home is destroyed 
//      - stop collecting food 
//      - group up and hunt enemies/enemy spawn point 
// - defend spawnpoint? 
// - blocking enemies from areas? eg blocking gap between water (depends on map) 
// - form small squads when near enemies?
// - form squad after a number of turns without finding food?
// - destroy other spawnpoint 

public class ExampleBot extends Bot {

    private HashMap<Id, Direction> playerDirectionHashMap;
    private GameStateLogger.GameStateLoggerBuilder gameStateLoggerBuilder;
    private List<Position> nextPositions;
    private Set<SpawnPoint> spawns;
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
                Direction newDirection = Direction.random();
                while (newDirection == Direction.NORTH || newDirection == Direction.SOUTH
                        || newDirection == Direction.EAST || newDirection == Direction.WEST) {
                    newDirection = Direction.random();
                }
                playerDirectionHashMap.put(playerID, Direction.random());
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
        gameStateLoggerBuilder.withPlayers().withOutOfBounds().process(gameState);
        moveRandomly(gameState);
        avoidPlayers(gameState);
        collectFood(gameState);
        List<Move> moves = extractMoves(gameState);
        return moves;
    }

    @Override
    public void initialise(GameState gameState) {
        playerDirectionHashMap = new HashMap<>();
        findSpawnPoint(gameState);
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
                boolean sameDirection = true;
                Direction newDirection = Direction.random();
                while (sameDirection) {
                    if (!newDirection.equals(direction) && canMove(gameState, player, newDirection)) {
                        sameDirection = false;
                    } else {
                        newDirection = Direction.random();
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
        // checks new position will not result in a collision with other players or water
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
        ArrayList<Position> claimedFoodPositions = new ArrayList<>(); //list of food which has been asigned to a player
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

    private void spawnpoints(GameState gameState) {
        Set<SpawnPoint> spawnPoints = gameState.getSpawnPoints();
        this.spawns = spawnPoints;
    }

    private void findSpawnPoint(GameState gameState) {
        Set<SpawnPoint> spawnPoints = gameState.getSpawnPoints();
        for (SpawnPoint spawnPoint: spawnPoints) {
            Id owner = spawnPoint.getOwner();
            
            if (owner.equals(getId())) {
                this.home = spawnPoint;
            }
            else if (!owner.equals(getId())) {
                this.enemy = spawnPoint;
            }
        }
    }

    private Optional<Route> makeRoute(GameState gameState, Player player, Position futurePosition) {
        Position currentPosition = player.getPosition();
        Set<Position> avoid = Collections.emptySet();
        Optional<Route> route = gameState.getMap().findRoute(player.getPosition(), futurePosition, avoid);
        avoid = gameState.getOutOfBoundsPositions();
        boolean invalidRoute = true;
        if (route.isPresent()) {
            invalidRoute = route.get().collides(avoid);
            if (invalidRoute) {
                route.empty();
            }
        }
        return route;
    }

    private void fighting(GameState gameState) {
        
    }
}
