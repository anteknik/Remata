package org.remata.portofolio.actions.admin.database;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.remata.portofolio.persistence.*;
import org.remata.portofolio.di.*;
import org.remata.elements.*;
import org.remata.elements.messages.*;
import org.remata.portofolio.buttons.annotations.*;
import net.sourceforge.stripes.action.*;
import org.slf4j.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/reload-model")
public class ReloadModelAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/reload-model";
    @Inject("org.remata.portofolio.modules.DatabaseModule.persistence")
    Persistence persistence;
    public static final Logger logger;
    
    @DefaultHandler
    public Resolution execute() {
        return (Resolution)new ForwardResolution("/m/admin/reload-model.jsp");
    }
    
    @Button(list = "reload-model", key = "reload", order = 1.0, type = " btn-primary ")
    public Resolution reloadModel() {
        synchronized (this.persistence) {
            this.persistence.loadXmlModel();
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("model.successfully.reloaded", new Object[0]));
            return (Resolution)new ForwardResolution("/m/admin/reload-model.jsp");
        }
    }
    
    @Button(list = "reload-model-bar", key = "return.to.pages", order = 1.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)ReloadModelAction.class);
    }
}
