package two_phase_commit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import base_de_datos.DatabaseModelMysql;
import base_de_datos.DatabaseModelPostgres;
import base_de_datos.DatabaseModelSQLServer;
import errors.ErrorHandler;

public class ConnectionManager {
    private Connection conexionFragmentos;
    private Map<Zona, Connection> conexiones;
    private static final int TIMEOUT = 5000;

    public ConnectionManager(Connection conexionFragmentos, Map<Zona, Connection> conexiones) {
        this.conexionFragmentos = conexionFragmentos;
        this.conexiones = conexiones;
    }

    public boolean crearConexiones(List<String> targetFragments) throws SQLException {
        Statement statement = conexionFragmentos.createStatement();
        ResultSet resultSet = statement
                .executeQuery("SELECT Fragmento, IP, gestor, basededatos, usuario, Contraseña FROM fragmentos");

        while (resultSet.next()) {
            String fragmento = resultSet.getString("Fragmento");
            Zona zona = obtenerZonaPorEstado(fragmento);
            if (zona != null && targetFragments.contains(zona.name())) {
                String servidor = resultSet.getString("IP");
                System.out.println("Servidor: " + servidor);
                String gestor = resultSet.getString("gestor");
                String basededatos = resultSet.getString("basededatos");
                String usuario = resultSet.getString("usuario");
                String password = resultSet.getString("Contraseña");

                Connection conexion = asignarConexion(servidor, gestor, basededatos, usuario, password);
                if (conexion == null) {
                    ErrorHandler.showMessage("No se pudo establecer la conexión con el servidor " + servidor,
                            "Error de conexión", ErrorHandler.ERROR_MESSAGE);
                    return false;
                }
                conexiones.put(zona, conexion);
            }
        }

        if (conexiones.isEmpty()) {
            ErrorHandler.showMessage("No se encontraron fragmentos para las zonas seleccionadas", "Error de fragmento",
                    ErrorHandler.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public boolean crearConexion(String fragmento) throws SQLException {
        Statement statement = conexionFragmentos.createStatement();

        ResultSet resultSet = statement
                .executeQuery("SELECT Fragmento, IP, gestor, basededatos, usuario, Contraseña FROM fragmentos");

        while (resultSet.next()) {
            String fragmentoBD = resultSet.getString("Fragmento");
            Zona zona = obtenerZonaPorEstado(fragmentoBD);
            if (zona.toString().equals(fragmento)) {
                String servidor = resultSet.getString("IP");
                String gestor = resultSet.getString("gestor");
                String basededatos = resultSet.getString("basededatos");
                String usuario = resultSet.getString("usuario");
                String password = resultSet.getString("Contraseña");

                Connection conexion = asignarConexion(servidor, gestor, basededatos, usuario, password);
                if (conexion == null) {
                    SwingUtilities.invokeLater(() -> ErrorHandler.showMessage(
                            "No se pudo establecer la conexión con el servidor " + servidor,
                            "Error de conexión", ErrorHandler.ERROR_MESSAGE));
                    return false;
                }
                conexiones.put(Zona.valueOf(fragmento.toUpperCase()), conexion);
                return true;
            }
        }
        System.err.println("No se encontró el fragmento " + fragmento);
        return false;
    }

    public Connection getConnection(Zona zona) {
        return conexiones.get(zona);
    }

    public List<String> obtenerZonasDeFragmentos() throws SQLException {
        List<String> zonas = new ArrayList<>();
        String query = "SELECT DISTINCT Fragmento FROM fragmentos";
        try (Statement stmt = conexionFragmentos.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String fragmento = rs.getString("Fragmento");
                Zona zona = obtenerZonaPorEstado(fragmento);
                if (zona != null) {
                    zonas.add(zona.name());
                }
            }
        }
        return zonas;
    }

    private Zona obtenerZonaPorEstado(String nombre) {
        for (Zona zona : Zona.values()) {
            if (zona.contieneEstado(nombre)) {
                System.out.println(nombre + " pertenece a la zona " + zona.name());
                return zona;
            }
        }
        return null;
    }

    private Connection asignarConexion(String servidor, String gestor, String basededatos, String usuario,
            String password) {
        switch (gestor.toLowerCase()) {
            case "sqlserver":
                if (!isDatabaseReachable(servidor, 1433)) {
                    System.err.println("No se pudo conectar al servidor SQL Server");
                    return null;
                }
                return new DatabaseModelSQLServer(servidor, basededatos, usuario, password).getConexion();
            case "mysql":
                if (!isDatabaseReachable(servidor, 3306)) {
                    System.err.println("No se pudo conectar al servidor MySQL");
                    return null;
                }
                return new DatabaseModelMysql(servidor, basededatos, usuario, password).getConexion();
            case "postgres":
                if (!isDatabaseReachable(servidor, 1212)) {
                    System.err.println("No se pudo conectar al servidor PostgreSQL");
                    return null;
                }
                return new DatabaseModelPostgres(servidor, basededatos, usuario, password).getConexion();
            default:
                System.err.println("Error al crear la conexión para el gestor: " + gestor);
                return null;
        }
    }

    public static boolean isDatabaseReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            socket.connect(socketAddress, TIMEOUT);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void closeConnections() {
        for (Connection connection : conexiones.values()) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
        conexiones.clear();
    }
}