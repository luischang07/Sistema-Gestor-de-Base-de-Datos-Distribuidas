package two_phase_commit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import errors.ErrorHandler;

public class QueryParser {
    private static final String SELECT_REGEX = "(?i)^\\s*SELECT\\s+.+\\s+FROM\\s+.+(\\s+WHERE\\s+.+)?$";
    private static final String INSERT_REGEX = "(?i)^\\s*INSERT\\s+INTO\\s+.+\\s+VALUES\\s*\\(.+\\)\\s*$";
    private static final String UPDATE_REGEX = "(?i)^\\s*UPDATE\\s+.+\\s+SET\\s+.+(\\s+WHERE\\s+.+)?$";
    private static final String DELETE_REGEX = "(?i)^\\s*DELETE\\s+FROM\\s+.+(\\s+WHERE\\s+.+)?$";
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b.*\\bEstado\\b");

    private ConnectionManager connectionManager;

    public QueryParser(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<String> parseQuery(String query, boolean isSelect) throws ErrorHandler {
        List<String> targetFragments = new ArrayList<>();

        if (isSelect) {
            if (!isValidSelectStatement(query)) {
                throw new ErrorHandler("Invalid SELECT statement syntax");
            }
            agregarFragmentosPorCondiciones(query, targetFragments);
        } else {
            if (!isValidModifyingStatement(query)) {
                throw new ErrorHandler("Invalid INSERT, UPDATE, or DELETE statement syntax");
            }
            if (query.toLowerCase().startsWith("insert")) {
                agregarFragmentosPorZona(query, targetFragments);
            } else {
                agregarFragmentosPorCondiciones(query, targetFragments);
            }
        }

        if (targetFragments.isEmpty()) {
            agregarTodasLasZonas(targetFragments);
        }

        return targetFragments;
    }

    private boolean isValidSelectStatement(String query) {
        return Pattern.matches(SELECT_REGEX, query);
    }

    private boolean isValidModifyingStatement(String query) {
        return Pattern.matches(INSERT_REGEX, query) ||
                Pattern.matches(UPDATE_REGEX, query) ||
                Pattern.matches(DELETE_REGEX, query);
    }

    private void agregarFragmentosPorZona(String query, List<String> targetFragments) {
        for (Zona zona : Zona.values()) {
            for (String estado : zona.getEstados()) {
                if (query.toLowerCase().contains(estado.toLowerCase())) {
                    targetFragments.add(zona.name());
                    System.out.println("Agregando " + zona.name());
                    break;
                }
            }
        }
    }

    private void agregarFragmentosPorCondiciones(String query, List<String> targetFragments) {
        Matcher whereMatcher = WHERE_PATTERN.matcher(query);
        if (whereMatcher.find()) {
            agregarFragmentosPorZona(query, targetFragments);
        }
    }

    private void agregarTodasLasZonas(List<String> targetFragments) {
        try {
            List<String> zonasEnFragmentos = connectionManager.obtenerZonasDeFragmentos();
            targetFragments.addAll(zonasEnFragmentos);
        } catch (SQLException e) {
            System.err.println("Error al obtener las zonas de la tabla de fragmentos: " + e.getMessage());
            for (Zona zona : Zona.values()) {
                targetFragments.add(zona.name());
            }
        }
    }
}