package org.remata.portofolio.actions.admin.page.forms;

import org.remata.elements.annotations.*;

public class CopyPage
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    @RegExp(value = "[a-zA-Z0-9][a-zA-Z0-9_\\-]*", errorMessage = "invalid.fragment.only.letters.numbers.etc.are.allowed")
    @Required
    @Label("Fragment")
    public String fragment;
    @Required
    @Label("Copy to")
    public String destinationPagePath;
}
