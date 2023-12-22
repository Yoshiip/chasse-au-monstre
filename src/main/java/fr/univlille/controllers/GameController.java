package fr.univlille.controllers;

import java.io.IOException;

import fr.univlille.App;
import fr.univlille.CellEvent;
import fr.univlille.Coordinate;
import fr.univlille.GameMode;
import fr.univlille.GameParameters;
import fr.univlille.HunterStrategy;
import fr.univlille.MonsterStrategy;
import fr.univlille.iutinfo.cam.player.perception.ICellEvent.CellInfo;
import fr.univlille.models.GameModel;
import fr.univlille.views.GameView;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class GameController {

    @FXML
    public ToggleButton powerupButton;

    @FXML
    public Label turnLabel;

    @FXML
    public Button endTurnButton;

    @FXML
    public Label powerupEnabledLabel;

    @FXML
    public Label shootLeftLabel;

    @FXML
    public VBox mainVBox;

    @FXML
    public AnchorPane switchPane;

    @FXML
    public Label switchPaneCountdown;

    @FXML
    public Label errorLabel;

    @FXML
    public AnchorPane gameOverScreen;

    @FXML
    public Label winnerLabel;

    private GameView gameView;
    private GameModel game;
    private GameParameters parameters;

    private MonsterStrategy monsterStrategy;
    private HunterStrategy hunterStrategy;

    /**
     * Cette méthode permet d'initialiser la partie. Elle est appellé à chaque
     * rédemarrage du jeu.
     */
    public void initGame() {
        gameOverScreen.setVisible(false);

        game = new GameModel();
        this.parameters = App.getApp().parameters;
        game.generateMaze(parameters);

        if (gameView != null) {
            mainVBox.getChildren().remove(gameView);
        }
        gameView = new GameView(game, parameters);

        mainVBox.getChildren().add(2, gameView);
        gameView.draw();
        gameView.setMainPage(this);
        updateEntitiesLabel();

        // On ajoute la première position du monstre dans l'historique
        Coordinate monsterPosition = game.getMonster().getPosition();

        // Cela peut paraître bizarre de récreer une coordonnée avec les mêmes
        // coordonnées,
        // mais c'est simplement car sinon les deux instances seront liés et cette
        // position
        // sera dans l'historique sera modifiée à chaque nouveau déplacement du monstre
        // (ce qu'on ne veut pas!)
        game.addToHistory(new CellEvent(new Coordinate(monsterPosition.getCol(), monsterPosition.getRow()),
                CellInfo.MONSTER, game.getTurn()));

        if (parameters.getGameMode() == GameMode.BOT) {
            // On crée la MonsterStrategy ou la HunterStrategy en fonction du rôle que le
            // joueur a pris.
            if (parameters.isAiPlayerIsHunter()) {
                monsterStrategy = new MonsterStrategy();
                monsterStrategy.initialize(game.getMaze());
                playTurn(); // Comme c'est toujours le monstre qui commence, on le laisse d'abord jouer.
            } else {
                hunterStrategy = new HunterStrategy();
                hunterStrategy.initialize(parameters.getMazeWidth(), parameters.getMazeHeight());
            }
        }
    }

    @FXML
    public void initialize() {
        initGame();
    }

    /**
     * Cette méthode permet de créer un Thread qui attends automatiquement le nombre
     * de millisecondes données en paramètre, puis éxecute le code de l'argument
     * continuation.
     * 
     * @param millis       Le nombre de millisecondes à attendre
     * @param continuation Le code à éxecuter à la fin du delay.
     */
    public static void delay(long millis, Runnable continuation) {
        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(millis);
                return null;
            }
        };
        sleeper.setOnSucceeded(event -> continuation.run());
        new Thread(sleeper).start();
    }

    @FXML
    public void playButtonPressed() {
        playTurn();
    }

    private boolean isBotTurn() {
        return parameters.getGameMode() == GameMode.BOT
                && (gameView.isHunterTurn() && !parameters.isAiPlayerIsHunter()
                        || !gameView.isHunterTurn() && parameters.isAiPlayerIsHunter());
    }

    public void playTurn() {
        if (isBotTurn()) {
            if (parameters.isAiPlayerIsHunter()) {
                monsterStrategy
                        .update(new CellEvent(game.getMonster().getPosition(), CellInfo.MONSTER, game.getTurn()));
                gameView.setMovePosition(monsterStrategy.play()); // on fait jouer le monstre
            } else {
                while (game.getHunter().getShootsLeft() > 0) {
                    gameView.setCursorPosition(hunterStrategy.play()); // on fait jouer le chasseur
                    gameView.play();
                }
            }
        }
        if (gameView.isHunterTurn() || gameView.play()) {
            errorLabel.setText("");
            updateEntitiesLabel();
        } else {
            errorLabel.setText("Mouvement invalide!");
        }

        if (game.monsterWon()) {
            game.setGameEnded(true);
        }

        if (game.isGameEnded()) {
            if (game.monsterWon()) {
                winnerLabel.setText("Le monstre a gagné!");
            } else {
                winnerLabel.setText("Le chasseur a gagné!");
            }
            gameOverScreen.setVisible(true);
        }
        swapScreen();
    }

    @FXML
    public void powerupButtonPressed() {
        if (gameView.isHunterTurn()) {
            if (game.getHunter().getGrenadesLeft() > 0) {
                boolean grenadeMode = game.getHunter().isGrenadeMode();
                powerupEnabledLabel.setVisible(grenadeMode);
                game.getHunter().setGrenadeMode(!grenadeMode);
            } else {
                errorLabel.setText("Vous n'avez plu de grenade...");
            }
        } else {
            if (game.getMonster().superJumpLeft > 0) {
                if (game.getMonster().superJump) {
                    powerupEnabledLabel.setVisible(false);
                    game.getMonster().superJump = false;
                } else {
                    powerupEnabledLabel.setVisible(true);
                    game.getMonster().superJump = true;
                }
            } else {
                errorLabel.setText("Vous n'avez plu de SuperJump...");
            }
        }
    }

    private void swapScreen() {
        if (parameters.getGameMode() == GameMode.TWO_PLAYERS) {
            // Animation de l'écran
            switchPane.setVisible(true);
            switchPaneCountdown.setText("Dans 3...");
            delay(1000, () -> switchPaneCountdown.setText("Dans 2.."));
            delay(2000, () -> switchPaneCountdown.setText("Dans 1."));
            delay(3000, () -> switchPane.setVisible(false));
        }

        // On échange les tours
        gameView.setHunterTurn(!gameView.isHunterTurn());
        if (gameView.isHunterTurn()) {
            game.getHunter().turnBegin();
            game.getHunter().setGrenadeMode(false);
        } else {
            game.getMonster().superJump = false;
            gameView.getMonsterView().turnStarted();
        }

        updateEntitiesLabel();
        gameView.draw();

        if (isBotTurn()) {
            playTurn();
        }
    }

    @FXML
    public void restartGamePressed() {
        System.out.println("restart");
        initGame();
    }

    @FXML
    public void menuButtonPressed() throws IOException {
        App.getApp().changeScene("menu");
    }

    public void updateEntitiesLabel() {
        turnLabel.setText("Tour n°" + game.getTurn());
        if (gameView.isHunterTurn()) {
            powerupButton.setDisable(game.getHunter().getGrenadesLeft() <= 0);
            boolean grenade = game.getHunter().isGrenadeMode();
            if (game.getHunter().getShootsLeft() == 1) {
                shootLeftLabel.setText("Il vous reste " + game.getHunter().getShootsLeft() + " tir!");
            } else {
                shootLeftLabel.setText("Il vous reste " + game.getHunter().getShootsLeft() + " tirs!");
            }
            
            powerupButton.setText("Grenade (" + game.getHunter().getGrenadesLeft() + ")");
            powerupEnabledLabel.setVisible(grenade);
            
            if(!grenade) {
                powerupButton.setSelected(false);
            }
        } else {
            boolean superjump = game.getMonster().superJump;
            powerupButton.setDisable(game.getMonster().superJumpLeft <= 0);
            
            powerupButton.setText("SuperJump (" + game.getMonster().superJumpLeft + ")");
            powerupEnabledLabel.setVisible(superjump);
            if(!superjump) {
                powerupButton.setSelected(false);
            }
        }
        
        shootLeftLabel.setVisible(gameView.isHunterTurn());
    }
}
