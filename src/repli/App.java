package repli;// repli.App.java
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.swing.Icon;
import javax.swing.UIManager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // 1) Cargar el layout FXML
        Parent root = FXMLLoader.load(getClass().getResource("/repli/main.fxml"));
        stage.setTitle("Replicador de Reportes GTR");

        // 2) Tomar el icono de “Equipo” del sistema Windows
        Icon swingIcon = UIManager.getIcon("FileView.computerIcon");
        BufferedImage bImg = new BufferedImage(
                swingIcon.getIconWidth(),
                swingIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = bImg.createGraphics();
        swingIcon.paintIcon(null, g2, 0, 0);
        g2.dispose();

        // 3) Convertir a JavaFX Image y asignar
        Image fxIcon = SwingFXUtils.toFXImage(bImg, null);
        stage.getIcons().add(fxIcon);

        // 4) Mostrar la ventana
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
