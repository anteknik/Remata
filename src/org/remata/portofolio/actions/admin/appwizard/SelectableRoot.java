package org.remata.portofolio.actions.admin.appwizard;

import com.manydesigns.elements.annotations.*;

public class SelectableRoot
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    @Updatable(false)
    public String tableName;
    @Label("")
    public boolean selected;
    
    public SelectableRoot(final String tableName, final boolean selected) {
        this.tableName = tableName;
        this.selected = selected;
    }
    
    public SelectableRoot() {
    }
}
