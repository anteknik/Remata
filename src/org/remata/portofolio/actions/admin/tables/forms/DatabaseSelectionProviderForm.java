package org.remata.portofolio.actions.admin.tables.forms;

import org.remata.portofolio.model.database.*;
import org.apache.commons.beanutils.*;
import org.remata.elements.annotations.*;

public class DatabaseSelectionProviderForm extends DatabaseSelectionProvider
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    protected String columns;
    
    public DatabaseSelectionProviderForm(final DatabaseSelectionProvider copyFrom) {
        try {
            BeanUtils.copyProperties((Object)this, (Object)copyFrom);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
    
    public DatabaseSelectionProvider copyTo(final DatabaseSelectionProvider dsp) {
        try {
            BeanUtils.copyProperties((Object)dsp, (Object)this);
        }
        catch (Exception e) {
            throw new Error(e);
        }
        return dsp;
    }
    
    @Required
    @FieldSize(50)
    public String getName() {
        return super.getName();
    }
    
    @Required
    public String getToDatabase() {
        return super.getToDatabase();
    }
    
    @Multiline
    public String getHql() {
        return super.getHql();
    }
    
    @Multiline
    public String getSql() {
        return super.getSql();
    }
    
    @FieldSize(75)
    @Required
    public String getColumns() {
        return this.columns;
    }
    
    public void setColumns(final String columns) {
        this.columns = columns;
    }
}
