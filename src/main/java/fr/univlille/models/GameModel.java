package fr.univlille.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fr.univlille.Coordinate;
import fr.univlille.GameParameters;
import fr.univlille.Maze;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent;
import fr.univlille.iutinfo.cam.player.perception.ICoordinate;
import fr.univlille.utils.Subject;

public class GameModel extends Subject {

    /**
     * The current turn of the game.
     * Initialized at 1 when creating the maze.
     */
    private int turn;
    /**
     * 1 represents a wall,
     * 0 represents an empty cell.
     * There is no need for other types of cells as they're contained in other
     * variables or in "history".
     */
    private boolean[][] maze;

    /**
     * The coordinates of the exit.
     */
    private ICoordinate exit;

    private HunterModel hunter;
    private MonsterModel monster;

    private GameParameters parameters;

    public GameParameters getParameters() {
        return parameters;
    }

    public boolean[][] getMaze() {
        return maze;
    }

    public void setParameters(GameParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * A list containing all the moves of the hunter and the monster.
     * As it stores instances of `ICellEvent` it remembers at which turn one
     * particular move was done,
     * so it's thanks to this variable that we can know at which turn the monster
     * was on a particular cell.
     */
    private ArrayList<ICellEvent> history = new ArrayList<>();

    /**
     * A boolean that stores whether or not the game has finished.
     */
    private boolean gameEnded;

    public int getHeight() {
        return maze.length;
    }

    public int getWidth() {
        return maze[0].length;
    }

    public HunterModel getHunter() {
        return hunter;
    }

    public MonsterModel getMonster() {
        return monster;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    public int getTurn() {
        return turn;
    }

    public void incrementTurn() {
        this.turn += 1;
    }

    /**
     * Checks if a particular cell is a wall or empty.
     * 
     * @param x The X coordinate of the given cell.
     * @param y The Y coordinate of the given cell.
     * @return `true` if this cell is a wall, `false` if it's empty.
     */
    public boolean isWallAt(int x, int y) {
        if(x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) { // si en dehors du labyrinthe
            return true;
        }
        return maze[y][x];
    }

    /**
     * Checks if a particular cell is a wall or empty.
     * 
     * @param coordinate The coordinates of the given cell.
     * @return `true` if this cell is a wall, `false` if it's empty.
     */
    public boolean isWallAt(ICoordinate coordinate) {
        return isWallAt(coordinate.getRow(), coordinate.getCol());
    }

    /**
     * Checks if the position of the monster matches the position of the exit.
     * 
     * @return `true` if the monster has reached the exit, `false` otherwise.
     */
    public boolean monsterWon() {
        return monster.getPosition().equals(exit);
    }

    /**
     * Gets the width and height of the maze as an instance of `Coordinate`.
     * 
     * @return An instance of `Coordinate` where `x` is the width of the maze and
     *         `y` the height.
     */
    public ICoordinate getMazeDimensions() {
        return new Coordinate(getWidth(), getHeight());
    }

    /**
     * Gets the position of the exit.
     * 
     * @return The exact coordinates of the exit.
     */
    public ICoordinate getExit() {
        return exit;
    }

    /**
     * Gets a random position within the maze.
     * For now, it gives a random position that is not a wall.
     * 
     * @return A random position in the maze.
     */
    public ICoordinate randomPosition() {
        ArrayList<Coordinate> availableCoordinates = new ArrayList<>();
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (!isWallAt(x, y)) {
                    availableCoordinates.add(new Coordinate(x, y));
                }
            }
        }

        Random random = new Random();
        return availableCoordinates.get(random.nextInt(availableCoordinates.size()));
    }

    /**
     * Generates the maze.
     * It initializes the hunter and monster models.
     * It gives a random position to the monster.
     * The turns start at 1 and the history is cleared.
     * The exit is also randomized.
     */
    public void generateMaze(GameParameters parameters) {
        this.parameters = parameters;
        Maze laby = new Maze(parameters.getMazeWidth(), parameters.getMazeHeight());
        maze = laby.createMaze(parameters.getWallsPercentage());

        this.hunter = new HunterModel(this);
        this.monster = new MonsterModel(this, randomPosition());

        this.turn = 1;
        this.history.clear();

        exit = randomPosition();
        while (exit.equals(getMonster().getPosition())) {
            exit = randomPosition();
        }
    }

    public List<ICellEvent> getHistory() {
        return history;
    }

    public void addToHistory(ICellEvent cellEvent) {
        history.add(cellEvent);
    }

}