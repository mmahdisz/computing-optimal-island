package main;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.graph.Graph;
import main.graph.Vertex;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Controller {

    public static final Double SQUARE_BOARD_SIZE = 800.0;
    public static final Double SQUARE_BOARD_X = 380.0;
    public static final Double SQUARE_BOARD_Y = 25.0;
    private final Double BUTTONS_Y_SAFE_DISTANCE = 50.0;

    private final Group root;
    private final Scene scene;
    private final Stage primarySatge;

    private final boolean isVerticesConfirmed = false;
    private final Graph graph;

    private Rectangle rectangle;
    private Button randomVerticesGeneratorBtn;
    private Button colorOptions;
    private Button runAlgorithmBtn;
    private Button clearBtn;
    private Button saveBtn;
    private Button infoBtn;

    public Controller(Stage primaryStage, Scene scene, Group root) {
        this.primarySatge = primaryStage;
        this.scene = scene;
        this.root = root;
        graph = new Graph(new ArrayList<>());
    }

    public void setScene() {
        setupBoard();
        setupMenu();
        setBoardClickable();
        setGenerateRandomVerticesButton();
        setColorOptionsButton();
        setClearButton();
        setSaveDataButton();
    }

    private void setupBoard() {
        rectangle = new Rectangle(SQUARE_BOARD_X, SQUARE_BOARD_Y, SQUARE_BOARD_SIZE, SQUARE_BOARD_SIZE);
        rectangle.setStroke(Color.DARKGREY);
        rectangle.setStrokeWidth(2);
        rectangle.setFill(Color.gray(0.98));
        rectangle.setArcHeight(5);
        rectangle.setArcWidth(5);
        root.getChildren().add(rectangle);
    }

    private void setupMenu() {
        randomVerticesGeneratorBtn = new Button("Generate Random Vertices");
        randomVerticesGeneratorBtn.setLayoutY(rectangle.getY() + 25);
        setupButtonView(randomVerticesGeneratorBtn);

        colorOptions = new Button("Color Options");
        colorOptions.setLayoutY(randomVerticesGeneratorBtn.getLayoutY() + BUTTONS_Y_SAFE_DISTANCE);
        setupButtonView(colorOptions);

        runAlgorithmBtn = new Button("Run Algorithm");
        runAlgorithmBtn.setLayoutY(colorOptions.getLayoutY() + BUTTONS_Y_SAFE_DISTANCE);
        setupButtonView(runAlgorithmBtn);

        clearBtn = new Button("Clear");
        clearBtn.setLayoutY(runAlgorithmBtn.getLayoutY() + 10.5 * BUTTONS_Y_SAFE_DISTANCE);
        setupButtonView(clearBtn);

        saveBtn = new Button("Save Data");
        saveBtn.setLayoutY(clearBtn.getLayoutY() + BUTTONS_Y_SAFE_DISTANCE);
        setupButtonView(saveBtn);

        infoBtn = new Button("Information & Documentation");
        infoBtn.setLayoutY(saveBtn.getLayoutY() + BUTTONS_Y_SAFE_DISTANCE);
        setupButtonView(infoBtn);

    }

    private void setBoardClickable() {
        scene.setOnMouseClicked(mouseEvent -> {
            if (!isVerticesConfirmed && isBoardClicked(mouseEvent) &&
                    !graph.isVertexCoordinationInvalid(mouseEvent.getX(), mouseEvent.getY())) {
                randomVerticesGeneratorBtn.setDisable(true);
                root.getChildren().add(graph.addVertexOnClick(mouseEvent).getCircle());
            }
        });
    }

    private void setGenerateRandomVerticesButton() {
        randomVerticesGeneratorBtn.setOnMouseClicked(event -> {
            randomVerticesGeneratorBtn.setDisable(true);
            colorOptions.setDisable(true);
            graph.randomVertexGenerator();
            for (Vertex v : graph.getVertexList())
                root.getChildren().add(v.getCircle());
        });
    }

    private void setColorOptionsButton() {
        colorOptions.setOnMouseClicked(mouseEvent -> Graph.VertexColorGenerator.colorOptionDialog());
    }

    private void setClearButton() {

    }

    private void setupButtonView(Button button) {
        button.setLayoutX(25);
        button.setMaxWidth(rectangle.getX() - 50);
        button.setMinWidth(rectangle.getX() - 50);
        button.setStyle("-fx-font: 16 sans;");
        root.getChildren().add(button);
    }

    private boolean isBoardClicked(MouseEvent mouseEvent) {
        return rectangle.contains(mouseEvent.getX() + 10, mouseEvent.getY() + 10)
                && rectangle.contains(mouseEvent.getX() - 10, mouseEvent.getY() - 10);
    }

    private void setSaveDataButton() {
        saveBtn.setOnMouseClicked(event -> {
            graph.sortY();
            String result = "Vertices: " + graph.getVertexList().toString();

            FileChooser fileChooser = new FileChooser();

            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);

            File file = fileChooser.showSaveDialog(primarySatge);

            if (file != null) {
                saveTextToFile(result, file);
            }
        });
    }

    private void saveTextToFile(String content, File file) {
        try {
            PrintWriter writer;
            writer = new PrintWriter(file);
            writer.println(content);
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
