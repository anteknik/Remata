package org.remata.portofolio.actions.admin.page.forms;

import org.remata.portofolio.pages.*;
import org.remata.elements.options.*;
import org.remata.elements.annotations.*;

public class NewPage extends Page
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    protected String actionClassName;
    protected String insertPositionName;
    protected String fragment;
    
    @Label("Page type")
    @Required
    public String getActionClassName() {
        return this.actionClassName;
    }
    
    public void setActionClassName(final String actionClassName) {
        this.actionClassName = actionClassName;
    }
    
    @Label("Where")
    @Select(displayMode = DisplayMode.RADIO)
    @Required
    public String getInsertPositionName() {
        return this.insertPositionName;
    }
    
    public void setInsertPositionName(final String insertPositionName) {
        this.insertPositionName = insertPositionName;
    }
    
    @RegExp(value = "[a-zA-Z0-9][a-zA-Z0-9_\\-]*", errorMessage = "invalid.fragment.only.letters.numbers.etc.are.allowed")
    @Required
    public String getFragment() {
        return this.fragment;
    }
    
    public void setFragment(final String fragment) {
        this.fragment = fragment;
    }
}
