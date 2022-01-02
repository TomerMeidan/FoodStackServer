package util;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Class to avoid repeating code, contains GridPane and col, row also contains a
 * method for adding an Image to grid
 * @author mosa
 *
 */
public class MyGrid {
	private GridPane grid;
	private ToggleGroup group;
	private int col;
	private int row;

	public MyGrid() {
		grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setAlignment(Pos.CENTER);
		col = 0;
		row = 0;
		group = new ToggleGroup();
	}

	/**
	 * Loads the image file into a button using getIMG method and adds it to the grid<br>
	 *  set EventHandler for the buttons
	 * @param name (name of restaurant)
	 * @param event (event handler)
	 */
	public void addToGrid(String name, EventHandler<ActionEvent> event,String path) {
		ImageView imgView;
		if(path!=null)
			imgView = getIMG(path);
		else imgView = getIMG("");
		VBox vBoxForGrid = new VBox(5);
		Label l = new Label(name);
		RadioButton b = new RadioButton();
		b.getStyleClass().remove("radio-button");
		b.getStyleClass().add("toggle-button");
		b.setToggleGroup(group);

		b.setPrefSize(100, 100);
		b.setGraphic(imgView);
		b.setOnAction(event);
		vBoxForGrid.getChildren().addAll(b, l);
		grid.add(vBoxForGrid, col, row);
		col++;
		if (col >= 3) {
			col = 0;
			row++;
		}
	}

	/**
	 * load image by using name<br>
	 *  if image is not found, load "not available" image 
	 * @param name (name of image)
	 * @return ImageView containing the respective image
	 */
	public ImageView getIMG(String name) {
		Image img;
		try {
			img = new Image(name);
		} catch (IllegalArgumentException e) {
			img = new Image("/images/not available.jpg");
		}
		ImageView imgView = new ImageView(img);
		imgView.setFitHeight(100);
		imgView.setFitWidth(100);
		imgView.setPreserveRatio(true);
		return imgView;
	}

	public Node getGrid() {
		return grid;
	}
}
