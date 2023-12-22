package fr.univlille.views;

import fr.univlille.Theme;
import fr.univlille.controllers.GameController;
import fr.univlille.Coordinate;
import fr.univlille.GameMode;
import fr.univlille.GameParameters;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent;
import fr.univlille.iutinfo.cam.player.perception.ICoordinate;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent.CellInfo;
import fr.univlille.models.GameModel;
import fr.univlille.utils.Observer;
import fr.univlille.utils.Subject;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class GameView extends Canvas implements Observer {
    /**
     * A reference to the instance of "Game" containing the hunter and monster
     * models, as well as the maze itself.
     */
    public final GameModel model;

    /**
     * The context that allows us to draw stuff into.
     */
    public final GraphicsContext gc;

    /**
     * A tile is one image from the tileset (spritesheet).
     * Each tile is 32 pixels wide.
     */
    public static final int TILE_SIZE = 32;

    private boolean hunterTurn;

    public boolean isHunterTurn() {
        return hunterTurn;
    }

    public void setHunterTurn(boolean hunterTurn) {
        this.hunterTurn = hunterTurn;
    }

    private Coordinate cursorPosition;
    private Coordinate movePosition;

    private GameController mainPage;

    public GameController getMainPage() {
        return mainPage;
    }

    public void setMainPage(GameController mainPage) {
        this.mainPage = mainPage;
    }

    private HunterView hunterView;
    public HunterView getHunterView() {
        return hunterView;
    }

    private MonsterView monsterView;

    public MonsterView getMonsterView() {
        return monsterView;
    }

    /**
     * Each image in the game is contained in a spritesheet.
     * A spritesheet is a set of fixed-size images, and each image is a
     * "decoration".
     * Each decoration has a unique index, just like an array.
     */
    public static Image spritesheet = new Image(GameView.class.getResourceAsStream("/images/spritesheet.png"));

    private GameParameters parameters;


    public GameView(GameModel model, GameParameters parameters) {
        this.model = model;
        this.parameters = parameters;

        this.gc = getGraphicsContext2D();

        hunterView = new HunterView(gc, this, model);
        monsterView = new MonsterView(gc, this, model, parameters);

        ICoordinate mazeDimensions = model.getMazeDimensions();
        setWidth((double) TILE_SIZE * mazeDimensions.getCol());
        setHeight((double) TILE_SIZE * mazeDimensions.getRow());

        cursorPosition = new Coordinate(0, 0);
        movePosition = new Coordinate(-1, -1);
        setOnMouseMoved(e -> {
            if (model.isGameEnded() || (hunterTurn && model.getHunter().getShootsLeft() <= 0
                    && model.getHunter().getGrenadesLeft() <= 0)) {
                return;
            }
            Coordinate relativeMousePosition = new Coordinate(
                    (int) (e.getSceneX() - getLayoutX() - (TILE_SIZE * 0.5)),
                    (int) (e.getSceneY() - getLayoutY() - (TILE_SIZE * 0.5)));
            cursorPosition = new Coordinate(
                    (double) relativeMousePosition.getCol() / TILE_SIZE,
                    (double) relativeMousePosition.getRow() / TILE_SIZE);
            draw();

        });

        setOnMousePressed(e -> {
            if (hunterTurn) {
                handleMousePressedHunter();
            } else {
                handleMousePressedMonster();
            }
            draw();
            mainPage.updateEntitiesLabel();
        });

        // on attache la vue au hunter
        model.getHunter().attach(this);
    }

    public void handleMousePressedHunter() {
        if (model.isGameEnded() || (model.getHunter().getShootsLeft() <= 0 && model.getHunter().getGrenadesLeft() <= 0)) {
            return;
        }
        if (model.getHunter().isGrenadeMode()) {
            playGrenade();
        }
        if (model.getHunter().isHunterShootValid(cursorPosition)) {
            play();
        }
    }

    public void handleMousePressedMonster() {
        if (model.getMonster().superJump && model.getMonster().superJumpLeft > 0) {
            if (model.getMonster().isMonsterMovementValid(cursorPosition, 2.0)) {
                movePosition = cursorPosition;
            }
        } else if (model.getMonster().isMonsterMovementValid(cursorPosition, 1.0)) {
            movePosition = cursorPosition;
        }

        // Si le joueur joue contre un robot, alors on termine immédiatement le tour (pour éviter qu'il ait a déplacer sa souris le bouton en bas)
        if (parameters.getGameMode() == GameMode.BOT && !parameters.isAiPlayerIsHunter()) {
            mainPage.playTurn();
        }
    }

    public ICoordinate getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(ICoordinate cursorPosition) {
        this.cursorPosition = (Coordinate) cursorPosition;
    }

    public ICoordinate getMovePosition() {
        return movePosition;
    }

    public void setMovePosition(ICoordinate movePosition) {
        this.movePosition = (Coordinate) movePosition;
    }

    /**
     * Cette fonction affiche sur le Canvas les informations nécessaires. Elle est
     * appellée à chaque mouvement de souris ou à chaque action.
     */
    public void draw() {
        if (hunterTurn) {
            hunterView.draw();
        } else {
            monsterView.draw();
        }
    }

    public boolean playHunterMove() {
        if (model.getHunter().getShootsLeft() <= 0) {
            return false;
        }
        model.getHunter().shoot(cursorPosition);
        model.getHunter().setShootsLeft(model.getHunter().getShootsLeft() - 1);
        return true;
    }

    public boolean play() {
        boolean isValid = false;
        if (isHunterTurn()) {
            isValid = playHunterMove();
        } else {
            isValid = model.getMonster().play(movePosition);
        }
        if (isValid) {
            cursorPosition = new Coordinate(-1, -1);
            movePosition = new Coordinate(-1, -1);
        }
        return isValid;
    }

    public boolean playHunterGrenade() {
        if (model.getHunter().getGrenadesLeft() <= 0) {
            return false;
        }
        model.getHunter().grenade(cursorPosition);
        model.getHunter().setGrenadesLeft(model.getHunter().getGrenadesLeft() - 1);
        return true;
    }

    public boolean playGrenade() {
        boolean isValid = false;
        if (hunterTurn) {
            isValid = playHunterGrenade();
        } else {
            isValid = model.getMonster().play(movePosition);
        }
        if (isValid) {
            cursorPosition = new Coordinate(-1, -1);
            movePosition = new Coordinate(-1, -1);
        }
        return isValid;
    }

    /**
     * Sets the selected theme and re-draws the UI accordingly.
     * If the theme isn't valid, nothing happens.
     * 
     * @param theme The theme to be applied to the game.
     */
    public void setTheme(Theme theme) {
        switch (theme) {
            case DEFAULT:
                spritesheet = new Image(getClass().getResourceAsStream("/images/spritesheet.png"));
                break;
            case HALLOWEEN:
                spritesheet = new Image(getClass().getResourceAsStream("/images/spritesheet_halloween.png"));
                break;
            default:
                return;
        }
        draw();
    }

    @Override
    public void update(Subject subj) {
        ICellEvent cellEvent = (ICellEvent) subj;
        if (cellEvent.getState() == CellInfo.WALL) {
            mainPage.errorLabel.setText("Vous avez touché un arbre.");
        } else if (cellEvent.getState() == CellInfo.MONSTER) {
            monsterCase(cellEvent);
        } else {
            mainPage.errorLabel.setText("Vous n'avez rien touché...");
        }
        mainPage.updateEntitiesLabel();
        draw();
    }

    @Override
    public void update(Subject subj, Object data) {
        ICellEvent cellEvent = (ICellEvent) data;
        if (cellEvent.getState() == CellInfo.WALL) {
            mainPage.errorLabel.setText("Vous avez touché un arbre.");
        } else if (cellEvent.getState() == CellInfo.MONSTER) {
            monsterCase(cellEvent);
        } else {
            mainPage.errorLabel.setText("Vous n'avez rien touché...");
        }
        mainPage.updateEntitiesLabel();
        draw();
    }

    private void monsterCase(ICellEvent cellEvent) {
        if (cellEvent.getTurn() == model.getTurn()) { // Si le monstre est actuellement sur cette case
            model.setGameEnded(true);
        } else {
            mainPage.errorLabel
                    .setText("Le monstre est passé ici au tour n° " + cellEvent.getTurn() + ".");
        }
    }
}
