package org.remata.portofolio.actions.admin;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.apache.commons.configuration.*;
import org.remata.portofolio.di.*;
import java.io.*;
import org.remata.portofolio.dispatcher.*;
import org.remata.elements.forms.*;
import org.remata.elements.options.*;
import org.remata.elements.configuration.*;
import org.remata.elements.*;
import org.remata.elements.messages.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;
import org.slf4j.*;
import org.remata.elements.annotations.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/settings")
public class SettingsAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/settings";
    @Inject("portofinoConfiguration")
    public Configuration configuration;
    @Inject("PAGES_DIRECTORY")
    public File pagesDir;
    Form form;
    public static final Logger logger;
    
    @DefaultHandler
    public Resolution execute() {
        this.setupFormAndBean();
        return (Resolution)new ForwardResolution("/m/admin/settings.jsp");
    }
    
    private void setupFormAndBean() {
        final SelectionProvider pagesSelectionProvider = DispatcherLogic.createPagesSelectionProvider(this.pagesDir, new File[0]);
        final Settings settings = new Settings();
        settings.appName = this.configuration.getString("app.name");
        settings.landingPage = this.configuration.getString("landing.page");
        settings.loginPage = this.configuration.getString("login.page");
        settings.preloadGroovyPages = this.configuration.getBoolean("groovy.preloadPages", false);
        settings.preloadGroovyClasses = this.configuration.getBoolean("groovy.preloadClasses", false);
        (this.form = new FormBuilder((Class)Settings.class).configSelectionProvider(pagesSelectionProvider, new String[] { "landingPage" }).configSelectionProvider(pagesSelectionProvider, new String[] { "loginPage" }).build()).readFromObject((Object)settings);
    }
    
    @Button(list = "settings", key = "update", order = 1.0, type = " btn-primary ")
    public Resolution update() {
        this.setupFormAndBean();
        this.form.readFromRequest(this.context.getRequest());
        if (this.form.validate()) {
            SettingsAction.logger.debug("Applying settings to model");
            try {
                final Settings settings = new Settings();
                this.form.writeToObject((Object)settings);
                this.configuration.setProperty("app.name", (Object)settings.appName);
                this.configuration.setProperty("landing.page", (Object)settings.landingPage);
                this.configuration.setProperty("login.page", (Object)settings.loginPage);
                if (!settings.preloadGroovyPages || this.configuration.getProperty("groovy.preloadPages") != null) {
                    this.configuration.setProperty("groovy.preloadPages", (Object)settings.preloadGroovyPages);
                }
                if (!settings.preloadGroovyClasses || this.configuration.getProperty("groovy.preloadClasses") != null) {
                    this.configuration.setProperty("groovy.preloadClasses", (Object)settings.preloadGroovyClasses);
                }
                CommonsConfigurationUtils.save(this.configuration);
                SettingsAction.logger.info("Configuration saved");
            }
            catch (Exception e) {
                SettingsAction.logger.error("Configuration not saved", (Throwable)e);
                SessionMessages.addErrorMessage(ElementsThreadLocals.getText("the.configuration.could.not.be.saved", new Object[0]));
                return (Resolution)new ForwardResolution("/m/admin/settings.jsp");
            }
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("configuration.updated.successfully", new Object[0]));
            return (Resolution)new RedirectResolution((Class)this.getClass());
        }
        return (Resolution)new ForwardResolution("/m/admin/settings.jsp");
    }
    
    @Button(list = "settings", key = "return.to.pages", order = 2.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    public Form getForm() {
        return this.form;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)SettingsAction.class);
    }
    
    public static class Settings
    {
        @Required
        @Label("Application name")
        @CssClass({ "fill-row" })
        public String appName;
        @Required
        public String landingPage;
        @Required
        public String loginPage;
        @Required
        @Label("Preload Groovy pages at startup")
        public boolean preloadGroovyPages;
        @Required
        @Label("Preload Groovy shared classes at startup")
        public boolean preloadGroovyClasses;
    }
}
