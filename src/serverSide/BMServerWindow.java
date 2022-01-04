package serverSide;


import common.Logger;
import common.Logger.Level;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BMServerWindow {

	  	@FXML
	    private VBox pathVBox;

	    @FXML
	    private TextField txtPort;

	    @FXML
	    private Button connectBtn;

	    @FXML
	    private Button disconnectBtn;
	    
	    @FXML
	    private TextField txtUser;
	    
	    @FXML
	    private PasswordField txtPassword;
	    
	    @FXML
	    private CheckBox defaultCB;
	    
	    @FXML
	    private Button importButton;

	    private static PeriodicActivityService periodicSrvc = new PeriodicActivityService();
	    public void start(Stage primaryStage) throws Exception {	
	   
			Parent root = FXMLLoader.load(getClass().getResource("/templates/ServerPort.fxml"));
			Scene scene = new Scene(root);
			primaryStage.setTitle("ServerClient");
			primaryStage.setScene(scene);
			primaryStage.show();
			
		}
		
	    @FXML
	    void onClickConnect(ActionEvent event) {
	    	String port = txtPort.getText();
	    	BMServer.sendUser(txtUser.getText());
	    	BMServer.sendPassword(txtPassword.getText());
	    	BMServer.runServer(port);
	    	connectBtn.disableProperty().set(true);
	    	disconnectBtn.disableProperty().set(false);
	    	importButton.setDisable(false);
	    	}

	    @FXML
	    void onClickDis(ActionEvent event) {
	    	connectBtn.disableProperty().set(false);
	    	disconnectBtn.disableProperty().set(true);
	    	BMServer.stopServer();
	    }
	    
	    //autofill
	    public void onCBClick(ActionEvent event) {
	    	txtPort.setText(String.valueOf(BMServer.DEFAULT_PORT));
			txtUser.setText(BMServer.DEFAULT_USER);
			txtPassword.setText(BMServer.DEFAULT_PASSWORD);	
	    }
	    @FXML
	    void onClickImportButton(ActionEvent event) {
	    	DataBase db = new DataBase();
	    	importButton.disableProperty().set(true);
	    	db.importUsers();
	    	periodicSrvc.start();
	    	importButton.setDisable(true);
	    	txtPort.setDisable(true);
	    	txtUser.setDisable(true);
	    }
	    

}
