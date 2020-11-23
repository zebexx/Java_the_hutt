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
import com.scottlogic.hackathon.game.GameGeometry;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.*;

//TODO: 
//Player usernames? xX_Noob_bot_Pwner69_Xx 
//Teleportation????? 
//Cheatcodes???? 
//was there a rule about not creating more class files? 

//Improve exploration 
// - Instead if getting a new random direction when hitting an obstactle, correct the route but carry on in the same general direction
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
    private HashMap<Id, Route> playerRouteHashMap;
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
            if (isMyPlayer(player) && !playerRouteHashMap.containsKey(playerID)) {
                Optional<Route> route = randomRoute(gameState, player, routePositions(gameState, player));
                
                playerRouteHashMap.put(playerID, route.get());
                //playerDirectionHashMap.put(playerID, Direction.random());
            }/*  else if (isMyPlayer(player)) {
                Direction oldDirection = playerDirectionHashMap.get(playerID);
                playerDirectionHashMap.put(playerID, oldDirection);
            } */
        }
    }

    @Override
    public List<Move> makeMoves(final GameState gameState) {
        findSpawnPoint(gameState);
        nextPositions = new ArrayList<>();
        removeDeadPlayers(gameState);
        gameStateLoggerBuilder.process(gameState);
        moveRandomly(gameState);
        //avoidPlayers(gameState);
        collectFood(gameState);
        fighting(gameState);
        List<Move> moves = extractMoves(gameState);
        return moves;
    }

    @Override
    public void initialise(GameState gameState) {
        playerDirectionHashMap = new HashMap<>();
        playerRouteHashMap = new HashMap<>();
    }

    private List<Move> extractMoves(GameState gameState) {
        List<Move> moves = new ArrayList<>();

        if (!playerRouteHashMap.isEmpty()) {
            ArrayList<Id> removeFromHashMap = new ArrayList<>();

            for (Entry<Id, Route> item : playerRouteHashMap.entrySet()) {
                Id playerID = item.getKey();
                Route route = item.getValue();
                Player player = findPlayerByID(gameState, playerID);
                Optional<Direction> newDirection = route.getFirstDirection();
                route.step();
                if (player != null && newDirection.isPresent()) {
                    playerDirectionHashMap.put(playerID, newDirection.get());
                } else {
                    removeFromHashMap.add(playerID);
                }
            }
            for (Id id : removeFromHashMap) {
                playerDirectionHashMap.remove(id);
            }
        }
        
        for (Entry<Id, Route> item : playerRouteHashMap.entrySet()) {
            Id playerID = item.getKey();
            Route route = item.getValue();
            Player player = findPlayerByID(gameState, playerID);
            if (player != null && canMove(gameState, player, route.getFirstDirection().get())) {
                moves.add(new MoveImpl(playerID, route.getFirstDirection().get()));
                Position newPosition = gameState.getMap().getNeighbour(player.getPosition(), route.getFirstDirection().get());
                nextPositions.add(newPosition);
            } else if (player != null) {
                // Player needs to switch to another direction to move
                //boolean sameDirection = true;
                Optional<Route> newRoute = randomRoute(gameState, player, routePositions(gameState, player));
                Direction newdirection = newRoute.get().getFirstDirection().get();

                while (!canMove(gameState, player, newdirection)) {
                    newRoute = randomRoute(gameState, player, routePositions(gameState, player));
                }
                playerRouteHashMap.put(playerID, newRoute.get());
                /* Direction newDirection = Direction.random();
                while (sameDirection) {
                    if (!newDirection.equals(direction) && canMove(gameState, player, newDirection)) {
                        sameDirection = false;
                    } else {
                        //change this to stay on course
                        newDirection = Direction.random();
                        while (newDirection == Direction.NORTH || newDirection == Direction.SOUTH
                                || newDirection == Direction.EAST || newDirection == Direction.WEST) {
                            newDirection = Direction.random();
                        }
                    }
                } */
                
                
                moves.add(new MoveImpl(playerID, newRoute.get().getFirstDirection().get()));
                //playerDirectionHashMap.put(playerID, newDirection);
                Position newPosition = gameState.getMap().getNeighbour(player.getPosition(), newRoute.get().getFirstDirection().get());
                nextPositions.add(newPosition);
            }
        }
        return moves;
    }

    private void removeDeadPlayers(GameState gameState) {
        // Remove dead players from the HashMap
        for (Player p : gameState.getRemovedPlayers()) {
            playerDirectionHashMap.remove(p.getId());
            playerRouteHashMap.remove(p.getId());
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

                    if (distanceToFood < 10 && !(claimedFoodPositions.contains(food.getPosition()))) {
                        if (closestDistanceToFood > distanceToFood) {

                            Optional<Route> route = makeRoute(gameState, player, food.getPosition());
                            if (route.isPresent() && !route.isEmpty()) {
                                closestDistanceToFood = distanceToFood;
                                closestFood = food.getPosition();
                            }
                        }
                    }
                }
                
                if (closestFood != null) {
                    Optional<Route> route = makeRoute(gameState, player, closestFood);
                    playerRouteHashMap.put(player.getId(), route.get());
                    claimedFoodPositions.add(closestFood);
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
                            if (playerRouteHashMap.get(player1.getId())
                                    .equals(playerRouteHashMap.get(player2.getId()))) {
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

    private ArrayList<Position> routePositions(GameState gameState, Player player) {
        Position playerP = player.getPosition();
        ArrayList<Position> surroundingPositions = gameState.getMap().getSurroundingPositions(playerP, 9)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Position> possiblePositions = new ArrayList<>();
        for (Position p : surroundingPositions) {
            int distance = gameState.getMap().distance(playerP, p);
            // keep only the positions 9 spaces away
            if (distance < 9) {
                possiblePositions.add(p);
            }
        }
        return possiblePositions;
    }

    private Optional<Route> randomRoute(GameState gameState, Player player, ArrayList<Position> possiblePositions) {
        Optional<Route> newRoute = makeRoute(gameState, player, player.getPosition()); // initialise newRoute
        boolean invalidRoute = true;
        while (invalidRoute) { // search for a route that doesn't cross water
            Random random = new Random();
            int rIndex = (random.nextInt(possiblePositions.size()));
            newRoute = makeRoute(gameState, player, possiblePositions.get(rIndex));
            if (!newRoute.isEmpty()) {
                invalidRoute = false;
            }
        }
        return newRoute;
    }

    /* private Optional<Route> getSimilarRoute(GameState gameState, Player player, ArrayList<Position> possiblePositions) {
        Position oldPosition = player.getPosition();
        Optional<Direction> oldDirection = playerRouteHashMap.get(player.getId()).getFirstDirection();
        for (Position p : possiblePositions) {
            ArrayList<Direction> newDirections = gameState.getMap().directionsTowards(oldPosition, p)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (newDirections.contains(oldDirection)) {
                
            }
        }
        Optional<Route> newRoute = makeRoute(gameState, player, player.getPosition()); // initialise newRoute
        boolean invalidRoute = true;
        while (invalidRoute) { // search for a route that doesn't cross water
            Random random = new Random();
            int rIndex = (random.nextInt(possiblePositions.size()));
            newRoute = makeRoute(gameState, player, possiblePositions.get(rIndex));
            if (!newRoute.isEmpty() ) {
                invalidRoute = false;
            }
        }
        return newRoute;
    } */

    private Optional<Route> makeRoute(GameState gameState, Player player, Position futurePosition) {
        Set<Position> avoid = Collections.emptySet();
        Optional<Route> route = gameState.getMap().findRoute(player.getPosition(), futurePosition, avoid);
        avoid = gameState.getOutOfBoundsPositions();
        if (route.isPresent()) {
            boolean invalidRoute = route.get().collides(avoid);
            if (invalidRoute) {
                route.empty();
            }
        }
        return route;
    }

    private void fighting(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            if (isMyPlayer(player)) {
                //destroys enemy spawnpoint
                if (enemy != null) {
                    if (gameState.getMap().distance(enemy.getPosition(), player.getPosition()) < 10) {
                        Optional<Route> route = makeRoute(gameState, player, enemy.getPosition());
                        if (route.isPresent() && !route.isEmpty()) {
                            playerRouteHashMap.put(player.getId(), route.get());
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
                        
                        // TODO: make sure that both the player & the friend go towards the same enemy + case where there's more than one enemy
                        // check that they can both go towards the enemy (route)

                        if(alone) {
                            Optional<Direction> away = gameState.getMap()
                                .directionsAwayFrom(player.getPosition(), enemyP.getPosition()).findFirst();
                            playerDirectionHashMap.put(player.getId(), away.get());
                            //playerRouteHashMap.put(player.getId()), );
                        } else {
                            Optional<Direction> attackDirection = gameState.getMap().directionsTowards(player.getPosition(), enemyP.getPosition()).findFirst();
                            playerDirectionHashMap.put(player.getId(), attackDirection.get());
                            playerRouteHashMap.put(player.getId(), makeRoute(gameState, player, enemyP.getPosition()).get());
                        }
                    }
                }
            }
        }
    }
}

