package two_phase_commit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TransferQueue;

import javax.swing.table.DefaultTableModel;

import errors.ErrorHandler;

public class Modelo {

    public static final int TRANSACCIONES = 0;
    public static final int CONSULTAS = 1;
    public List<Map<String, Object>> resultados;

    private Connection conexion;

    public void transacciones(int tipo, String query) {
        if (tipo == CONSULTAS) {
            SQLparser parser = new SQLparser(conexion);
            resultados = parser.ejecutarSelect(query);
            System.out.println(resultados);
        } else if (tipo == TRANSACCIONES) {
            SQLparser parser = new SQLparser(conexion);
            try {
                parser.ejecutarTransacion(query);
            } catch (SQLException e) {
                ErrorHandler.showMessage("Error en la transacción: " + e.getMessage(), "Error de transacción",
                        0);
            }
        }
    }

    public Connection getConexion() {
        return conexion;
    }

    public void setConexion(Connection conexion) {
        this.conexion = conexion;
    }

    public void cerrarConexion() {
        this.conexion = null;
    }

}
