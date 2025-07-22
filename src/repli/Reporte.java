package repli;

import java.time.LocalDateTime;

public class Reporte {
    private String imei;
    private LocalDateTime fecha;
    private double latitud;
    private double longitud;
    private double velocidad;
    private int nivelSenal;
    private Double temperatura; // puede ser null

    public Reporte(String imei, LocalDateTime fecha, double latitud, double longitud,
                   double velocidad, int nivelSenal, Double temperatura) {
        this.imei = imei;
        this.fecha = fecha;
        this.latitud = latitud;
        this.longitud = longitud;
        this.velocidad = velocidad;
        this.nivelSenal = nivelSenal;
        this.temperatura = temperatura;
    }

    // Getters
    public String getImei() { return imei; }
    public LocalDateTime getFecha() { return fecha; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public double getVelocidad() { return velocidad; }
    public int getNivelSenal() { return nivelSenal; }
    public Double getTemperatura() { return temperatura; }
}

