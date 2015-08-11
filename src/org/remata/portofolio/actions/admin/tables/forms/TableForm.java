package org.remata.portofolio.actions.admin.tables.forms;

import org.remata.portofolio.model.database.*;
import org.apache.commons.beanutils.*;
import org.apache.commons.lang.*;
import org.remata.elements.annotations.*;

public class TableForm extends Table
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    
    public TableForm(final Table copyFrom) {
        try {
            BeanUtils.copyProperties((Object)this, (Object)copyFrom);
            this.actualEntityName = copyFrom.getActualEntityName();
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
    
    public Table copyTo(final Table table) {
        try {
            BeanUtils.copyProperties((Object)table, (Object)this);
        }
        catch (Exception e) {
            throw new Error(e);
        }
        return table;
    }
    
    @FieldSize(50)
    @RegExp(value = "(_|$|[a-z]|[A-Z]|[\u0080-\ufffe])(_|$|[a-z]|[A-Z]|[\u0080-\ufffe]|[0-9])*", errorMessage = "invalid.entity.name")
    @LabelI18N("entity.name")
    public String getEntityName() {
        return super.getEntityName();
    }
    
    @FieldSize(50)
    @LabelI18N("java.class")
    public String getJavaClass() {
        return super.getJavaClass();
    }
    
    @FieldSize(50)
    @LabelI18N("short.name")
    public String getShortName() {
        return super.getShortName();
    }
    
    @Insertable(false)
    @Updatable(false)
    public String getHqlQuery() {
        return "from " + StringUtils.defaultIfEmpty(this.entityName, this.actualEntityName);
    }
    
    public String toString() {
        return "Not used.";
    }
}
