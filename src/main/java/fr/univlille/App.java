package fr.univlille;

import java.io.IOException;

import fr.univlille.multiplayer.Client;
import fr.univlille.multiplayer.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final int DEFAULT_MULTIPLAYER_PORT = 6666;
    public GameParameters parameters;
    private static Scene scene;
    private static App app;

    public static int getDefaultMultiplayerPort() {
        return DEFAULT_MULTIPLAYER_PORT;
    }

    public static App getApp() {
        if (app == null) {
            app = new App();
        }
        return app;
    }

    private static Parent loadFXML(String filename) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("controllers/" + filename + ".fxml"));

        if (fxmlLoader.getLocation() == null) {
            System.err.println("Le chemin du fichier FXML est invalide!");
            System.exit(1);
        }

        return fxmlLoader.load();
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent parent = loadFXML("menu");
        App.scene = new Scene(parent, 1000, 1000);
        stage.setScene(scene);
        stage.setTitle("Chasse au monstre");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // making sure the server and the client are properly closed when exiting the app
        Client.getInstance().kill(false);
        Server.getInstance().kill(false);
    }

    public void showParameters(GameMode gameMode) throws IOException {
        parameters = new GameParameters();
        parameters.setGameMode(gameMode);
        changeScene("settings");
    }

    public void changeScene(String name) throws IOException {
        scene.setRoot(loadFXML(name));
    }

    public void startGame(GameParameters parameters) throws IOException {
        this.parameters = parameters;
        scene.setRoot(loadFXML("game"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}