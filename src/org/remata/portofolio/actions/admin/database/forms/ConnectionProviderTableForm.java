package org.remata.portofolio.actions.admin.database.forms;

import org.remata.elements.annotations.*;

public class ConnectionProviderTableForm
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public String databaseName;
    @Status(red = { "error" }, amber = { "disconnected" }, green = { "connected" })
    public String status;
    public String description;
    
    public ConnectionProviderTableForm() {
    }
    
    public ConnectionProviderTableForm(final String databaseName, final String description, final String status) {
        this.databaseName = databaseName;
        this.status = status;
        this.description = description;
    }
}
