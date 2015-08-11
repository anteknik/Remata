package org.remata.portofolio.actions.admin.database.forms;

import org.remata.portofolio.model.database.*;
import org.apache.commons.lang.*;
import com.manydesigns.elements.annotations.*;
import java.util.*;

public class ConnectionProviderForm
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    protected Database database;
    protected JdbcConnectionProvider jdbcConnectionProvider;
    protected JndiConnectionProvider jndiConnectionProvider;
    
    public ConnectionProviderForm(final Database database) {
        this.database = database;
        if (database.getConnectionProvider() instanceof JdbcConnectionProvider) {
            this.jdbcConnectionProvider = (JdbcConnectionProvider)database.getConnectionProvider();
        }
        else {
            if (!(database.getConnectionProvider() instanceof JndiConnectionProvider)) {
                throw new IllegalArgumentException("Invalid connection provider type: " + database.getConnectionProvider());
            }
            this.jndiConnectionProvider = (JndiConnectionProvider)database.getConnectionProvider();
        }
    }
    
    public void setDatabaseName(final String databaseName) {
        this.database.setDatabaseName(databaseName);
    }
    
    @Updatable(false)
    @Required(true)
    public String getDatabaseName() {
        return this.database.getDatabaseName();
    }
    
    public void setTrueString(final String trueString) {
        this.database.setTrueString(StringUtils.defaultIfEmpty(trueString, (String)null));
    }
    
    public String getTrueString() {
        return this.database.getTrueString();
    }
    
    public void setFalseString(final String falseString) {
        this.database.setFalseString(StringUtils.defaultIfEmpty(falseString, (String)null));
    }
    
    public String getFalseString() {
        return this.database.getFalseString();
    }
    
    @FieldSize(50)
    @Required
    @CssClass({ "fill-row" })
    public String getDriver() {
        return this.jdbcConnectionProvider.getDriver();
    }
    
    public void setDriver(final String driver) {
        this.jdbcConnectionProvider.setDriver(driver);
    }
    
    @FieldSize(100)
    @Required
    @Label("connection URL")
    public String getUrl() {
        return this.jdbcConnectionProvider.getActualUrl();
    }
    
    public void setUrl(final String url) {
        this.jdbcConnectionProvider.setActualUrl(url);
    }
    
    public String getUsername() {
        return this.jdbcConnectionProvider.getActualUsername();
    }
    
    public void setUsername(final String username) {
        this.jdbcConnectionProvider.setActualUsername(username);
    }
    
    @Password
    public String getPassword() {
        return this.jdbcConnectionProvider.getActualPassword();
    }
    
    public void setPassword(final String password) {
        this.jdbcConnectionProvider.setActualPassword(password);
    }
    
    @Required
    public String getJndiResource() {
        return this.jndiConnectionProvider.getJndiResource();
    }
    
    public void setJndiResource(final String jndiResource) {
        this.jndiConnectionProvider.setJndiResource(jndiResource);
    }
    
    @Label("Hibernate dialect (leave empty to use default)")
    public String getHibernateDialect() {
        if (this.jdbcConnectionProvider != null) {
            return this.jdbcConnectionProvider.getHibernateDialect();
        }
        if (this.jndiConnectionProvider != null) {
            return this.jndiConnectionProvider.getHibernateDialect();
        }
        return null;
    }
    
    public void setHibernateDialect(final String dialect) {
        if (this.jdbcConnectionProvider != null) {
            this.jdbcConnectionProvider.setHibernateDialect(dialect);
        }
        else {
            if (this.jndiConnectionProvider == null) {
                throw new Error("Misconfigured");
            }
            this.jndiConnectionProvider.setHibernateDialect(dialect);
        }
    }
    
    public String getErrorMessage() {
        if (this.jdbcConnectionProvider != null) {
            return this.jdbcConnectionProvider.getErrorMessage();
        }
        if (this.jndiConnectionProvider != null) {
            return this.jndiConnectionProvider.getErrorMessage();
        }
        return null;
    }
    
    public String getStatus() {
        if (this.jdbcConnectionProvider != null) {
            return this.jdbcConnectionProvider.getStatus();
        }
        if (this.jndiConnectionProvider != null) {
            return this.jndiConnectionProvider.getStatus();
        }
        return null;
    }
    
    public Date getLastTested() {
        if (this.jdbcConnectionProvider != null) {
            return this.jdbcConnectionProvider.getLastTested();
        }
        if (this.jndiConnectionProvider != null) {
            return this.jndiConnectionProvider.getLastTested();
        }
        return null;
    }
}
