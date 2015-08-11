package org.remata.portofolio.actions.admin.page;

import org.remata.portofolio.security.*;
import java.io.*;
import org.remata.portofolio.di.*;
import org.remata.portofolio.actions.safemode.*;
import org.remata.portofolio.dispatcher.*;
import org.remata.portofolio.pages.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.buttons.annotations.*;
import org.remata.portofolio.actions.admin.*;
import org.slf4j.*;

@RequiresAdministrator
public abstract class RootConfigurationAction extends PageAdminAction
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final Logger logger;
    @Inject("PAGES_DIRECTORY")
    public File pagesDir;
    
    @Before
    @Override
    public Resolution prepare() {
        this.originalPath = "/";
        final File rootDir = this.pagesDir;
        Page rootPage;
        try {
            rootPage = DispatcherLogic.getPage(rootDir);
        }
        catch (Exception e) {
            throw new Error("Couldn't load root page", e);
        }
        this.pageInstance = new PageInstance((PageInstance)null, rootDir, rootPage, (Class)SafeModeAction.class);
        this.dispatch = new Dispatch(new PageInstance[] { this.pageInstance });
        return null;
    }
    
    @Buttons({ @Button(list = "root-permissions", key = "return.to.pages", order = 2.0), @Button(list = "root-children", key = "return.to.pages", order = 2.0) })
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)SettingsAction.class);
    }
}
