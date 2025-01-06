package main.java;

import javafx.application.Application;
import javafx.stage.Stage;

public class VoirViaSons extends Application {

    @Override
    public void start(Stage primaryStage) {
        InterfaceController controller = new InterfaceController();
        primaryStage.setTitle("Voir Via les Sons");
        primaryStage.setScene(controller.createInterface(primaryStage));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
