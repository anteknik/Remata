package org.remata.portofolio.actions.admin.servletcontext;

import org.remata.portofolio.stripes.*;
import org.remata.portofolio.security.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.elements.ognl.*;
import org.apache.commons.lang.*;
import org.remata.elements.forms.*;
import org.remata.elements.*;
import javax.servlet.*;
import java.util.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;
import org.slf4j.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/servlet-context")
public class ServletContextAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/servlet-context";
    TableForm form;
    public static final Logger logger;
    
    @DefaultHandler
    @RequiresPermissions({ "org.remata.portofolio.servletcontext:list" })
    public Resolution execute() {
        this.setupForm();
        return (Resolution)new ForwardResolution("/m/admin/servletcontext/list.jsp");
    }
    
    protected void setupForm() {
        final ServletContext servletContext = this.context.getServletContext();
        final Enumeration<String> attributeNames = (Enumeration<String>)servletContext.getAttributeNames();
        final List<KeyValue> attributes = new ArrayList<KeyValue>();
        while (attributeNames.hasMoreElements()) {
            final String key = attributeNames.nextElement();
            final String value = StringUtils.abbreviate(OgnlUtils.convertValueToString(servletContext.getAttribute(key)), 300);
            attributes.add(new KeyValue(key, value));
        }
        final TableFormBuilder builder = new TableFormBuilder((Class)KeyValue.class);
        builder.configNRows(attributes.size());
        builder.configMode(Mode.VIEW);
        (this.form = builder.build()).readFromObject((Object)attributes);
    }
    
    @Button(list = "modules", key = "return.to.pages", order = 2.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    public TableForm getForm() {
        return this.form;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)ServletContextAction.class);
    }
    
    public static class KeyValue
    {
        public String key;
        public String value;
        
        public KeyValue(final String key, final String value) {
            this.key = key;
            this.value = value;
        }
    }
}
