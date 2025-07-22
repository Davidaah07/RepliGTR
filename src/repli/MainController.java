package repli;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainController {

    @FXML private ComboBox<String> comboAplicacion;
    @FXML private TextField txtImei;
    @FXML private DatePicker fechaInicio;
    @FXML private TextField horaInicio;
    @FXML private DatePicker fechaFin;
    @FXML private TextField horaFin;
    @FXML private TextField txtLimite;
    @FXML private TextField txtIntervalo;
    @FXML private Button btnIniciar;
    @FXML private Button btnDetener;
    @FXML private Button btnSalir;
    @FXML private TextArea txtLog;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private HBox progressContainer;

    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        comboAplicacion.getItems().setAll("Max", "Plus");
        comboAplicacion.getSelectionModel().selectFirst();

        // Iconos
        Polygon playIcon = new Polygon(0,0,10,5,0,10);
        playIcon.setFill(Color.DARKGREEN);
        btnIniciar.setGraphic(playIcon);

        Rectangle stopIcon = new Rectangle(10,10);
        stopIcon.setFill(Color.RED);
        btnDetener.setGraphic(stopIcon);

        // Ocultar barra al inicio
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);
    }

    @FXML
    private void onIniciar() {
        try {
            // Deshabilitar botones y preparar log
            btnIniciar.setDisable(true);
            btnDetener.setDisable(false);
            btnSalir.setDisable(true);
            txtLog.clear();

            // Leer parámetros
            String imei = txtImei.getText().trim();
            LocalDateTime inicio = LocalDateTime.of(
                    fechaInicio.getValue(),
                    LocalTime.parse(horaInicio.getText().trim(), TIME_FMT)
            );
            LocalDateTime fin = LocalDateTime.of(
                    fechaFin.getValue(),
                    LocalTime.parse(horaFin.getText().trim(), TIME_FMT)
            );
            int limite = Integer.parseInt(txtLimite.getText().trim());
            int intervaloSegundos = Integer.parseInt(txtIntervalo.getText().trim());

            // Inicializar DAOs
            ConfigReader cfg = new ConfigReader("config.properties");
            DatabaseManager dbProd = new DatabaseManager(
                    cfg.get("source.url"), cfg.get("source.user"), cfg.get("source.password")
            );
            DatabaseManager dbQa = new DatabaseManager(
                    cfg.get("destination.url"), cfg.get("destination.user"), cfg.get("destination.password")
            );
            ReporteDAO reader = new ReporteDAO(dbProd);
            String producto = comboAplicacion.getValue();
            ReporteDAO writer = new ReporteDAO(dbQa, producto);

            // Obtener reportes
            List<Reporte> reportes = reader.obtenerReportes(
                    imei,
                    Timestamp.valueOf(inicio),
                    Timestamp.valueOf(fin),
                    limite
            );

            if (reportes.isEmpty()) {
                appendLog("No se encontraron reportes.");
                resetUI();
                return;  // ¡no mostramos la barra!
            }

            // Sólo ahora mostramos la barra y estado
            progressContainer.setVisible(true);
            progressContainer.setManaged(true);
            progressBar.setProgress(0);
            lblStatus.setText("Cargando");

            appendLog("Encontrados " + reportes.size() + " reportes. Iniciando replicación...");

            // Configurar scheduler
            stopFlag.set(false);
            AtomicInteger idx = new AtomicInteger(0);
            scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(() -> {
                int i = idx.getAndIncrement();
                if (i < reportes.size() && !stopFlag.get()) {
                    Reporte r = reportes.get(i);
                    writer.insertarReporte(r);
                    Platform.runLater(() -> {
                        appendLog(String.format("[%d/%d] Replicado: %s",
                                i+1, reportes.size(), r.getFecha()));
                        progressBar.setProgress((i+1)/(double)reportes.size());
                    });
                } else {
                    stopScheduler();
                    Platform.runLater(() -> {
                        progressBar.setProgress(1.0);
                        lblStatus.setText("Listo");
                        resetUI();
                    });
                }
            }, 0, intervaloSegundos, TimeUnit.SECONDS);

        } catch (Exception e) {
            appendLog("❌ Error: " + e.getMessage());
            resetUI();
        }
    }

    @FXML
    private void onDetener() {
        stopFlag.set(true);
        appendLog("Deteniendo replicación...");
        stopScheduler();
        resetUI();  // aquí ocultamos la barra si se detiene manual
    }

    @FXML
    private void onSalir() {
        Platform.exit();
    }

    private void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private void appendLog(String texto) {
        txtLog.appendText(texto + "\n");
    }

    private void resetUI() {
        btnIniciar.setDisable(false);
        btnDetener.setDisable(true);
        btnSalir.setDisable(false);
        if (stopFlag.get()) {
            // Ocultar barra si detuvieron
            progressContainer.setVisible(false);
            progressContainer.setManaged(false);
        }
    }
}

