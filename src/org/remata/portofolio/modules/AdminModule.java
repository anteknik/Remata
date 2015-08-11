package org.remata.portofolio.modules;

import javax.servlet.*;
import org.remata.portofolio.di.*;
import org.remata.portofolio.menu.*;
import org.slf4j.*;

public class AdminModule implements Module
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String ADMIN_MENU = "org.remata.portofolio.menu.Menu.admin";
    @Inject("org.remata.portofolio.servletContext")
    public ServletContext servletContext;
    protected ModuleStatus status;
    public static final Logger logger;
    
    public AdminModule() {
        this.status = ModuleStatus.CREATED;
    }
    
    public String getModuleVersion() {
        return ModuleRegistry.getPortofinoVersion();
    }
    
    public int getMigrationVersion() {
        return 1;
    }
    
    public double getPriority() {
        return 30.0;
    }
    
    public String getId() {
        return "admin";
    }
    
    public String getName() {
        return "Admin";
    }
    
    public int install() {
        return 1;
    }
    
    public void init() {
        AdminModule.logger.debug("Installing standard menu builders");
        final MenuBuilder adminMenu = new MenuBuilder();
        SimpleMenuAppender group = SimpleMenuAppender.group("configuration", (String)null, "Configuration", 1.0);
        adminMenu.menuAppenders.add(group);
        SimpleMenuAppender link = SimpleMenuAppender.link("configuration", "settings", (String)null, "Settings", "/actions/admin/settings", 1.0);
        adminMenu.menuAppenders.add(link);
        link = SimpleMenuAppender.link("configuration", "modules", (String)null, "Modules", "/actions/admin/modules", 2.0);
        adminMenu.menuAppenders.add(link);
        link = SimpleMenuAppender.link("configuration", "servlet-context", (String)null, "Servlet Context", "/actions/admin/servlet-context", 3.0);
        adminMenu.menuAppenders.add(link);
        link = SimpleMenuAppender.link("configuration", "topLevelPages", (String)null, "Top-level pages", "/actions/admin/root-page/children", 4.0);
        adminMenu.menuAppenders.add(link);
        group = SimpleMenuAppender.group("security", (String)null, "Security", 2.0);
        adminMenu.menuAppenders.add(group);
        link = SimpleMenuAppender.link("security", "rootPermissions", (String)null, "Root permissions", "/actions/admin/root-page/permissions", 1.0);
        adminMenu.menuAppenders.add(link);
        group = SimpleMenuAppender.group("dataModeling", (String)null, "Data modeling", 3.0);
        adminMenu.menuAppenders.add(group);
        link = SimpleMenuAppender.link("dataModeling", "wizard", (String)null, "Wizard", "/actions/admin/wizard", 1.0);
        adminMenu.menuAppenders.add(link);
        link = SimpleMenuAppender.link("dataModeling", "connectionProviders", (String)null, "Connection providers", "/actions/admin/connection-providers", 2.0);
        adminMenu.menuAppenders.add(link);
        link = SimpleMenuAppender.link("dataModeling", "tables", (String)null, "Tables", "/actions/admin/tables", 3.0);
        adminMenu.menuAppenders.add(link);
        link = SimpleMenuAppender.link("dataModeling", "reloadModel", (String)null, "Reload model", "/actions/admin/reload-model", 4.0);
        adminMenu.menuAppenders.add(link);
        group = SimpleMenuAppender.group("mail", (String)null, "Mail", 4.0);
        adminMenu.menuAppenders.add(group);
        link = SimpleMenuAppender.link("mail", "Mail", (String)null, "Mail", "/actions/admin/mail/settings", 1.0);
        adminMenu.menuAppenders.add(link);
        this.servletContext.setAttribute("org.remata.portofolio.menu.Menu.admin", (Object)adminMenu);
        this.status = ModuleStatus.ACTIVE;
    }
    
    public void start() {
        this.status = ModuleStatus.STARTED;
    }
    
    public void stop() {
        this.status = ModuleStatus.STOPPED;
    }
    
    public void destroy() {
        this.status = ModuleStatus.DESTROYED;
    }
    
    public ModuleStatus getStatus() {
        return this.status;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)AdminModule.class);
    }
}
