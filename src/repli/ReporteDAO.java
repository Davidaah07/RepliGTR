package repli;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReporteDAO {
    private final DatabaseManager dbManager;
    private final String producto; // "Max" o "Plus"

    // Constructor para lectura
    public ReporteDAO(DatabaseManager dbManager) {
        this(dbManager, null);
    }

    // Constructor para escritura, indicando el producto
    public ReporteDAO(DatabaseManager dbManager, String producto) {
        this.dbManager = dbManager;
        this.producto = producto;
    }

    /**
     * Obtiene reportes en un rango de fechas, hasta un máximo de 'limite' registros.
     */
    public List<Reporte> obtenerReportes(String imei, Timestamp inicio, Timestamp fin, int limite) {
        List<Reporte> reportes = new ArrayList<>();
        String sql =
                "SELECT cod_equ AS imei, fec_rep AS fecha, org_lat AS latitud, org_lon AS longitud, " +
                        "       org_vel AS velocidad, num_sat AS nivel_senal " +
                        "  FROM general.reportes " +
                        " WHERE cod_equ = ? AND fec_rep BETWEEN ? AND ? " +
                        " ORDER BY fec_rep " +
                        " LIMIT ?";

        try (Connection conn = dbManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, imei);
            ps.setTimestamp(2, inicio);
            ps.setTimestamp(3, fin);
            ps.setInt(4, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reportes.add(new Reporte(
                            rs.getString("imei"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getDouble("latitud"),
                            rs.getDouble("longitud"),
                            rs.getDouble("velocidad"),
                            rs.getInt("nivel_senal"),
                            null  // no replicamos temperatura
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener reportes: " + e.getMessage());
        }
        return reportes;
    }

    /**
     * Inserta un reporte en general.reportes (QA), incluyendo cod_pro_apl y fec_rep_par
     * para que PostgreSQL lo distribuya a la partición correcta.
     */
    public boolean insertarReporte(Reporte r) {
        if (producto == null) {
            throw new IllegalStateException("Debe indicar el producto en el constructor para insertar.");
        }
        String sql = ""
                + "INSERT INTO general.reportes "
                + "(cod_equ, fec_rep, org_lat, org_lon, org_vel, num_sat, cod_pro_apl, fec_rep_par) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getImei());
            ps.setTimestamp(2, Timestamp.valueOf(r.getFecha()));
            ps.setDouble(3, r.getLatitud());
            ps.setDouble(4, r.getLongitud());
            ps.setDouble(5, r.getVelocidad());
            ps.setInt(6, r.getNivelSenal());
            ps.setString(7, producto);
            // La columna fec_rep_par es solo la fecha, sin hora
            ps.setDate(8, Date.valueOf(r.getFecha().toLocalDate()));

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al insertar reporte: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene los últimos 'limite' reportes para un IMEI, ordenados cronológicamente.
     */
    public List<Reporte> obtenerUltimosReportes(String imei, int limite) {
        List<Reporte> lista = new ArrayList<>();
        String sql =
                "SELECT cod_equ AS imei, fec_rep AS fecha, org_lat AS latitud, org_lon AS longitud, " +
                        "       org_vel AS velocidad, num_sat AS nivel_senal " +
                        "  FROM general.reportes " +
                        " WHERE cod_equ = ? " +
                        " ORDER BY fec_rep DESC " +
                        " LIMIT ?";

        try (Connection conn = dbManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, imei);
            ps.setInt(2, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Reporte(
                            rs.getString("imei"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            rs.getDouble("latitud"),
                            rs.getDouble("longitud"),
                            rs.getDouble("velocidad"),
                            rs.getInt("nivel_senal"),
                            null
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener últimos reportes: " + e.getMessage());
        }

        Collections.reverse(lista);
        return lista;
    }
}

