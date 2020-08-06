import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientController  implements Initializable {

    private Network network;

    @FXML
    TextField loginText;

    @FXML
    PasswordField passText;

    @FXML
    Button btnLogin;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }


    public void btnLoginOnAction(ActionEvent actionEvent) {

    }
}
