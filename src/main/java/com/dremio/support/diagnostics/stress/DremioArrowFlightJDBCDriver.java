package com.dremio.support.diagnostics.stress;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;

public class DremioArrowFlightJDBCDriver implements DremioApi {

    private final Connection connection;
    private final Object currentContextLock = new Object();
    private String currentContext = "";
    public DremioArrowFlightJDBCDriver(UsernamePasswordAuth auth, String host, boolean ignoreSSL, Integer timeoutSeconds) {
        try {
            Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try{
        connection = DriverManager.getConnection(host + "&user=" + auth.getUsername() + "&password="+ auth.getPassword()+ "&disableCertificateVerification="+ ignoreSSL);
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
        response.setCreated(false);
        synchronized (currentContextLock){
            if (!currentContext.equals(context)){
                currentContext = context;
                try {
                    if (!connection.createStatement().execute("USE " + context)){
                        response.setErrorMessage("failed using USE");
                        return response;
                    }
                    if (connection.createStatement().execute(sql)){
                        response.setErrorMessage("");
                        return response;
                    }
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
                response.setErrorMessage("");
                return response;
            } else {
                response.setErrorMessage("unhandled exception");
                return response;
            }
        } catch (SQLException e) {
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
