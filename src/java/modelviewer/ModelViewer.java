package modelviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import modelviewer.controllers.ModelController;

import java.util.Objects;

public class ModelViewer extends Application {

    public static ModelController modelController;

    public Scene scene;

    public void setModelController(ModelController controller) {
        modelController = controller;
    }

    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/modelviewerui.fxml"));
        scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/darktheme.css")).toExternalForm());
        primaryStage.setScene(scene);
    }
}
