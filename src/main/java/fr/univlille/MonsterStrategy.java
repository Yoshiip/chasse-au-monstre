package fr.univlille;

import java.util.ArrayList;
import java.util.Random;

import fr.univlille.iutinfo.cam.player.monster.IMonsterStrategy;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent;
import fr.univlille.iutinfo.cam.player.perception.ICoordinate;

public class MonsterStrategy implements IMonsterStrategy {
 
    private boolean[][] maze;

    private int mazeWidth;
    private int mazeHeight;

    private ICoordinate monsterPosition;

    @Override
    public ICoordinate play() {
        ArrayList<ICoordinate> directions = new ArrayList<>();

        Random random = new Random();
        if(!isWallAt(monsterPosition.getCol() + 1, monsterPosition.getRow())) directions.add(new Coordinate(1, 0));
        if(!isWallAt(monsterPosition.getCol() - 1, monsterPosition.getRow())) directions.add(new Coordinate(-1, 0));
        if(!isWallAt(monsterPosition.getCol(), monsterPosition.getRow() + 1)) directions.add(new Coordinate(0, 1));
        if(!isWallAt(monsterPosition.getCol(), monsterPosition.getRow() - 1)) directions.add(new Coordinate(0, -1));
        
        
        ICoordinate direction = directions.get(random.nextInt(directions.size()));

        return new Coordinate(
            monsterPosition.getCol() + direction.getCol(),
            monsterPosition.getRow() + direction.getRow()
        );
    }

    public boolean isWallAt(int x, int y) {
        if(x < 0 || x >= mazeWidth || y < 0 || y >= mazeHeight) { // si en dehors du labyrinthe
            return true;
        }
        return maze[y][x];
    }


    @Override
    public void initialize(boolean[][] arg0) {
        this.maze = arg0;
        this.mazeWidth = arg0[0].length;
        this.mazeHeight = arg0.length;
    }

    @Override
    public void update(ICellEvent arg0) {
        monsterPosition = arg0.getCoord();
    }

}