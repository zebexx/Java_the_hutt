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
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

//TODO: 
//Player usernames? xX_Noob_bot_Pwner69_Xx 
//Teleportation????? 
//Cheatcodes???? 
//was there a rule about not creating more class files? 

// if you get a new direction because the old one is leading you to the water, then stick to it.
// if the diagonal directions are all going towards water, then go for non-diagonal directions?

//improve exploration 

//fighting 
// - run away in bad fight 
// - attack enemy spawn point if no enemy players are on it 
// - if home is destroyed 
//      - stop collecting food 
//      - group up and hunt enemies/enemy spawn point
// - defend spawnpoint?
// - blocking enemies from areas? eg blocking gap between water (depends on map) 

public class ExampleBot extends Bot {

    private HashMap<Id, Direction> playerDirectionHashMap;
    private HashMap<Id, ArrayList<Player>> teamHashMap;
    private GameStateLogger.GameStateLoggerBuilder gameStateLoggerBuilder;
    private List<Position> nextPositions;
    private SpawnPoint home;
    private SpawnPoint enemy;
    private int counter = 4;

    public ExampleBot(String name) {
        super(name);
        gameStateLoggerBuilder = GameStateLogger.configure(getId());
    }

    private void moveRandomly(GameState gameState) {
        
        ArrayList<Direction> diagonalDirections = new ArrayList<>();
        diagonalDirections.add(Direction.NORTHEAST);
        diagonalDirections.add(Direction.SOUTHEAST);
        diagonalDirections.add(Direction.SOUTHWEST);
        diagonalDirections.add(Direction.NORTHWEST);
        for (Player player : gameState.getPlayers()) {
            Id playerID = player.getId();
            boolean needNewDirection = player.getPosition().equals(home.getPosition())
                    || !playerDirectionHashMap.containsKey(player.getId());
            if (isMyPlayer(player) && needNewDirection) {
                System.out.println("Counter: " + counter);
                int index = counter % 4;
                ++counter;
                System.out.println("Index: " + index);
                Direction newDirection = diagonalDirections.get(index);
                playerDirectionHashMap.put(player.getId(), newDirection);
            } else if (isMyPlayer(player) && !needNewDirection) {
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
        gameStateLoggerBuilder.process(gameState);
        moveRandomly(gameState);
        //avoidPlayers(gameState);
        fighting(gameState);
        collectFood(gameState);
        attackEnemySpawnPoint(gameState);
        List<Move> moves = extractMoves(gameState);
        return moves;
    }

    @Override
    public void initialise(GameState gameState) {
        playerDirectionHashMap = new HashMap<>();
        teamHashMap = new HashMap<>();
    
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
                Direction newDirection = direction; // initialise newDirection
                for (Direction d : allDirections) {
                    if (canMove(gameState, player, d)) {
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

                    if (distanceToFood < 10 && !(claimedFoodPositions.contains(food.getPosition()))) {
                        if (closestDistanceToFood > distanceToFood && checkRoute(gameState, player, food.getPosition())) {
                            closestDistanceToFood = distanceToFood;
                            closestFood = food.getPosition();
                        }
                    }
                }

                if (!(closestFood == null)) {
                    Optional<Direction> direction = gameState.getMap()
                            .directionsTowards(player.getPosition(), closestFood).findFirst();

                    if (direction.isPresent()) {
                        playerDirectionHashMap.put(player.getId(), direction.get());
                    }
                }
            }
        }
    }
    
    private boolean checkRoute(GameState gameState, Player player, Position futurePosition) {
        Set<Position> avoid = Collections.emptySet();
        Optional<Route> route = gameState.getMap().findRoute(player.getPosition(), futurePosition, avoid);
        avoid = gameState.getOutOfBoundsPositions();
        if (route.isPresent()) {
            boolean invalidRoute = route.get().collides(avoid);
            return (!invalidRoute);
        } else {
            return false;
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
    
    private void attackEnemySpawnPoint(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            if (isMyPlayer(player) && enemy != null) {
                if (gameState.getMap().distance(enemy.getPosition(), player.getPosition()) < 10) {
                    Optional<Direction> direction = gameState.getMap()
                            .directionsTowards(player.getPosition(), enemy.getPosition()).findFirst();
                    if (direction.isPresent()) {
                        playerDirectionHashMap.put(player.getId(), direction.get());
                    }
                }
            }
        }
    }

    private void fighting(GameState gameState) {
        for (Player player : gameState.getPlayers()) {
            boolean isInTeam = false;
            for(Entry<Id, ArrayList<Player>> team : teamHashMap.entrySet()) {
                Id targetId = team.getKey();
                ArrayList<Player> teamPlayers = team.getValue();
                if (teamPlayers.contains(player)) {
                    isInTeam = true;
                }
            }
            if (isMyPlayer(player) && !isInTeam) {

                for (Player enemyP : gameState.getPlayers()) {
                    // get the closest enemy player?
                    if (!isMyPlayer(enemyP)
                            && gameState.getMap().distance(enemyP.getPosition(), player.getPosition()) < 10
                            && checkRoute(gameState, player, enemyP.getPosition())) {
                        boolean alone = true;
                        int distanceToEnemy = gameState.getMap().distance(enemyP.getPosition(), player.getPosition());

                        for (Player friend : gameState.getPlayers()) {
                            if (isMyPlayer(friend) 
                                    && gameState.getMap().distance(enemyP.getPosition(),
                                            friend.getPosition()) == distanceToEnemy
                                    && !teamHashMap.containsKey(enemyP.getId())) { //there isn't already a team targeting that enemy
                                alone = false;
                                ArrayList<Player> newTeam = new ArrayList<>();
                                newTeam.add(player);
                                newTeam.add(friend);
                                teamHashMap.put(enemyP.getId(), newTeam);
                            } else if (isMyPlayer(friend) 
                                    && gameState.getMap().distance(enemyP.getPosition(),
                                            friend.getPosition()) == distanceToEnemy
                                    && teamHashMap.containsKey(enemyP.getId())) {
                                alone = false;
                                ArrayList<Player> existingTeam = teamHashMap.get(enemyP.getId());
                                existingTeam.add(friend);
                                existingTeam.add(player);
                            }
                        }
                        
                        // Check to see how many enemies are close together
                        // case where there's more than one enemy

                        if (alone) {
                            Optional<Direction> away = gameState.getMap()
                                    .directionsAwayFrom(player.getPosition(), enemyP.getPosition()).findFirst();
                            playerDirectionHashMap.put(player.getId(), away.get());
                        } 
                            
                        
                    }
                }
            }
        }
        for (Entry<Id, ArrayList<Player>> team : teamHashMap.entrySet()) {
            Id targetId = team.getKey();
            ArrayList<Player> teamPlayers = team.getValue();
            for (Player Tplayer : teamPlayers) {
                Player enemyP = findPlayerByID(gameState, targetId);
                Position enemyPos = enemyP.getPosition();
                Position playerPos = Tplayer.getPosition();
                Optional<Direction> directionEnemy = gameState.getMap().directionsTowards(playerPos, enemyPos).findFirst(); 
                playerDirectionHashMap.put(Tplayer.getId(), directionEnemy.get());
            }
        }
    }
}
