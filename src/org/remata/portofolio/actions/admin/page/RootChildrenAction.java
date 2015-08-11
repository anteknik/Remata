package org.remata.portofolio.actions.admin.page;

import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import java.io.*;
import org.remata.portofolio.di.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/root-page/children")
public class RootChildrenAction extends RootConfigurationAction
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/root-page/children";
    @Inject("PAGES_DIRECTORY")
    public File pagesDir;
    
    @DefaultHandler
    @Override
    public Resolution pageChildren() {
        return super.pageChildren();
    }
    
    @Override
    protected Resolution forwardToPageChildren() {
        return (Resolution)new ForwardResolution("/m/admin/page/rootChildren.jsp");
    }
    
    @Button(list = "root-children", key = "update", order = 1.0, type = " btn-primary ")
    @Override
    public Resolution updatePageChildren() {
        return super.updatePageChildren();
    }
    
    @Override
    protected void setupChildPages() {
        this.childPagesForm = this.setupChildPagesForm(this.childPages, this.pagesDir, this.getPage().getLayout(), "");
    }
    
    @Override
    protected String[] getChildPagesFormFields() {
        return new String[] { "active", "name", "title", "showInNavigation" };
    }
}
