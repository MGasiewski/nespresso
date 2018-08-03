package nespresso.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class NtscScreen extends Application implements Screen {

	private Canvas canvas;
	private Pane root;
	private Scene scene;
	
	@Override
	public void drawPixel(int x, int y, Color c) {
		canvas.getGraphicsContext2D().getPixelWriter().setColor(x, y, c);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		root = new Pane();
		canvas = new Canvas(256, 224);
		root.getChildren().add(canvas);
		scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Nespresso");
		primaryStage.show();
	}

}
