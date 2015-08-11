package org.remata.portofolio.actions.admin.page;

import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/root-page/permissions")
public class RootPermissionsAction extends RootConfigurationAction
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/root-page/permissions";
    
    @DefaultHandler
    @Override
    public Resolution pagePermissions() {
        return super.pagePermissions();
    }
    
    @Override
    protected Resolution forwardToPagePermissions() {
        return (Resolution)new ForwardResolution("/m/admin/page/rootPermissions.jsp");
    }
    
    @Button(list = "root-permissions", key = "update", order = 1.0, type = " btn-primary ")
    @Override
    public Resolution updatePagePermissions() {
        return super.updatePagePermissions();
    }
}
