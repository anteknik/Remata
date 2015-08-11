package org.remata.portofolio.actions.admin.page;

import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.di.*;
import org.apache.shiro.*;
import org.remata.portofolio.logic.*;
import org.remata.portofolio.stripes.*;
import org.remata.portofolio.buttons.annotations.*;
import org.remata.elements.*;
import org.remata.elements.messages.*;
import javax.servlet.http.*;
import net.sourceforge.stripes.action.*;
import org.remata.elements.text.*;
import org.apache.commons.beanutils.*;
import org.remata.portofolio.dispatcher.*;
import org.remata.elements.util.*;
import org.remata.portofolio.scripting.*;
import org.remata.portofolio.pageactions.registry.*;
import ognl.*;
import org.apache.commons.io.*;
import org.apache.commons.lang.*;
import org.remata.portofolio.actions.admin.page.forms.*;
import org.remata.elements.options.*;
import org.remata.elements.fields.*;
import org.remata.portofolio.pageactions.*;
import java.io.*;
import org.remata.elements.forms.*;
import org.remata.portofolio.shiro.*;
import java.util.*;
import org.apache.shiro.subject.*;
import org.apache.shiro.mgt.*;
import org.remata.portofolio.security.*;
import net.sourceforge.stripes.util.*;
import org.remata.portofolio.pages.*;
import org.slf4j.*;

@RequiresAuthentication
@UrlBinding("/actions/admin/page")
public class PageAdminAction extends AbstractPageAction
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    protected String originalPath;
    public Dispatch dispatch;
    private static final Logger logger;
    @Inject("PAGES_DIRECTORY")
    public File pagesDir;
    @Inject("org.remata.portofolio.pageactions.registry.PageActionRegistry")
    public PageActionRegistry registry;
    protected static final String[][] NEW_PAGE_SETUP_FIELDS;
    protected Form newPageForm;
    protected String title;
    protected List<EditChildPage> childPages;
    protected List<EditChildPage> detailChildPages;
    protected TableForm childPagesForm;
    protected TableForm detailChildPagesForm;
    protected Set<String> groups;
    protected Map<String, String> accessLevels;
    protected Map<String, List<String>> permissions;
    protected String testUserId;
    protected Map<Serializable, String> users;
    protected AccessLevel testedAccessLevel;
    protected Set<String> testedPermissions;
    protected Form moveForm;
    protected Form copyForm;
    
    public PageAdminAction() {
        this.childPages = new ArrayList<EditChildPage>();
        this.detailChildPages = new ArrayList<EditChildPage>();
        this.accessLevels = new HashMap<String, String>();
        this.permissions = new HashMap<String, List<String>>();
    }
    
    public Resolution preparePage() {
        return null;
    }
    
    @Before
    public Resolution prepare() {
        final Dispatcher dispatcher = DispatcherUtil.get(this.context.getRequest());
        this.dispatch = dispatcher.getDispatch(this.originalPath);
        this.pageInstance = this.dispatch.getLastPageInstance();
        if (!SecurityLogic.hasPermissions(this.portofinoConfiguration, this.pageInstance, SecurityUtils.getSubject(), AccessLevel.EDIT, new String[0])) {
            return (Resolution)new ForbiddenAccessResolution();
        }
        return null;
    }
    
    @Buttons({ @Button(list = "page-children-edit", key = "cancel", order = 99.0), @Button(list = "page-permissions-edit", key = "cancel", order = 99.0), @Button(list = "page-create", key = "cancel", order = 99.0) })
    public Resolution cancel() {
        return (Resolution)new RedirectResolution(this.originalPath);
    }
    
    public Resolution updateLayout() {
        if (!this.checkPermissionsOnTargetPage(this.getPageInstance())) {
            return (Resolution)new ForbiddenAccessResolution("You are not authorized to edit the layout of this page.");
        }
        final HttpServletRequest request = this.context.getRequest();
        final Enumeration parameters = request.getParameterNames();
        while (parameters.hasMoreElements()) {
            final String parameter = parameters.nextElement();
            if (parameter.startsWith("embeddedPageAction_")) {
                final String layoutContainer = parameter.substring("embeddedPageAction_".length());
                final String[] embeddedPageActionIds = request.getParameterValues(parameter);
                this.updateLayout(layoutContainer, embeddedPageActionIds);
            }
        }
        try {
            DispatcherLogic.savePage(this.getPageInstance());
        }
        catch (Exception e) {
            PageAdminAction.logger.error("Error updating layout", (Throwable)e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("layout.update.failed", new Object[0]));
        }
        return (Resolution)new RedirectResolution(this.originalPath);
    }
    
    protected void updateLayout(final String layoutContainer, final String[] embeddedPageActionIds) {
        final PageInstance instance = this.getPageInstance();
        Layout layout = instance.getLayout();
        if (layout == null) {
            layout = new Layout();
            instance.setLayout(layout);
        }
        for (int i = 0; i < embeddedPageActionIds.length; ++i) {
            final String pageFragment = embeddedPageActionIds[i];
            for (final ChildPage p : layout.getChildPages()) {
                if (pageFragment.equals(p.getName())) {
                    p.setContainer(layoutContainer);
                    p.setOrder(i + "");
                }
            }
        }
    }
    
    public Resolution newPage() throws Exception {
        this.prepareNewPageForm();
        return (Resolution)new ForwardResolution("/m/admin/page/new-page.jsp");
    }
    
    @Button(list = "page-create", key = "create.new", order = 1.0, type = " btn-primary ")
    public Resolution createPage() {
        try {
            return this.doCreateNewPage();
        }
        catch (Exception e) {
            PageAdminAction.logger.error("Error creating page", (Throwable)e);
            final String msg = ElementsThreadLocals.getText("error.creating.page._", new Object[] { e.getMessage() });
            SessionMessages.addErrorMessage(msg);
            return (Resolution)new ForwardResolution("/m/admin/page/new-page.jsp");
        }
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(final String title) {
        this.title = title;
    }
    
    private Resolution doCreateNewPage() throws Exception {
        this.prepareNewPageForm();
        if (!this.newPageForm.validate()) {
            return (Resolution)new ForwardResolution("/m/admin/page/new-page.jsp");
        }
        final NewPage newPage = new NewPage();
        this.newPageForm.writeToObject((Object)newPage);
        final InsertPosition insertPosition = InsertPosition.valueOf(newPage.getInsertPositionName());
        final String pageClassName = newPage.getActionClassName();
        final ClassLoader classLoader = (ClassLoader)this.context.getServletContext().getAttribute("org.remata.portofolio.application.classLoader");
        final Class actionClass = Class.forName(pageClassName, true, classLoader);
        final PageActionInfo info = this.registry.getInfo(actionClass);
        final String scriptTemplate = info.scriptTemplate;
        final Class<?> configurationClass = (Class<?>)info.configurationClass;
        final boolean supportsDetail = info.supportsDetail;
        String className;
        final String pageId = className = RandomUtil.createRandomId();
        if (Character.isDigit(className.charAt(0))) {
            className = "_" + className;
        }
        final OgnlContext ognlContext = ElementsThreadLocals.getOgnlContext();
        ognlContext.put((Object)"generatedClassName", (Object)className);
        ognlContext.put((Object)"pageClassName", (Object)pageClassName);
        final String script = OgnlTextFormat.format(scriptTemplate, (Object)this);
        final Page page = new Page();
        BeanUtils.copyProperties((Object)page, (Object)newPage);
        page.setId(pageId);
        Object configuration = null;
        if (configurationClass != null) {
            configuration = ReflectionUtil.newInstance((Class)configurationClass);
            if (configuration instanceof ConfigurationWithDefaults) {
                ((ConfigurationWithDefaults)configuration).setupDefaults();
            }
        }
        page.init();
        final String fragment = newPage.getFragment();
        File parentDirectory = null;
        File directory = null;
        Page parentPage = null;
        Layout parentLayout = null;
        String configurePath = null;
        PageInstance parentPageInstance = null;
        switch (insertPosition) {
            case TOP: {
                parentDirectory = this.pagesDir;
                directory = new File(parentDirectory, fragment);
                parentPage = DispatcherLogic.getPage(parentDirectory);
                parentLayout = parentPage.getLayout();
                configurePath = "";
                parentPageInstance = new PageInstance((PageInstance)null, parentDirectory, parentPage, (Class)null);
                break;
            }
            case CHILD: {
                parentPageInstance = this.getPageInstance();
                parentPage = parentPageInstance.getPage();
                parentLayout = parentPageInstance.getLayout();
                parentDirectory = parentPageInstance.getDirectory();
                directory = parentPageInstance.getChildPageDirectory(fragment);
                configurePath = this.originalPath;
                break;
            }
            case SIBLING: {
                parentPageInstance = this.dispatch.getPageInstance(-2);
                parentPage = parentPageInstance.getPage();
                parentLayout = parentPageInstance.getLayout();
                parentDirectory = parentPageInstance.getDirectory();
                directory = parentPageInstance.getChildPageDirectory(fragment);
                configurePath = parentPageInstance.getPath();
                break;
            }
            default: {
                throw new IllegalStateException("Don't know how to add page " + page + " at position " + insertPosition);
            }
        }
        if (!this.checkPermissionsOnTargetPage(parentPageInstance)) {
            return (Resolution)new ForbiddenAccessResolution("You are not authorized to create a new page here.");
        }
        if (directory.exists()) {
            PageAdminAction.logger.error("Can't create page - directory {} exists", (Object)directory.getAbsolutePath());
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("error.creating.page.the.directory.already.exists", new Object[0]));
            return (Resolution)new ForwardResolution("/m/admin/page/new-page.jsp");
        }
        Label_0796: {
            if (ElementsFileUtils.safeMkdirs(directory)) {
                Label_0833: {
                    try {
                        page.getLayout().setTemplate(parentPage.getLayout().getTemplate());
                        page.getDetailLayout().setTemplate(parentPage.getDetailLayout().getTemplate());
                        PageAdminAction.logger.debug("Creating the new child page in directory: {}", (Object)directory);
                        DispatcherLogic.savePage(directory, page);
                        if (configuration != null) {
                            DispatcherLogic.saveConfiguration(directory, configuration);
                        }
                        final File groovyScriptFile = ScriptingUtil.getGroovyScriptFile(directory, "action");
                        FileWriter fw = null;
                        try {
                            fw = new FileWriter(groovyScriptFile);
                            fw.write(script);
                        }
                        finally {
                            IOUtils.closeQuietly((Writer)fw);
                        }
                        PageAdminAction.logger.debug("Registering the new child page in parent page (directory: {})", (Object)parentDirectory);
                        final ChildPage childPage = new ChildPage();
                        childPage.setName(directory.getName());
                        childPage.setShowInNavigation(true);
                        parentLayout.getChildPages().add(childPage);
                        if (supportsDetail) {
                            final File detailDir = new File(directory, "_detail");
                            PageAdminAction.logger.debug("Creating _detail directory: {}", (Object)detailDir);
                            if (!ElementsFileUtils.safeMkdir(detailDir)) {
                                PageAdminAction.logger.warn("Couldn't create detail directory {}", (Object)detailDir);
                            }
                        }
                        DispatcherLogic.savePage(parentDirectory, parentPage);
                        break Label_0833;
                    }
                    catch (Exception e) {
                        PageAdminAction.logger.error("Exception saving page configuration");
                        SessionMessages.addErrorMessage(ElementsThreadLocals.getText("error.creating.page._", new Object[0]));
                        return (Resolution)new ForwardResolution("/m/admin/page/new-page.jsp");
                    }
                    break Label_0796;
                }
                PageAdminAction.logger.info("Page " + pageId + " created. Path: " + directory.getAbsolutePath());
                SessionMessages.addInfoMessage(ElementsThreadLocals.getText("page.created.successfully.you.should.now.configure.it", new Object[0]));
                final String url = this.context.getRequest().getContextPath() + configurePath + "/" + fragment;
                return (Resolution)((RedirectResolution)new RedirectResolution(url, false).addParameter("configure", new Object[0])).addParameter("returnUrl", new Object[] { url });
            }
        }
        PageAdminAction.logger.error("Can't create directory {}", (Object)directory.getAbsolutePath());
        SessionMessages.addErrorMessage(ElementsThreadLocals.getText("error.creating.page.the.directory.could.not.be.created", new Object[0]));
        return (Resolution)new ForwardResolution("/m/admin/page/new-page.jsp");
    }
    
    protected boolean checkPermissionsOnTargetPage(final PageInstance targetPageInstance) {
        return this.checkPermissionsOnTargetPage(targetPageInstance, AccessLevel.EDIT);
    }
    
    protected boolean checkPermissionsOnTargetPage(final PageInstance targetPageInstance, final AccessLevel accessLevel) {
        final Subject subject = SecurityUtils.getSubject();
        if (!SecurityLogic.hasPermissions(this.portofinoConfiguration, targetPageInstance, subject, accessLevel, new String[0])) {
            PageAdminAction.logger.warn("User not authorized modify page {}", (Object)targetPageInstance);
            return false;
        }
        return true;
    }
    
    public Resolution deletePage() {
        final PageInstance pageInstance = this.getPageInstance();
        final PageInstance parentPageInstance = pageInstance.getParent();
        if (parentPageInstance == null) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("you.cant.delete.the.root.page", new Object[0]));
            return (Resolution)new RedirectResolution(this.originalPath);
        }
        if (!this.checkPermissionsOnTargetPage(parentPageInstance)) {
            return (Resolution)new ForbiddenAccessResolution("You are not authorized to delete this page.");
        }
        final Dispatcher dispatcher = DispatcherUtil.get(this.context.getRequest());
        final String contextPath = this.context.getRequest().getContextPath();
        final String landingPagePath = this.portofinoConfiguration.getString("landing.page");
        final Dispatch landingPageDispatch = dispatcher.getDispatch(landingPagePath);
        if (landingPageDispatch != null && landingPageDispatch.getLastPageInstance().getDirectory().equals(pageInstance.getDirectory())) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("you.cant.delete.the.landing.page", new Object[0]));
            return (Resolution)new RedirectResolution(this.originalPath);
        }
        try {
            final String pageName = pageInstance.getName();
            final File childPageDirectory = parentPageInstance.getChildPageDirectory(pageName);
            final Layout parentLayout = parentPageInstance.getLayout();
            final Iterator<ChildPage> it = parentLayout.getChildPages().iterator();
            while (it.hasNext()) {
                if (pageName.equals(it.next().getName())) {
                    it.remove();
                    DispatcherLogic.savePage(parentPageInstance.getDirectory(), parentPageInstance.getPage());
                    break;
                }
            }
            FileUtils.deleteDirectory(childPageDirectory);
        }
        catch (Exception e) {
            PageAdminAction.logger.error("Error deleting page", (Throwable)e);
        }
        return (Resolution)new RedirectResolution(StringUtils.defaultIfEmpty(parentPageInstance.getPath(), "/"));
    }
    
    public Page getPage() {
        return this.getPageInstance().getPage();
    }
    
    public PageInstance getPageInstance() {
        return this.dispatch.getLastPageInstance();
    }
    
    public Resolution movePage() {
        this.buildMovePageForm();
        this.moveForm.readFromRequest(this.context.getRequest());
        if (this.moveForm.validate()) {
            final MovePage p = new MovePage();
            this.moveForm.writeToObject((Object)p);
            return this.copyPage(p.destinationPagePath, null, true);
        }
        final Field field = (Field)((FieldSet)this.moveForm.get(0)).get(0);
        if (!field.getErrors().isEmpty()) {
            SessionMessages.addErrorMessage(field.getLabel() + ": " + field.getErrors().get(0));
        }
        return (Resolution)new RedirectResolution(this.originalPath);
    }
    
    public Resolution copyPage() {
        this.buildCopyPageForm();
        this.copyForm.readFromRequest(this.context.getRequest());
        if (this.copyForm.validate()) {
            final CopyPage p = new CopyPage();
            this.copyForm.writeToObject((Object)p);
            return this.copyPage(p.destinationPagePath, p.fragment, false);
        }
        Field field = (Field)((FieldSet)this.copyForm.get(0)).get(0);
        if (!field.getErrors().isEmpty()) {
            SessionMessages.addErrorMessage(field.getLabel() + ": " + field.getErrors().get(0));
        }
        field = (Field)((FieldSet)this.copyForm.get(0)).get(1);
        if (!field.getErrors().isEmpty()) {
            SessionMessages.addErrorMessage(field.getLabel() + ": " + field.getErrors().get(0));
        }
        return (Resolution)new RedirectResolution(this.originalPath);
    }
    
    protected Resolution copyPage(final String destinationPagePath, String newName, final boolean deleteOriginal) {
        if (StringUtils.isEmpty(destinationPagePath)) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("you.must.select.a.destination", new Object[0]));
            return (Resolution)new RedirectResolution(this.originalPath);
        }
        final PageInstance pageInstance = this.getPageInstance();
        final PageInstance oldParent = pageInstance.getParent();
        if (oldParent == null) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("you.cant.copy.or.move.the.root.page", new Object[0]));
            return (Resolution)new RedirectResolution(this.originalPath);
        }
        if (deleteOriginal) {
            PageAdminAction.logger.debug("Checking if we've been asked to move the landing page...");
            final Dispatcher dispatcher = DispatcherUtil.get(this.context.getRequest());
            final String contextPath = this.context.getRequest().getContextPath();
            final String landingPagePath = this.portofinoConfiguration.getString("landing.page");
            final Dispatch landingPageDispatch = dispatcher.getDispatch(landingPagePath);
            if (landingPageDispatch != null && landingPageDispatch.getLastPageInstance().getDirectory().equals(pageInstance.getDirectory())) {
                SessionMessages.addErrorMessage(ElementsThreadLocals.getText("you.cant.move.the.landing.page", new Object[0]));
                return (Resolution)new RedirectResolution(this.originalPath);
            }
        }
        PageInstance newParent;
        if ("/".equals(destinationPagePath)) {
            final File dir = this.pagesDir;
            try {
                newParent = new PageInstance((PageInstance)null, dir, DispatcherLogic.getPage(dir), (Class)null);
            }
            catch (Exception e) {
                throw new Error("Couldn't load root page", e);
            }
        }
        else {
            final Dispatcher dispatcher2 = DispatcherUtil.get(this.context.getRequest());
            final Dispatch destinationDispatch = dispatcher2.getDispatch(destinationPagePath);
            newParent = destinationDispatch.getLastPageInstance();
        }
        if (newParent.getDirectory().equals(oldParent.getDirectory())) {
            final List<String> params = (List<String>)newParent.getParameters();
            newParent = new PageInstance(newParent.getParent(), newParent.getDirectory(), oldParent.getPage(), (Class)null);
            newParent.getParameters().addAll(params);
        }
        if (!this.checkPermissionsOnTargetPage(newParent)) {
            return (Resolution)new ForbiddenAccessResolution(ElementsThreadLocals.getText("you.dont.have.edit.access.level.on.the.destination.page", new Object[0]));
        }
        if (newParent == null) {
            final String msg = ElementsThreadLocals.getText("invalid.destination._", new Object[] { destinationPagePath });
            SessionMessages.addErrorMessage(msg);
            return (Resolution)new RedirectResolution(this.originalPath);
        }
        newName = (StringUtils.isEmpty(newName) ? pageInstance.getName() : newName);
        final File newDirectory = newParent.getChildPageDirectory(newName);
        final File newParentDirectory = newDirectory.getParentFile();
        PageAdminAction.logger.debug("Ensuring that new parent directory {} exists", (Object)newParentDirectory);
        ElementsFileUtils.safeMkdirs(newParentDirectory);
        if (newDirectory.exists()) {
            final String msg2 = ElementsThreadLocals.getText("destination.is.an.existing.file/directory._", new Object[] { newDirectory.getAbsolutePath() });
            SessionMessages.addErrorMessage(msg2);
            return (Resolution)new RedirectResolution(this.originalPath);
        }
        try {
            final Iterator<ChildPage> it = oldParent.getLayout().getChildPages().iterator();
            ChildPage oldChildPage = null;
            final String oldName = pageInstance.getName();
            while (it.hasNext()) {
                oldChildPage = it.next();
                if (oldChildPage.getName().equals(oldName)) {
                    if (deleteOriginal) {
                        it.remove();
                        break;
                    }
                    break;
                }
            }
            if (deleteOriginal) {
                PageAdminAction.logger.debug("Removing from old parent");
                DispatcherLogic.savePage(oldParent.getDirectory(), oldParent.getPage());
                PageAdminAction.logger.debug("Moving directory");
                FileUtils.moveDirectory(pageInstance.getDirectory(), newDirectory);
            }
            else {
                PageAdminAction.logger.debug("Copying directory");
                FileUtils.copyDirectory(pageInstance.getDirectory(), newDirectory);
                PageAdminAction.logger.debug("Generating a new Id for the new page");
                final Page newPage = DispatcherLogic.getPage(newDirectory);
                final String pageId = RandomUtil.createRandomId();
                newPage.setId(pageId);
                DispatcherLogic.savePage(newDirectory, newPage);
            }
            PageAdminAction.logger.debug("Registering the new child page in parent page (directory: {})", (Object)newDirectory);
            final ChildPage newChildPage = new ChildPage();
            newChildPage.setName(newName);
            if (oldChildPage != null) {
                newChildPage.setShowInNavigation(oldChildPage.isShowInNavigation());
            }
            else {
                newChildPage.setShowInNavigation(true);
            }
            newParent.getLayout().getChildPages().add(newChildPage);
            DispatcherLogic.savePage(newParent.getDirectory(), newParent.getPage());
        }
        catch (Exception e2) {
            PageAdminAction.logger.error("Couldn't copy/move page", (Throwable)e2);
            final String msg3 = ElementsThreadLocals.getText("page.copyOrMove.failed", new Object[] { destinationPagePath });
            SessionMessages.addErrorMessage(msg3);
        }
        if (newParent.getParameters().isEmpty()) {
            return (Resolution)new RedirectResolution(destinationPagePath + (destinationPagePath.endsWith("/") ? "" : "/") + newName);
        }
        newParent.getParameters().clear();
        return (Resolution)new RedirectResolution(newParent.getPath());
    }
    
    private void prepareNewPageForm() throws Exception {
        final DefaultSelectionProvider classSelectionProvider = new DefaultSelectionProvider("actionClassName");
        for (final PageActionInfo info : this.registry) {
            classSelectionProvider.appendRow((Object)info.actionClass.getName(), info.description, true);
        }
        final Subject subject = SecurityUtils.getSubject();
        final boolean includeSiblingOption = this.dispatch.getPageInstancePath().length > 2 && SecurityLogic.hasPermissions(this.portofinoConfiguration, this.dispatch.getPageInstance(-2), subject, AccessLevel.EDIT, new String[0]);
        final List<String[]> insertPositions = new ArrayList<String[]>();
        final File rootDirectory = this.pagesDir;
        final Page rootPage = DispatcherLogic.getPage(rootDirectory);
        final PageInstance rootPageInstance = new PageInstance((PageInstance)null, rootDirectory, rootPage, (Class)null);
        if (SecurityLogic.hasPermissions(this.portofinoConfiguration, rootPageInstance, subject, AccessLevel.EDIT, new String[0])) {
            insertPositions.add(new String[] { InsertPosition.TOP.name(), "at the top level" });
        }
        insertPositions.add(new String[] { InsertPosition.CHILD.name(), "as a child of " + this.getPage().getTitle() });
        if (includeSiblingOption) {
            insertPositions.add(new String[] { InsertPosition.SIBLING.name(), "as a sibling of " + this.getPage().getTitle() });
        }
        final DefaultSelectionProvider insertPositionSelectionProvider = new DefaultSelectionProvider("insertPositionName");
        for (final String[] posAndLabel : insertPositions) {
            insertPositionSelectionProvider.appendRow((Object)posAndLabel[0], posAndLabel[1], true);
        }
        this.newPageForm = new FormBuilder((Class)NewPage.class).configFields(PageAdminAction.NEW_PAGE_SETUP_FIELDS).configFieldSetNames(new String[] { "Page setup" }).configSelectionProvider((SelectionProvider)classSelectionProvider, new String[] { "actionClassName" }).configSelectionProvider((SelectionProvider)insertPositionSelectionProvider, new String[] { "insertPositionName" }).build();
        ((SelectField)this.newPageForm.findFieldByPropertyName("insertPositionName")).setValue((Object)InsertPosition.CHILD.name());
        this.newPageForm.readFromRequest(this.context.getRequest());
    }
    
    public Resolution pageChildren() {
        this.setupChildPages();
        return this.forwardToPageChildren();
    }
    
    protected void setupChildPages() {
        final File directory = this.getPageInstance().getDirectory();
        this.childPagesForm = this.setupChildPagesForm(this.childPages, directory, this.getPage().getLayout(), "");
        if (PageActionLogic.supportsDetail(this.getPageInstance().getActionClass())) {
            final File detailDirectory = new File(directory, "_detail");
            if (!detailDirectory.isDirectory() && !ElementsFileUtils.safeMkdir(detailDirectory)) {
                PageAdminAction.logger.error("Could not create detail directory{}", (Object)detailDirectory.getAbsolutePath());
                SessionMessages.addErrorMessage("Could not create detail directory");
                return;
            }
            this.detailChildPagesForm = this.setupChildPagesForm(this.detailChildPages, detailDirectory, this.getPage().getDetailLayout(), "detail");
        }
    }
    
    protected TableForm setupChildPagesForm(final List<EditChildPage> childPages, final File childrenDirectory, final Layout layout, final String prefix) {
        final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }
        };
        final List<EditChildPage> unorderedChildPages = new ArrayList<EditChildPage>();
        for (final File dir : childrenDirectory.listFiles(filter)) {
            if (!"_detail".equals(dir.getName())) {
                EditChildPage childPage = null;
                String title;
                try {
                    final Page page = DispatcherLogic.getPage(dir);
                    title = page.getTitle();
                }
                catch (Exception e) {
                    PageAdminAction.logger.error("Couldn't load page for " + dir, (Throwable)e);
                    title = null;
                }
                for (final ChildPage cp : layout.getChildPages()) {
                    if (cp.getName().equals(dir.getName())) {
                        childPage = new EditChildPage();
                        childPage.active = true;
                        childPage.name = cp.getName();
                        childPage.showInNavigation = cp.isShowInNavigation();
                        childPage.title = title;
                        childPage.embedded = (cp.getContainer() != null);
                        break;
                    }
                }
                if (childPage == null) {
                    childPage = new EditChildPage();
                    childPage.active = false;
                    childPage.name = dir.getName();
                    childPage.title = title;
                }
                unorderedChildPages.add(childPage);
            }
        }
        PageAdminAction.logger.debug("Adding known pages in order");
        for (final ChildPage cp2 : layout.getChildPages()) {
            for (final EditChildPage ecp : unorderedChildPages) {
                if (cp2.getName().equals(ecp.name)) {
                    childPages.add(ecp);
                    break;
                }
            }
        }
        PageAdminAction.logger.debug("Adding unknown pages");
        for (final EditChildPage ecp2 : unorderedChildPages) {
            if (!ecp2.active) {
                childPages.add(ecp2);
            }
        }
        final TableForm childPagesForm = new TableFormBuilder((Class)EditChildPage.class).configNRows(childPages.size()).configFields(this.getChildPagesFormFields()).configPrefix(prefix).build();
        childPagesForm.readFromObject((Object)childPages);
        return childPagesForm;
    }
    
    protected String[] getChildPagesFormFields() {
        return new String[] { "active", "name", "title", "showInNavigation", "embedded" };
    }
    
    @Button(list = "page-children-edit", key = "update", order = 1.0, type = " btn-primary ")
    public Resolution updatePageChildren() {
        if (!this.checkPermissionsOnTargetPage(this.getPageInstance())) {
            return (Resolution)new ForbiddenAccessResolution(ElementsThreadLocals.getText("you.dont.have.edit.access.level.on.the.destination.page", new Object[0]));
        }
        this.setupChildPages();
        String[] order = this.context.getRequest().getParameterValues("directChildren");
        final boolean success = this.updatePageChildren(this.childPagesForm, this.childPages, this.getPage().getLayout(), order);
        this.childPages.clear();
        if (success && this.detailChildPagesForm != null) {
            order = this.context.getRequest().getParameterValues("detailChildren");
            this.updatePageChildren(this.detailChildPagesForm, this.detailChildPages, this.getPage().getDetailLayout(), order);
            this.detailChildPages.clear();
        }
        this.setupChildPages();
        SessionMessages.addInfoMessage(ElementsThreadLocals.getText("object.updated.successfully", new Object[0]));
        return this.forwardToPageChildren();
    }
    
    protected boolean updatePageChildren(final TableForm childPagesForm, final List<EditChildPage> childPages, final Layout layout, final String[] order) {
        childPagesForm.readFromRequest(this.context.getRequest());
        if (!childPagesForm.validate()) {
            return false;
        }
        childPagesForm.writeToObject((Object)childPages);
        final List<ChildPage> newChildren = new ArrayList<ChildPage>();
        for (final EditChildPage editChildPage : childPages) {
            if (!editChildPage.active) {
                continue;
            }
            ChildPage childPage = null;
            for (final ChildPage cp : layout.getChildPages()) {
                if (cp.getName().equals(editChildPage.name)) {
                    childPage = cp;
                    break;
                }
            }
            if (childPage == null) {
                childPage = new ChildPage();
                childPage.setName(editChildPage.name);
            }
            childPage.setShowInNavigation(editChildPage.showInNavigation);
            if (editChildPage.embedded) {
                if (childPage.getContainer() == null) {
                    childPage.setContainer("default");
                    childPage.setOrder("0");
                }
            }
            else {
                childPage.setContainer((String)null);
                childPage.setOrder((String)null);
            }
            newChildren.add(childPage);
            if (editChildPage.showInNavigation || editChildPage.embedded) {
                continue;
            }
            final String msg = ElementsThreadLocals.getText("the.page._.is.not.embedded.and.not.included.in.navigation", new Object[] { editChildPage.name });
            SessionMessages.addWarningMessage(msg);
        }
        final List<ChildPage> sortedChildren = new ArrayList<ChildPage>();
        if (order == null) {
            sortedChildren.addAll(newChildren);
        }
        else {
            for (final String name : order) {
                for (final ChildPage p : newChildren) {
                    if (name.equals(p.getName())) {
                        sortedChildren.add(p);
                        break;
                    }
                }
            }
        }
        layout.getChildPages().clear();
        layout.getChildPages().addAll(sortedChildren);
        try {
            DispatcherLogic.savePage(this.getPageInstance());
        }
        catch (Exception e) {
            PageAdminAction.logger.error("Couldn't save page", (Throwable)e);
            final String msg2 = ElementsThreadLocals.getText("error.updating.page._", new Object[] { e.getMessage() });
            SessionMessages.addErrorMessage(msg2);
            return false;
        }
        return true;
    }
    
    public List<EditChildPage> getChildPages() {
        return this.childPages;
    }
    
    public TableForm getChildPagesForm() {
        return this.childPagesForm;
    }
    
    public List<EditChildPage> getDetailChildPages() {
        return this.detailChildPages;
    }
    
    public TableForm getDetailChildPagesForm() {
        return this.detailChildPagesForm;
    }
    
    protected Resolution forwardToPageChildren() {
        return (Resolution)new ForwardResolution("/m/admin/page/children.jsp");
    }
    
    public Resolution pagePermissions() {
        if (!this.checkPermissionsOnTargetPage(this.getPageInstance(), AccessLevel.DEVELOP)) {
            return (Resolution)new ForbiddenAccessResolution("You don't have permissions to do that");
        }
        this.setupGroups();
        final PortofinoRealm portofinoRealm = ShiroUtils.getPortofinoRealm();
        (this.users = new LinkedHashMap<Serializable, String>()).put(null, "(anonymous)");
        this.users.putAll(portofinoRealm.getUsers());
        return this.forwardToPagePermissions();
    }
    
    @Button(list = "testUserPermissions", key = "test")
    public Resolution testUserPermissions() {
        if (!this.checkPermissionsOnTargetPage(this.getPageInstance(), AccessLevel.DEVELOP)) {
            return (Resolution)new ForbiddenAccessResolution("You don't have permissions to do that");
        }
        this.testUserId = StringUtils.defaultIfEmpty(this.testUserId, (String)null);
        final PortofinoRealm portofinoRealm = ShiroUtils.getPortofinoRealm();
        PrincipalCollection principalCollection;
        if (!StringUtils.isEmpty(this.testUserId)) {
            final Serializable user = portofinoRealm.getUserById(this.testUserId);
            principalCollection = (PrincipalCollection)new SimplePrincipalCollection((Object)user, "realm");
        }
        else {
            principalCollection = null;
        }
        final Permissions permissions = SecurityLogic.calculateActualPermissions(this.getPageInstance());
        this.testedAccessLevel = AccessLevel.NONE;
        this.testedPermissions = new HashSet<String>();
        final SecurityManager securityManager = SecurityUtils.getSecurityManager();
        for (final AccessLevel level : AccessLevel.values()) {
            if (level.isGreaterThanOrEqual(this.testedAccessLevel) && SecurityLogic.hasPermissions(this.portofinoConfiguration, permissions, securityManager, principalCollection, level, new String[0])) {
                this.testedAccessLevel = level;
            }
        }
        final String[] supportedPermissions = this.getSupportedPermissions();
        if (supportedPermissions != null) {
            for (final String permission : supportedPermissions) {
                final boolean permitted = SecurityLogic.hasPermissions(this.portofinoConfiguration, permissions, securityManager, principalCollection, this.testedAccessLevel, new String[] { permission });
                if (permitted) {
                    this.testedPermissions.add(permission);
                }
            }
        }
        return this.pagePermissions();
    }
    
    public String[] getSupportedPermissions() {
        Class<?> actualActionClass;
        SupportsPermissions supportsPermissions;
        for (actualActionClass = (Class<?>)this.getPageInstance().getActionClass(), supportsPermissions = actualActionClass.getAnnotation(SupportsPermissions.class); supportsPermissions == null && actualActionClass.getSuperclass() != Object.class; actualActionClass = actualActionClass.getSuperclass(), supportsPermissions = actualActionClass.getAnnotation(SupportsPermissions.class)) {}
        if (supportsPermissions != null && supportsPermissions.value().length > 0) {
            return supportsPermissions.value();
        }
        return null;
    }
    
    protected Resolution forwardToPagePermissions() {
        return (Resolution)new ForwardResolution("/m/admin/page/permissions.jsp");
    }
    
    protected void setupGroups() {
        final PortofinoRealm portofinoRealm = ShiroUtils.getPortofinoRealm();
        this.groups = (Set<String>)portofinoRealm.getGroups();
    }
    
    @Button(list = "page-permissions-edit", key = "update", order = 1.0, type = " btn-primary ")
    public Resolution updatePagePermissions() {
        if (!this.checkPermissionsOnTargetPage(this.getPageInstance(), AccessLevel.DEVELOP)) {
            return (Resolution)new ForbiddenAccessResolution("You don't have permissions to do that");
        }
        try {
            this.updatePagePermissions(this.getPageInstance());
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("page.permissions.saved.successfully", new Object[0]));
            return (Resolution)((RedirectResolution)new RedirectResolution(HttpUtil.getRequestedPath(this.context.getRequest())).addParameter("pagePermissions", new Object[0])).addParameter("originalPath", new Object[] { this.originalPath });
        }
        catch (Exception e) {
            PageAdminAction.logger.error("Couldn't update page permissions", (Throwable)e);
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("permissions.page.notUpdated", new Object[0]));
            return (Resolution)((RedirectResolution)new RedirectResolution(HttpUtil.getRequestedPath(this.context.getRequest())).addParameter("pagePermissions", new Object[0])).addParameter("originalPath", new Object[] { this.originalPath });
        }
    }
    
    protected void updatePagePermissions(final PageInstance page) throws Exception {
        final Permissions pagePermissions = page.getPage().getPermissions();
        pagePermissions.getGroups().clear();
        final Map<String, Group> groups = new HashMap<String, Group>();
        for (final Map.Entry<String, String> entry : this.accessLevels.entrySet()) {
            final Group group = new Group();
            group.setName((String)entry.getKey());
            group.setAccessLevel((String)entry.getValue());
            groups.put(group.getName(), group);
        }
        for (final Map.Entry<String, List<String>> custPerm : this.permissions.entrySet()) {
            final String groupId = custPerm.getKey();
            Group group2 = groups.get(groupId);
            if (group2 == null) {
                group2 = new Group();
                group2.setName(groupId);
                groups.put(groupId, group2);
            }
            group2.getPermissions().addAll(custPerm.getValue());
        }
        for (final Group group3 : groups.values()) {
            pagePermissions.getGroups().add(group3);
        }
        DispatcherLogic.savePage(page);
    }
    
    public AccessLevel getLocalAccessLevel(final Page currentPage, final String groupId) {
        final Permissions currentPagePermissions = currentPage.getPermissions();
        for (final Group group : currentPagePermissions.getGroups()) {
            if (groupId.equals(group.getName())) {
                return group.getActualAccessLevel();
            }
        }
        return null;
    }
    
    public Resolution confirmDelete() {
        return (Resolution)new ForwardResolution("/m/admin/page/deletePageDialog.jsp");
    }
    
    public Resolution chooseNewLocation() {
        this.buildMovePageForm();
        return (Resolution)new ForwardResolution("/m/admin/page/movePageDialog.jsp");
    }
    
    public Resolution copyPageDialog() {
        this.buildCopyPageForm();
        return (Resolution)new ForwardResolution("/m/admin/page/copyPageDialog.jsp");
    }
    
    protected void buildMovePageForm() {
        final PageInstance pageInstance = this.dispatch.getLastPageInstance();
        final SelectionProvider pagesSelectionProvider = DispatcherLogic.createPagesSelectionProvider(this.pagesDir, true, true, new File[] { pageInstance.getDirectory() });
        this.moveForm = new FormBuilder((Class)MovePage.class).configReflectiveFields().configSelectionProvider(pagesSelectionProvider, new String[] { "destinationPagePath" }).build();
    }
    
    protected void buildCopyPageForm() {
        final PageInstance pageInstance = this.dispatch.getLastPageInstance();
        final SelectionProvider pagesSelectionProvider = DispatcherLogic.createPagesSelectionProvider(this.pagesDir, true, true, new File[] { pageInstance.getDirectory() });
        this.copyForm = new FormBuilder((Class)CopyPage.class).configReflectiveFields().configSelectionProvider(pagesSelectionProvider, new String[] { "destinationPagePath" }).build();
    }
    
    public Form getMoveForm() {
        return this.moveForm;
    }
    
    public Form getCopyForm() {
        return this.copyForm;
    }
    
    public Set<String> getGroups() {
        return this.groups;
    }
    
    public Map<String, String> getAccessLevels() {
        return this.accessLevels;
    }
    
    public void setAccessLevels(final Map<String, String> accessLevels) {
        this.accessLevels = accessLevels;
    }
    
    public Map<String, List<String>> getPermissions() {
        return this.permissions;
    }
    
    public void setPermissions(final Map<String, List<String>> permissions) {
        this.permissions = permissions;
    }
    
    public String getTestUserId() {
        return this.testUserId;
    }
    
    public void setTestUserId(final String testUserId) {
        this.testUserId = testUserId;
    }
    
    public Map<Serializable, String> getUsers() {
        return this.users;
    }
    
    public AccessLevel getTestedAccessLevel() {
        return this.testedAccessLevel;
    }
    
    public Set<String> getTestedPermissions() {
        return this.testedPermissions;
    }
    
    public Form getNewPageForm() {
        return this.newPageForm;
    }
    
    public String getOriginalPath() {
        return this.originalPath;
    }
    
    public void setOriginalPath(final String originalPath) {
        this.originalPath = originalPath;
    }
    
    public Dispatch getDispatch() {
        return this.dispatch;
    }
    
    public String getCancelReturnUrl() {
        return this.context.getRequest().getContextPath() + this.originalPath;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)PageAdminAction.class);
        NEW_PAGE_SETUP_FIELDS = new String[][] { { "actionClassName", "fragment", "title", "description", "insertPositionName" } };
    }
    
    public enum InsertPosition
    {
        TOP, 
        CHILD, 
        SIBLING;
    }
}
