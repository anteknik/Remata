package org.remata.portofolio.actions.admin.modules;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.remata.portofolio.di.*;
import org.remata.elements.forms.*;
import org.remata.elements.*;
import org.remata.portofolio.modules.*;
import java.util.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;
import org.slf4j.*;
import org.remata.elements.annotations.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/modules")
public class ModulesAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, ManyDesigns srl";
    public static final String URL_BINDING = "/actions/admin/modules";
    @Inject("org.remata.portofolio.modules.ModuleRegistry")
    ModuleRegistry moduleRegistry;
    TableForm form;
    public static final Logger logger;
    
    @DefaultHandler
    public Resolution execute() {
        this.setupForm();
        return (Resolution)new ForwardResolution("/m/admin/modules/list.jsp");
    }
    
    protected void setupForm() {
        final TableFormBuilder builder = new TableFormBuilder((Class)ModuleView.class);
        builder.configNRows(this.moduleRegistry.getModules().size());
        builder.configMode(Mode.VIEW);
        this.form = builder.build();
        final List<ModuleView> modules = new ArrayList<ModuleView>();
        for (final Module module : this.moduleRegistry.getModules()) {
            final ModuleView view = new ModuleView();
            view.id = module.getId();
            view.name = module.getName();
            view.status = module.getStatus().name();
            view.version = module.getModuleVersion();
            modules.add(view);
        }
        this.form.readFromObject((Object)modules);
    }
    
    @Button(list = "modules", key = "return.to.pages", order = 2.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    public TableForm getForm() {
        return this.form;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)ModulesAction.class);
    }
    
    public static final class ModuleView
    {
        public String id;
        public String name;
        public String version;
        @Status(red = { "FAILED", "DESTROYED" }, amber = { "CREATED", "STOPPED" }, green = { "ACTIVE", "STARTED" })
        public String status;
    }
}
