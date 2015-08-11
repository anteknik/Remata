package org.remata.portofolio.actions.admin.database.forms;

import org.remata.elements.annotations.*;

public class SelectableSchema
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    @Updatable(false)
    public String schemaName;
    @Label("")
    public boolean selected;
    
    public SelectableSchema(final String schemaName, final boolean selected) {
        this.schemaName = schemaName;
        this.selected = selected;
    }
    
    public SelectableSchema() {
    }
}
