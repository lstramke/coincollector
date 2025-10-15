package io.github.lstramke.coincollector.configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSourceAutoActivateForeignKeys implements DataSource {
    private final DataSource delegate;

    public DataSourceAutoActivateForeignKeys(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = delegate.getConnection();
        enableForeignKeys(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = delegate.getConnection(username, password);
        enableForeignKeys(conn);
        return conn;
    }

    private void enableForeignKeys(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }
    
    @Override 
    public <T> T unwrap(Class<T> iface) throws SQLException { 
        return delegate.unwrap(iface);
    }
    
    @Override 
    public boolean isWrapperFor(Class<?> iface) throws SQLException { 
        return delegate.isWrapperFor(iface); 
    }
    
    @Override 
    public java.io.PrintWriter getLogWriter() throws SQLException { 
        return delegate.getLogWriter(); 
    }
    
    @Override 
    public void setLogWriter(java.io.PrintWriter out) throws SQLException { 
        delegate.setLogWriter(out);
    }
    
    @Override 
    public void setLoginTimeout(int seconds) throws SQLException { 
        delegate.setLoginTimeout(seconds); 
    }
    
    @Override 
    public int getLoginTimeout() throws SQLException { 
        return delegate.getLoginTimeout(); 
    }
    
    @Override 
    public java.util.logging.Logger getParentLogger() { 
        throw new UnsupportedOperationException(); 
    }
}

