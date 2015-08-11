package org.remata.portofolio.actions.admin.mail;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.apache.commons.configuration.*;
import org.remata.portofolio.di.*;
import org.remata.elements.configuration.*;
import org.remata.elements.*;
import org.remata.elements.messages.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;
import org.remata.elements.forms.*;
import org.slf4j.*;
import org.remata.elements.annotations.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/mail/settings")
public class MailSettingsAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, ManyDesigns srl";
    public static final String URL_BINDING = "/actions/admin/mail/settings";
    @Inject("portofinoConfiguration")
    public Configuration configuration;
    Form form;
    public static final Logger logger;
    
    @DefaultHandler
    public Resolution execute() {
        this.setupFormAndBean();
        return (Resolution)new ForwardResolution("/m/admin/mail/settings.jsp");
    }
    
    @Button(list = "settings", key = "update", order = 1.0, type = " btn-primary ")
    public Resolution update() {
        this.setupFormAndBean();
        this.form.readFromRequest(this.context.getRequest());
        if (this.form.validate()) {
            MailSettingsAction.logger.debug("Applying settings to app configuration");
            try {
                final MailSettingsForm bean = new MailSettingsForm();
                this.form.writeToObject((Object)bean);
                this.configuration.setProperty("mail.enabled", (Object)bean.mailEnabled);
                this.configuration.setProperty("mail.keep.sent", (Object)bean.keepSent);
                this.configuration.setProperty("mail.queue.location", (Object)bean.queueLocation);
                this.configuration.setProperty("mail.smtp.host", (Object)bean.smtpHost);
                this.configuration.setProperty("mail.smtp.port", (Object)bean.smtpPort);
                this.configuration.setProperty("mail.smtp.ssl.enabled", (Object)bean.smtpSSL);
                this.configuration.setProperty("mail.smtp.tls.enabled", (Object)bean.smtpTLS);
                this.configuration.setProperty("mail.smtp.login", (Object)bean.smtpLogin);
                this.configuration.setProperty("mail.smtp.password", (Object)bean.smtpPassword);
                CommonsConfigurationUtils.save(this.configuration);
                MailSettingsAction.logger.info("Configuration saved");
            }
            catch (Exception e) {
                MailSettingsAction.logger.error("Configuration not saved", (Throwable)e);
                SessionMessages.addErrorMessage(ElementsThreadLocals.getText("the.configuration.could.not.be.saved", new Object[0]));
                return (Resolution)new ForwardResolution("/m/admin/mail/settings.jsp");
            }
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("configuration.updated.successfully", new Object[0]));
            return (Resolution)new RedirectResolution((Class)this.getClass());
        }
        return (Resolution)new ForwardResolution("/m/admin/mail/settings.jsp");
    }
    
    @Button(list = "settings", key = "return.to.pages", order = 2.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    private void setupFormAndBean() {
        this.form = new FormBuilder((Class)MailSettingsForm.class).build();
        final MailSettingsForm bean = new MailSettingsForm();
        bean.mailEnabled = this.configuration.getBoolean("mail.enabled", false);
        bean.keepSent = this.configuration.getBoolean("mail.keep.sent", false);
        bean.smtpHost = this.configuration.getString("mail.smtp.host");
        bean.smtpPort = this.configuration.getInteger("mail.smtp.port", (Integer)null);
        bean.smtpSSL = this.configuration.getBoolean("mail.smtp.ssl.enabled", false);
        bean.smtpTLS = this.configuration.getBoolean("mail.smtp.tls.enabled", false);
        bean.smtpLogin = this.configuration.getString("mail.smtp.login");
        bean.smtpPassword = this.configuration.getString("mail.smtp.password");
        bean.queueLocation = this.configuration.getProperty("mail.queue.location") + "";
        this.form.readFromObject((Object)bean);
    }
    
    public Form getForm() {
        return this.form;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)MailSettingsAction.class);
    }
    
    public static class MailSettingsForm
    {
        @Required
        @FieldSet("General")
        public boolean mailEnabled;
        @FieldSet("General")
        @Label("Keep sent messages")
        public boolean keepSent;
        @Required
        @FieldSet("General")
        @Label("Queue location")
        @FieldSize(100)
        public String queueLocation;
        @FieldSet("SMTP")
        @Label("Host")
        public String smtpHost;
        @FieldSet("SMTP")
        @Label("Port")
        public Integer smtpPort;
        @Required
        @FieldSet("SMTP")
        @Label("SSL enabled")
        public boolean smtpSSL;
        @FieldSet("SMTP")
        @Label("TLS enabled")
        public boolean smtpTLS;
        @FieldSet("SMTP")
        @Label("Login")
        public String smtpLogin;
        @FieldSet("SMTP")
        @Password
        @Label("Password")
        public String smtpPassword;
    }
}
