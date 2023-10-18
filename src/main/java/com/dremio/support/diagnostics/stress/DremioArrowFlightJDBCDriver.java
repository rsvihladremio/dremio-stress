package com.dremio.support.diagnostics.stress;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DremioArrowFlightJDBCDriver implements DremioApi {

    private static final Logger logger = Logger.getLogger(DremioArrowFlightJDBCDriver.class.getName());
    private final Connection connection;
    private final Object currentContextLock = new Object();
    private String currentContext = "";
    public DremioArrowFlightJDBCDriver(String url) {
        try {
            Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try{
        connection = DriverManager.getConnection(url);
            // use con here
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * runs a sql statement over jdbc
     *
     * @param sql   sql string to submit to dremio
     * @param table
     * @return the result of the job
     * @throws IOException occurs when the underlying apiCall does, typically a problem with handling
     *                     of the body
     */
    @Override
    public DremioApiResponse runSQL(String sql, Collection<String> table) throws IOException {
        String context = String.join(".", table);
        var response = new DremioApiResponse();
        synchronized (currentContextLock){
            if (!currentContext.equals(context)){
                currentContext = context;
                try {
                    logger.info(()->String.format("changing context %s", context));
                    if (!connection.createStatement().execute("USE " + context)){
                        response.setErrorMessage("failed using USE");
                        response.setSuccessful(false);
                        return response;
                    }
                    if (connection.createStatement().execute(sql)){
                        response.setSuccessful(true);
                        response.setErrorMessage("");
                        return response;
                    }
                    response.setSuccessful(false);
                    response.setErrorMessage("unhandled error executing sql");
                    return response;
                } catch (SQLException e) {
                    response.setErrorMessage(e.getMessage());
                    return response;
                }
            }
        }
        try {
            if (connection.createStatement().execute(sql)) {
                response.setSuccessful(true);
                response.setErrorMessage("");
            } else {
                response.setSuccessful(false);
                response.setErrorMessage("unhandled exception");
            }
            return response;
        } catch (SQLException e) {
            response.setSuccessful(false);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    /**
     * The http URL for the dremio server
     *
     * @return return the url used to access Dremio
     */
    @Override
    public String getUrl() {
        return "";
    }
}
