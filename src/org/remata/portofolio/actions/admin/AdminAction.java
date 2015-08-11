package org.remata.portofolio.actions.admin;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.remata.portofolio.di.*;
import org.remata.portofolio.menu.*;
import java.util.*;
import net.sourceforge.stripes.action.*;
import org.slf4j.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin")
public class AdminAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin";
    @Inject("org.remata.portofolio.menu.Menu.admin")
    MenuBuilder adminMenu;
    public static final Logger logger;
    
    @DefaultHandler
    public Resolution execute() {
        final Menu menu = this.adminMenu.build();
        for (final MenuItem item : menu.items) {
            if (item instanceof MenuLink) {
                return (Resolution)new RedirectResolution(((MenuLink)item).link);
            }
            if (!(item instanceof MenuGroup)) {
                continue;
            }
            final List<MenuLink> menuLinks = (List<MenuLink>)((MenuGroup)item).menuLinks;
            if (!menuLinks.isEmpty()) {
                return (Resolution)new RedirectResolution(menuLinks.get(0).link);
            }
        }
        throw new Error("BUG! There should be at least one registered admin menu item!");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)AdminAction.class);
    }
}
