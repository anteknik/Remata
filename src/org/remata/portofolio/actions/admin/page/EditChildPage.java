package org.remata.portofolio.actions.admin.page;

import org.remata.elements.annotations.*;

public class EditChildPage
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    @Updatable(false)
    @Insertable(false)
    public String name;
    public boolean active;
    @LabelI18N("embed.in.parent")
    public boolean embedded;
    @LabelI18N("show.in.navigation")
    public boolean showInNavigation;
    @Updatable(false)
    @Insertable(false)
    public String title;
}
