package fr.univlille.models;

import java.util.ArrayList;
import java.util.List;

import fr.univlille.CellEvent;
import fr.univlille.Coordinate;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent;
import fr.univlille.iutinfo.cam.player.perception.ICoordinate;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent.CellInfo;
import fr.univlille.utils.Subject;
import javafx.scene.control.skin.CheckBoxSkin;

public class HunterModel extends Subject {
    private ArrayList<ICellEvent> shootsHistory;

    private GameModel gameModel;

    private int maxShoots;

    private int shootsLeft;
    private int grenadesLeft;

    private boolean grenadeMode;

    public boolean isGrenadeMode() {
        return grenadeMode;
    }

    public void setGrenadeMode(boolean grenadeMode) {
        this.grenadeMode = grenadeMode;
    }

    public int getShootsLeft() {
        return shootsLeft;
    }

    public void setShootsLeft(int shootsLeft) {
        this.shootsLeft = shootsLeft;
    }

    public int getGrenadesLeft() {
        return grenadesLeft;
    }

    public void setGrenadesLeft(int grenadesLeft) {
        this.grenadesLeft = grenadesLeft;
    }

    public List<ICellEvent> getShootsHistory() {
        return shootsHistory;
    }

    public int getMaxShoots() {
        return maxShoots;
    }

    public HunterModel(GameModel gameModel) {
        this.gameModel = gameModel;
        this.maxShoots = gameModel.getParameters().getHunterShoots();
        this.shootsLeft = this.maxShoots;
        this.grenadesLeft = gameModel.getParameters().getHunterGrenades();
        shootsHistory = new ArrayList<>();
    }

    public void turnBegin() {
        this.shootsLeft = maxShoots;
    }

    /**
     * Makes sure that the given hunter's target is valid.
     * The hunter cannot shoot outside of the maze and cannot shoot the borders.
     * 
     * @param shoot The target's position.
     * @return `true` if the target's position is valid, `false` otherwise.
     */
    public boolean isHunterShootValid(ICoordinate shoot) {
        ICoordinate mazeDimensions = gameModel.getMazeDimensions();
        return shoot.getCol() >= 0 && shoot.getCol() < mazeDimensions.getCol()
            && shoot.getRow() >= 0 && shoot.getRow() < mazeDimensions.getRow();
    }

    /**
     * Gets information about the cell that the hunter is targeting.
     * 
     * @param shootPosition The coordinates of the hunter's target.
     * @return The type of cell that the hunter has shot.
     */
    public void shoot(ICoordinate shootPosition) {
        CellInfo state = CellInfo.EMPTY;
        if (gameModel.isWallAt(shootPosition)) {
            state = CellInfo.WALL;
        }

        // remove other shoots history with the same position
        for (int i = shootsHistory.size() - 1; i >= 0; i--) {
            if (shootsHistory.get(i).getCoord().equals(shootPosition)) {
                shootsHistory.remove(i);
            }
        }
        
        // l'historique de déplacement du monstre
        for (int i = gameModel.getHistory().size() - 1; i >= 0; i--) {
            ICellEvent cellEvent = gameModel.getHistory().get(i);
            if (cellEvent.getCoord().equals(shootPosition)) {
                shootsHistory.add(cellEvent);
                notifyObservers(cellEvent);
                return;
            }
        }
        CellEvent cellEvent = new CellEvent(shootPosition, state, gameModel.getTurn());

        shootsHistory.add(cellEvent);
        notifyObservers(cellEvent);
    }

    public void grenade(Coordinate greandePosition) {
        shoot(greandePosition);
        if (isHunterShootValid(new Coordinate(greandePosition.getCol() + 1, greandePosition.getRow()))) {
            shoot(new Coordinate(greandePosition.getCol() + 1, greandePosition.getRow()));
        }
        if (isHunterShootValid(new Coordinate(greandePosition.getCol() - 1, greandePosition.getRow()))) {
            shoot(new Coordinate(greandePosition.getCol() - 1, greandePosition.getRow()));
        }
        if (isHunterShootValid(new Coordinate(greandePosition.getCol(), greandePosition.getRow() + 1))) {
            shoot(new Coordinate(greandePosition.getCol(), greandePosition.getRow() + 1));
        }
        if (isHunterShootValid(new Coordinate(greandePosition.getCol(), greandePosition.getRow() - 1))) {
            shoot(new Coordinate(greandePosition.getCol(), greandePosition.getRow() - 1));
        }
    }
}
