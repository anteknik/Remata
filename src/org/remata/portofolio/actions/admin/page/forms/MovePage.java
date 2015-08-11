package org.remata.portofolio.actions.admin.page.forms;

import org.remata.elements.annotations.*;

public class MovePage
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    @Required
    @LabelI18N("where.to.move")
    public String destinationPagePath;
}
