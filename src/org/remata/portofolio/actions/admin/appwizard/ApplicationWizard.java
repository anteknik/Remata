package org.remata.portofolio.actions.admin.appwizard;

import org.remata.portofolio.pageactions.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.persistence.*;
import org.remata.portofolio.di.*;
import org.remata.elements.options.*;
import org.remata.elements.*;
import org.remata.portofolio.actions.admin.database.forms.*;
import org.remata.portofolio.database.platforms.*;
import org.remata.elements.messages.*;
import org.remata.portofolio.buttons.annotations.*;
import org.apache.commons.lang.exception.*;
import org.remata.portofolio.sync.*;
import org.apache.commons.lang.*;
import com.google.common.collect.*;
import org.remata.elements.forms.*;
import org.remata.elements.reflection.*;
import org.remata.elements.fields.*;
import org.remata.portofolio.dispatcher.*;
import org.apache.shiro.*;
import net.sourceforge.stripes.action.*;
import groovy.text.*;
import java.awt.*;
import java.util.*;
import org.remata.portofolio.pageactions.calendar.configuration.*;
import org.remata.elements.util.*;
import java.io.*;
import org.apache.commons.io.*;
import org.remata.portofolio.logic.*;
import org.remata.portofolio.security.*;
import org.remata.portofolio.pages.*;
import org.apache.commons.configuration.*;
import org.remata.portofolio.model.database.*;
import org.remata.portofolio.model.*;
import org.remata.portofolio.pageactions.crud.configuration.*;
import liquibase.database.jvm.*;
import liquibase.database.*;
import java.sql.*;
import org.remata.elements.annotations.*;
import org.slf4j.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/wizard")
public class ApplicationWizard extends AbstractPageAction
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/wizard";
    public static final String JDBC = "JDBC";
    public static final String JNDI = "JNDI";
    public static final String NO_LINK_TO_PARENT;
    public static final int LARGE_RESULT_SET_THRESHOLD = 10000;
    protected int step;
    protected SelectField connectionProviderField;
    protected Form jndiCPForm;
    protected Form jdbcCPForm;
    protected Form connectionProviderForm;
    protected Form userAndGroupTablesForm;
    protected Form userManagementSetupForm;
    protected String connectionProviderType;
    protected String connectionProviderName;
    protected ConnectionProvider connectionProvider;
    protected Database database;
    public TableForm schemasForm;
    protected List<SelectableSchema> selectableSchemas;
    public TableForm rootsForm;
    protected List<SelectableRoot> selectableRoots;
    protected String generationStrategy;
    protected BooleanField generateCalendarField;
    protected boolean generateCalendar;
    protected String userTableName;
    protected String groupTableName;
    protected String userGroupTableName;
    protected String userNameProperty;
    protected String userEmailProperty;
    protected String userTokenProperty;
    protected String userIdProperty;
    protected String userPasswordProperty;
    protected String encryptionAlgorithm;
    protected String groupIdProperty;
    protected String groupNameProperty;
    protected String groupLinkProperty;
    protected String userLinkProperty;
    protected String adminGroupName;
    protected List<Table> roots;
    protected ListMultimap<Table, Reference> children;
    protected List<Table> allTables;
    protected Table userTable;
    protected Table groupTable;
    protected Table userGroupTable;
    protected int maxColumnsInSummary;
    protected int maxDepth;
    protected int depth;
    private final String databaseSessionKey;
    public static final String[] JDBC_CP_FIELDS;
    public static final String[] JNDI_CP_FIELDS;
    @Inject("org.remata.portofolio.modules.DatabaseModule.persistence")
    public Persistence persistence;
    @Inject("PAGES_DIRECTORY")
    public File pagesDir;
    @Inject("org.remata.portofolio.application.directory")
    public File appDir;
    public static final Logger logger;
    public static final int MULTILINE_THRESHOLD = 256;
    protected final Set<Column> detectedBooleanColumns;
    protected final Map<Table, Boolean> largeResultSet;
    
    public ApplicationWizard() {
        this.step = 0;
        this.selectableRoots = new ArrayList<SelectableRoot>();
        this.generationStrategy = "AUTO";
        this.generateCalendar = true;
        this.maxColumnsInSummary = 5;
        this.maxDepth = 5;
        this.databaseSessionKey = this.getClass().getName() + ".database";
        this.detectedBooleanColumns = new HashSet<Column>();
        this.largeResultSet = new HashMap<Table, Boolean>();
    }
    
    @DefaultHandler
    @Button(list = "select-schemas", key = "<<previous", order = 1.0)
    public Resolution start() {
        this.buildCPForms();
        this.context.getRequest().getSession().removeAttribute(this.databaseSessionKey);
        return this.createConnectionProviderForm();
    }
    
    protected Resolution createConnectionProviderForm() {
        this.step = 0;
        return (Resolution)new ForwardResolution("/m/admin/wizard/connection-provider.jsp");
    }
    
    @Before
    public void prepare() {
        this.connectionProviderName = this.context.getRequest().getParameter("connectionProviderName");
    }
    
    protected void buildCPForms() {
        final DefaultSelectionProvider connectionProviderSP = new DefaultSelectionProvider("connectionProviderName");
        for (final Database db : this.persistence.getModel().getDatabases()) {
            final ConnectionProvider cp = db.getConnectionProvider();
            if (!"error".equals(cp.getStatus())) {
                connectionProviderSP.appendRow((Object)db.getDatabaseName(), db.getDatabaseName() + " (" + cp.getDatabasePlatform().getDescription() + ")", true);
            }
        }
        final ClassAccessor classAccessor = (ClassAccessor)JavaClassAccessor.getClassAccessor((Class)ApplicationWizard.class);
        try {
            (this.connectionProviderField = new SelectField(classAccessor.getProperty("connectionProviderName"), (SelectionProvider)connectionProviderSP, Mode.EDIT, (String)null)).setLabel(ElementsThreadLocals.getText("use.an.existing.database.connection", new Object[0]));
            this.connectionProviderField.setComboLabel("--");
        }
        catch (NoSuchFieldException e) {
            throw new Error(e);
        }
        this.jndiCPForm = new FormBuilder((Class)ConnectionProviderForm.class).configFields(ApplicationWizard.JNDI_CP_FIELDS).configPrefix("jndi").configMode(Mode.CREATE).build();
        final DefaultSelectionProvider driverSelectionProvider = new DefaultSelectionProvider("name");
        final DatabasePlatformsRegistry manager = this.persistence.getDatabasePlatformsRegistry();
        final DatabasePlatform[] arr$;
        final DatabasePlatform[] databasePlatforms = arr$ = manager.getDatabasePlatforms();
        for (final DatabasePlatform dp : arr$) {
            if ("ok".equals(dp.getStatus())) {
                driverSelectionProvider.appendRow((Object)dp.getStandardDriverClassName(), dp.getDescription(), true);
            }
        }
        this.jdbcCPForm = new FormBuilder((Class)ConnectionProviderForm.class).configFields(ApplicationWizard.JDBC_CP_FIELDS).configPrefix("jdbc").configMode(Mode.CREATE).configSelectionProvider((SelectionProvider)driverSelectionProvider, new String[] { "driver" }).build();
        this.jdbcCPForm.findFieldByPropertyName("driver").setHelp(ElementsThreadLocals.getText("additional.drivers.can.be.downloaded", new Object[0]));
        this.jndiCPForm.readFromRequest(this.context.getRequest());
        this.jdbcCPForm.readFromRequest(this.context.getRequest());
        this.connectionProviderField.readFromObject((Object)this);
        this.connectionProviderField.readFromRequest(this.context.getRequest());
    }
    
    @Button(list = "user-management", key = "<<previous", order = 1.0)
    public Resolution backToSelectSchemas() {
        this.context.getRequest().getSession().removeAttribute(this.databaseSessionKey);
        return this.configureConnectionProvider();
    }
    
    @Button(list = "connection-provider", key = "next>>", order = 1.0, type = " btn-primary ")
    public Resolution configureConnectionProvider() {
        this.buildCPForms();
        if (!this.connectionProviderField.validate()) {
            return this.createConnectionProviderForm();
        }
        this.connectionProviderField.writeToObject((Object)this);
        if (!this.isNewConnectionProvider()) {
            this.connectionProvider = DatabaseLogic.findDatabaseByName(this.persistence.getModel(), this.connectionProviderName).getConnectionProvider();
            return this.afterCreateConnectionProvider();
        }
        if ("JDBC".equals(this.connectionProviderType)) {
            final JdbcConnectionProvider jdbcConnectionProvider = new JdbcConnectionProvider();
            jdbcConnectionProvider.setUrl("replace me");
            jdbcConnectionProvider.setUsername("replace me");
            jdbcConnectionProvider.setPassword("replace me");
            this.connectionProvider = (ConnectionProvider)jdbcConnectionProvider;
            this.connectionProviderForm = this.jdbcCPForm;
        }
        else {
            if (!"JNDI".equals(this.connectionProviderType)) {
                throw new Error("Unknown connection provider type: " + this.connectionProviderType);
            }
            this.connectionProvider = (ConnectionProvider)new JndiConnectionProvider();
            this.connectionProviderForm = this.jndiCPForm;
        }
        final Database database = new Database();
        database.setConnectionProvider(this.connectionProvider);
        this.connectionProvider.setDatabase(database);
        final ConnectionProviderForm edit = new ConnectionProviderForm(database);
        this.connectionProviderForm.readFromRequest(this.context.getRequest());
        if (!this.connectionProviderForm.validate()) {
            return this.createConnectionProviderForm();
        }
        this.connectionProviderForm.writeToObject((Object)edit);
        final Database existingDatabase = DatabaseLogic.findDatabaseByName(this.persistence.getModel(), edit.getDatabaseName());
        if (existingDatabase != null) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("there.is.already.a.database.named._", new Object[] { edit.getDatabaseName() }));
            return this.createConnectionProviderForm();
        }
        return this.afterCreateConnectionProvider();
    }
    
    public Resolution afterCreateConnectionProvider() {
        try {
            this.configureEditSchemas();
        }
        catch (Exception e) {
            ApplicationWizard.logger.error("Couldn't read schema names from db", (Throwable)e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("couldnt.read.schema.names.from.db._", new Object[] { e }));
            return this.createConnectionProviderForm();
        }
        return this.selectSchemasForm();
    }
    
    protected Resolution selectSchemasForm() {
        this.step = 1;
        return (Resolution)new ForwardResolution("/m/admin/wizard/select-schemas.jsp");
    }
    
    protected void configureEditSchemas() throws Exception {
        this.connectionProvider.init(this.persistence.getDatabasePlatformsRegistry());
        final Connection conn = this.connectionProvider.acquireConnection();
        ApplicationWizard.logger.debug("Reading database metadata");
        final DatabaseMetaData metadata = conn.getMetaData();
        final List<String> schemaNamesFromDb = (List<String>)this.connectionProvider.getDatabasePlatform().getSchemaNames(metadata);
        this.connectionProvider.releaseConnection(conn);
        this.selectableSchemas = new ArrayList<SelectableSchema>(schemaNamesFromDb.size());
        for (final String schemaName : schemaNamesFromDb) {
            final SelectableSchema schema = new SelectableSchema(schemaName, schemaNamesFromDb.size() == 1);
            this.selectableSchemas.add(schema);
        }
        (this.schemasForm = new TableFormBuilder((Class)SelectableSchema.class).configFields(new String[] { "selected", "schemaName" }).configMode(Mode.EDIT).configNRows(this.selectableSchemas.size()).configPrefix("schemas_").build()).readFromObject((Object)this.selectableSchemas);
        this.schemasForm.readFromRequest(this.context.getRequest());
    }
    
    @Buttons({ @Button(list = "select-schemas", key = "next>>", order = 2.0, type = " btn-primary "), @Button(list = "select-user-fields", key = "<<previous", order = 1.0) })
    public Resolution selectSchemas() {
        this.configureConnectionProvider();
        this.schemasForm.readFromRequest(this.context.getRequest());
        if (!this.schemasForm.validate()) {
            return this.selectSchemasForm();
        }
        this.schemasForm.writeToObject((Object)this.selectableSchemas);
        final boolean atLeastOneSelected = this.isAtLeastOneSchemaSelected();
        if (!atLeastOneSelected) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("select.at.least.a.schema", new Object[0]));
            return this.selectSchemasForm();
        }
        if (this.configureModelSchemas(false) == null) {
            return this.selectSchemasForm();
        }
        return this.afterSelectSchemas();
    }
    
    protected boolean isAtLeastOneSchemaSelected() {
        boolean atLeastOneSelected = false;
        for (final SelectableSchema schema : this.selectableSchemas) {
            if (schema.selected) {
                atLeastOneSelected = true;
                break;
            }
        }
        return atLeastOneSelected;
    }
    
    protected void updateModelFailed(final Exception e) {
        ApplicationWizard.logger.error("Could not update model", (Throwable)e);
        SessionMessages.addErrorMessage(ElementsThreadLocals.getText("could.not.save.model._", new Object[] { ExceptionUtils.getRootCauseMessage((Throwable)e) }));
        if (this.isNewConnectionProvider()) {
            this.persistence.getModel().getDatabases().remove(this.connectionProvider.getDatabase());
        }
        this.persistence.initModel();
    }
    
    protected Database configureModelSchemas(final boolean alwaysUseExistingModel) {
        Model refModel;
        if (!alwaysUseExistingModel && this.isNewConnectionProvider()) {
            refModel = new Model();
        }
        else {
            refModel = this.persistence.getModel();
        }
        final List<Schema> tempSchemas = new ArrayList<Schema>();
        final Database database = this.connectionProvider.getDatabase();
        for (final SelectableSchema schema : this.selectableSchemas) {
            Schema modelSchema = DatabaseLogic.findSchemaByName(database, schema.schemaName);
            if (schema.selected && modelSchema == null) {
                modelSchema = new Schema();
                modelSchema.setSchemaName(schema.schemaName);
                modelSchema.setDatabase(database);
                database.getSchemas().add(modelSchema);
                tempSchemas.add(modelSchema);
            }
        }
        this.database = (Database)this.context.getRequest().getSession().getAttribute(this.databaseSessionKey);
        if (this.database != null) {
            return this.database;
        }
        final DatabaseSyncer dbSyncer = new DatabaseSyncer(this.connectionProvider);
        Database targetDatabase;
        try {
            targetDatabase = dbSyncer.syncDatabase(refModel);
        }
        catch (Exception e) {
            ApplicationWizard.logger.error(e.getMessage(), (Throwable)e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("error.in.database.synchronization._", new Object[] { e }));
            return null;
        }
        finally {
            database.getSchemas().removeAll(tempSchemas);
            this.connectionProvider.setDatabase(database);
        }
        final Model model = new Model();
        model.getDatabases().add(targetDatabase);
        model.init();
        this.database = targetDatabase;
        this.context.getRequest().getSession().setAttribute(this.databaseSessionKey, (Object)this.database);
        return targetDatabase;
    }
    
    public boolean isNewConnectionProvider() {
        return StringUtils.isEmpty(this.connectionProviderName);
    }
    
    public Resolution afterSelectSchemas() {
        this.children = (ListMultimap<Table, Reference>)ArrayListMultimap.create();
        this.allTables = new ArrayList<Table>();
        this.roots = this.determineRoots((Multimap<Table, Reference>)this.children, this.allTables);
        Collections.sort(this.allTables, new Comparator<Table>() {
            @Override
            public int compare(final Table o1, final Table o2) {
                return o1.getQualifiedName().compareToIgnoreCase(o2.getQualifiedName());
            }
        });
        (this.rootsForm = new TableFormBuilder((Class)SelectableRoot.class).configFields(new String[] { "selected", "tableName" }).configMode(Mode.EDIT).configNRows(this.selectableRoots.size()).configPrefix("roots_").build()).readFromObject((Object)this.selectableRoots);
        try {
            final ClassAccessor classAccessor = (ClassAccessor)JavaClassAccessor.getClassAccessor((Class)this.getClass());
            final PropertyAccessor userPropertyAccessor = classAccessor.getProperty("userTableName");
            final PropertyAccessor groupPropertyAccessor = classAccessor.getProperty("groupTableName");
            final PropertyAccessor userGroupPropertyAccessor = classAccessor.getProperty("userGroupTableName");
            final DefaultSelectionProvider selectionProvider = new DefaultSelectionProvider("tableName");
            int schemaCount = 0;
            for (final SelectableSchema schema : this.selectableSchemas) {
                if (schema.selected) {
                    ++schemaCount;
                }
            }
            for (final Table table : this.allTables) {
                String tableName;
                if (schemaCount > 1) {
                    tableName = table.getSchemaName() + "." + table.getTableName();
                }
                else {
                    tableName = table.getTableName();
                }
                selectionProvider.appendRow((Object)table.getQualifiedName(), tableName, true);
            }
            final Mode mode = Mode.CREATE;
            final Field userTableField = (Field)new SelectField(userPropertyAccessor, (SelectionProvider)selectionProvider, mode, "");
            final Field groupTableField = (Field)new SelectField(groupPropertyAccessor, (SelectionProvider)selectionProvider, mode, "");
            final Field userGroupTableField = (Field)new SelectField(userGroupPropertyAccessor, (SelectionProvider)selectionProvider, mode, "");
            this.userAndGroupTablesForm = new Form(mode);
            final FieldSet fieldSet = new FieldSet(ElementsThreadLocals.getText("users.and.groups.tables", new Object[0]), 1, mode);
            fieldSet.add((Object)userTableField);
            fieldSet.add((Object)groupTableField);
            fieldSet.add((Object)userGroupTableField);
            this.userAndGroupTablesForm.add((Object)fieldSet);
            this.userAndGroupTablesForm.readFromRequest(this.context.getRequest());
        }
        catch (NoSuchFieldException e) {
            throw new Error(e);
        }
        return this.userManagementForm();
    }
    
    protected Resolution userManagementForm() {
        this.step = 2;
        return (Resolution)new ForwardResolution("/m/admin/wizard/user-management.jsp");
    }
    
    @Button(list = "user-management", key = "next>>", order = 2.0, type = " btn-primary ")
    public Resolution setupUserManagement() {
        this.selectSchemas();
        this.userAndGroupTablesForm.readFromRequest(this.context.getRequest());
        this.userAndGroupTablesForm.writeToObject((Object)this);
        if (!StringUtils.isEmpty(this.userTableName)) {
            final Model tmpModel = new Model();
            tmpModel.getDatabases().add(this.database);
            String[] name = DatabaseLogic.splitQualifiedTableName(this.userTableName);
            this.userTable = DatabaseLogic.findTableByName(tmpModel, name[0], name[1], name[2]);
            if (!StringUtils.isEmpty(this.groupTableName)) {
                name = DatabaseLogic.splitQualifiedTableName(this.groupTableName);
                this.groupTable = DatabaseLogic.findTableByName(tmpModel, name[0], name[1], name[2]);
            }
            if (!StringUtils.isEmpty(this.userGroupTableName)) {
                name = DatabaseLogic.splitQualifiedTableName(this.userGroupTableName);
                this.userGroupTable = DatabaseLogic.findTableByName(tmpModel, name[0], name[1], name[2]);
            }
            this.createUserManagementSetupForm();
            return this.selectUserFieldsForm();
        }
        return this.selectTablesForm();
    }
    
    protected void createUserManagementSetupForm() {
        final DefaultSelectionProvider userSelectionProvider = new DefaultSelectionProvider("");
        for (final Column column : this.userTable.getColumns()) {
            userSelectionProvider.appendRow((Object)column.getActualPropertyName(), column.getActualPropertyName(), true);
        }
        final DefaultSelectionProvider algoSelectionProvider = new DefaultSelectionProvider("");
        algoSelectionProvider.appendRow((Object)"plaintext:plaintext", ElementsThreadLocals.getText("plain.text", new Object[0]), true);
        algoSelectionProvider.appendRow((Object)"MD5:base64", ElementsThreadLocals.getText("md5.base64.encoded", new Object[0]), true);
        algoSelectionProvider.appendRow((Object)"MD5:hex", ElementsThreadLocals.getText("md5.hex.encoded", new Object[0]), true);
        algoSelectionProvider.appendRow((Object)"SHA-1:base64", ElementsThreadLocals.getText("sha1.base64.encoded.portofino3", new Object[0]), true);
        algoSelectionProvider.appendRow((Object)"SHA-1:hex", ElementsThreadLocals.getText("sha1.hex.encoded", new Object[0]), true);
        algoSelectionProvider.appendRow((Object)"SHA-256:base64", ElementsThreadLocals.getText("sha256.base64.encoded", new Object[0]), true);
        algoSelectionProvider.appendRow((Object)"SHA-256:hex", ElementsThreadLocals.getText("sha256.hex.encoded", new Object[0]), true);
        try {
            final ClassAccessor classAccessor = (ClassAccessor)JavaClassAccessor.getClassAccessor((Class)this.getClass());
            final Mode mode = Mode.CREATE;
            this.userManagementSetupForm = new Form(mode);
            PropertyAccessor propertyAccessor = classAccessor.getProperty("userIdProperty");
            final Field userIdPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userSelectionProvider, mode, "");
            userIdPropertyField.setRequired(true);
            propertyAccessor = classAccessor.getProperty("userNameProperty");
            final Field userNamePropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userSelectionProvider, mode, "");
            userNamePropertyField.setRequired(true);
            propertyAccessor = classAccessor.getProperty("userPasswordProperty");
            final Field userPasswordPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userSelectionProvider, mode, "");
            userPasswordPropertyField.setRequired(true);
            propertyAccessor = classAccessor.getProperty("encryptionAlgorithm");
            final Field encryptionAlgorithmField = (Field)new SelectField(propertyAccessor, (SelectionProvider)algoSelectionProvider, mode, "");
            encryptionAlgorithmField.setRequired(true);
            propertyAccessor = classAccessor.getProperty("userEmailProperty");
            final Field userEmailPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userSelectionProvider, mode, "");
            userEmailPropertyField.setRequired(false);
            propertyAccessor = classAccessor.getProperty("userTokenProperty");
            final Field userTokenPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userSelectionProvider, mode, "");
            userTokenPropertyField.setRequired(false);
            final FieldSet uFieldSet = new FieldSet(ElementsThreadLocals.getText("users.table.setup", new Object[0]), 1, mode);
            uFieldSet.add((Object)userIdPropertyField);
            uFieldSet.add((Object)userNamePropertyField);
            uFieldSet.add((Object)userPasswordPropertyField);
            uFieldSet.add((Object)encryptionAlgorithmField);
            uFieldSet.add((Object)userEmailPropertyField);
            uFieldSet.add((Object)userTokenPropertyField);
            this.userManagementSetupForm.add((Object)uFieldSet);
            this.userIdProperty = this.userTable.getPrimaryKey().getColumns().get(0).getActualPropertyName();
            if (this.groupTable != null && this.userGroupTable != null) {
                final DefaultSelectionProvider groupSelectionProvider = new DefaultSelectionProvider("");
                for (final Column column2 : this.groupTable.getColumns()) {
                    groupSelectionProvider.appendRow((Object)column2.getActualPropertyName(), column2.getActualPropertyName(), true);
                }
                final DefaultSelectionProvider userGroupSelectionProvider = new DefaultSelectionProvider("");
                for (final Column column3 : this.userGroupTable.getColumns()) {
                    userGroupSelectionProvider.appendRow((Object)column3.getActualPropertyName(), column3.getActualPropertyName(), true);
                }
                propertyAccessor = classAccessor.getProperty("groupIdProperty");
                final Field groupIdPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)groupSelectionProvider, mode, "");
                groupIdPropertyField.setRequired(true);
                propertyAccessor = classAccessor.getProperty("groupNameProperty");
                final Field groupNamePropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)groupSelectionProvider, mode, "");
                groupNamePropertyField.setRequired(true);
                propertyAccessor = classAccessor.getProperty("groupLinkProperty");
                final Field groupLinkPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userGroupSelectionProvider, mode, "");
                groupLinkPropertyField.setRequired(true);
                propertyAccessor = classAccessor.getProperty("userLinkProperty");
                final Field userLinkPropertyField = (Field)new SelectField(propertyAccessor, (SelectionProvider)userGroupSelectionProvider, mode, "");
                userLinkPropertyField.setRequired(true);
                propertyAccessor = classAccessor.getProperty("adminGroupName");
                final Field adminGroupNameField = (Field)new TextField(propertyAccessor, mode);
                final FieldSet gFieldSet = new FieldSet(ElementsThreadLocals.getText("groups.tables.setup", new Object[0]), 1, mode);
                gFieldSet.add((Object)groupIdPropertyField);
                gFieldSet.add((Object)groupNamePropertyField);
                gFieldSet.add((Object)groupLinkPropertyField);
                gFieldSet.add((Object)userLinkPropertyField);
                gFieldSet.add((Object)adminGroupNameField);
                this.userManagementSetupForm.add((Object)gFieldSet);
                this.groupIdProperty = this.groupTable.getPrimaryKey().getColumns().get(0).getActualPropertyName();
                for (final ForeignKey fk : this.userGroupTable.getForeignKeys()) {
                    for (final Reference ref : fk.getReferences()) {
                        if (ref.getActualToColumn().getTable().equals(this.userTable)) {
                            this.userLinkProperty = ref.getActualFromColumn().getActualPropertyName();
                        }
                        else {
                            if (!ref.getActualToColumn().getTable().equals(this.groupTable)) {
                                continue;
                            }
                            this.groupLinkProperty = ref.getActualFromColumn().getActualPropertyName();
                        }
                    }
                }
            }
            this.userManagementSetupForm.readFromObject((Object)this);
        }
        catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }
    
    @Button(list = "select-user-fields", key = "next>>", order = 2.0, type = " btn-primary ")
    public Resolution selectUserFields() {
        this.setupUserManagement();
        if (this.userTable == null) {
            return this.selectTablesForm();
        }
        this.userManagementSetupForm.readFromRequest(this.context.getRequest());
        if (this.userManagementSetupForm.validate()) {
            this.userManagementSetupForm.writeToObject((Object)this);
            return this.selectTablesForm();
        }
        return this.selectUserFieldsForm();
    }
    
    protected Resolution selectUserFieldsForm() {
        this.step = 3;
        return (Resolution)new ForwardResolution("/m/admin/wizard/select-user-fields.jsp");
    }
    
    protected Resolution selectTablesForm() {
        this.step = ((this.userTable == null) ? 3 : 4);
        this.setupCalendarField();
        return (Resolution)new ForwardResolution("/m/admin/wizard/select-tables.jsp");
    }
    
    protected void setupCalendarField() {
        final ClassAccessor classAccessor = (ClassAccessor)JavaClassAccessor.getClassAccessor((Class)ApplicationWizard.class);
        try {
            (this.generateCalendarField = new BooleanField(classAccessor.getProperty("generateCalendar"), Mode.EDIT)).setLabel(ElementsThreadLocals.getText("generate.a.calendar.page", new Object[0]));
            this.generateCalendarField.readFromObject((Object)this);
        }
        catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }
    
    protected List<Table> determineRoots(final Multimap<Table, Reference> children, final List<Table> allTables) {
        final List<Table> roots = new ArrayList<Table>();
        for (final SelectableSchema selectableSchema : this.selectableSchemas) {
            if (selectableSchema.selected) {
                final Schema schema = DatabaseLogic.findSchemaByName(this.database, selectableSchema.schemaName);
                roots.addAll(schema.getTables());
            }
        }
        final Iterator<Table> it = roots.iterator();
        while (it.hasNext()) {
            final Table table = it.next();
            if (table.getPrimaryKey() == null) {
                it.remove();
            }
            else {
                allTables.add(table);
                boolean removed = false;
                boolean selected = false;
                boolean known = false;
                for (final SelectableRoot root : this.selectableRoots) {
                    if (root.tableName.equals(table.getSchemaName() + "." + table.getTableName())) {
                        selected = root.selected;
                        known = true;
                        break;
                    }
                }
                if (known && !selected) {
                    it.remove();
                    removed = true;
                }
                if (!table.getForeignKeys().isEmpty()) {
                    for (final ForeignKey fk : table.getForeignKeys()) {
                        for (final Reference ref : fk.getReferences()) {
                            final Column column = ref.getActualToColumn();
                            if (column.getTable() != table) {
                                children.put((Object)column.getTable(), (Object)ref);
                                if (selected || removed) {
                                    continue;
                                }
                                it.remove();
                                removed = true;
                            }
                        }
                    }
                }
                if (!table.getSelectionProviders().isEmpty()) {
                    for (final ModelSelectionProvider sp : table.getSelectionProviders()) {
                        for (final Reference ref : sp.getReferences()) {
                            final Column column = ref.getActualToColumn();
                            if (column != null && column.getTable() != table) {
                                children.put((Object)column.getTable(), (Object)ref);
                                if (selected || removed) {
                                    continue;
                                }
                                it.remove();
                                removed = true;
                            }
                        }
                    }
                }
                if (known) {
                    continue;
                }
                final SelectableRoot root2 = new SelectableRoot(table.getSchemaName() + "." + table.getTableName(), !removed);
                this.selectableRoots.add(root2);
            }
        }
        Collections.sort(this.selectableRoots, new Comparator<SelectableRoot>() {
            @Override
            public int compare(final SelectableRoot o1, final SelectableRoot o2) {
                return o1.tableName.compareTo(o2.tableName);
            }
        });
        return roots;
    }
    
    @Button(list = "select-tables", key = "next>>", order = 2.0, type = " btn-primary ")
    public Resolution selectTables() {
        this.selectUserFields();
        this.rootsForm.readFromRequest(this.context.getRequest());
        this.rootsForm.writeToObject((Object)this.selectableRoots);
        this.afterSelectSchemas();
        if (this.roots.isEmpty()) {
            SessionMessages.addWarningMessage(ElementsThreadLocals.getText("no.page.will.be.generated", new Object[0]));
        }
        return this.buildAppForm();
    }
    
    @Button(list = "select-tables", key = "<<previous", order = 1.0)
    public Resolution goBackFromSelectTables() {
        this.selectUserFields();
        if (this.userTable == null) {
            return this.userManagementForm();
        }
        return this.selectUserFieldsForm();
    }
    
    protected Resolution buildAppForm() {
        this.step = ((this.userTable == null) ? 4 : 5);
        this.setupCalendarField();
        this.generateCalendarField.readFromRequest(this.context.getRequest());
        this.generateCalendarField.writeToObject((Object)this);
        return (Resolution)new ForwardResolution("/m/admin/wizard/build-app.jsp");
    }
    
    @Button(list = "build-app", key = "<<previous", order = 1.0)
    public Resolution returnToSelectTables() {
        this.selectTables();
        return this.selectTablesForm();
    }
    
    @Button(list = "build-app", key = "finish", order = 2.0, type = " btn-primary ")
    public Resolution buildApplication() {
        this.selectTables();
        final Database oldDatabase = DatabaseLogic.findDatabaseByName(this.persistence.getModel(), this.database.getDatabaseName());
        if (oldDatabase != null) {
            this.persistence.getModel().getDatabases().remove(oldDatabase);
        }
        this.persistence.getModel().getDatabases().add(this.database);
        this.connectionProvider.setDatabase(this.database);
        this.database.setConnectionProvider(this.connectionProvider);
        try {
            this.persistence.initModel();
        }
        catch (Exception e) {
            this.updateModelFailed(e);
            return this.buildAppForm();
        }
        if (!this.generationStrategy.equals("NO")) {
            if (this.generationStrategy.equals("AUTO")) {
                this.generateCalendar = true;
            }
            try {
                final TemplateEngine engine = (TemplateEngine)new SimpleTemplateEngine();
                final Template template = engine.createTemplate(ApplicationWizard.class.getResource("CrudPage.groovy"));
                final List<ChildPage> childPages = new ArrayList<ChildPage>();
                for (final Table table : this.roots) {
                    final File dir = new File(this.pagesDir, table.getActualEntityName());
                    this.depth = 1;
                    this.createCrudPage(dir, table, childPages, template);
                }
                if (this.userTable != null) {
                    this.setupUserPages(childPages, template);
                }
                if (this.generateCalendar) {
                    this.setupCalendar(childPages);
                }
                final Page rootPage = DispatcherLogic.getPage(this.pagesDir);
                Collections.sort(childPages, new Comparator<ChildPage>() {
                    @Override
                    public int compare(final ChildPage o1, final ChildPage o2) {
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    }
                });
                rootPage.getLayout().getChildPages().addAll(childPages);
                DispatcherLogic.savePage(this.pagesDir, rootPage);
            }
            catch (Exception e) {
                ApplicationWizard.logger.error("Error while creating pages", (Throwable)e);
                SessionMessages.addErrorMessage(ElementsThreadLocals.getText("could.not.create.pages._", new Object[] { e }));
                return this.buildAppForm();
            }
        }
        if (this.userTable != null) {
            this.setupUsers();
        }
        try {
            this.persistence.initModel();
            this.persistence.saveXmlModel();
        }
        catch (Exception e) {
            this.updateModelFailed(e);
            return this.buildAppForm();
        }
        if (this.userTable != null) {
            SecurityUtils.getSubject().logout();
            this.context.getRequest().getSession().invalidate();
            SessionMessages.addWarningMessage(ElementsThreadLocals.getText("user.management.has.been.configured.please.edit.security.groovy", new Object[0]));
        }
        SessionMessages.addInfoMessage(ElementsThreadLocals.getText("application.created", new Object[0]));
        this.context.getRequest().getSession().removeAttribute(this.databaseSessionKey);
        return (Resolution)new RedirectResolution("/");
    }
    
    protected void setupCalendar(final List<ChildPage> childPages) throws Exception {
        final List<List<String>> calendarDefinitions = new ArrayList<List<String>>();
        final Color[] colors = { Color.RED, new Color(64, 128, 255), Color.CYAN.darker(), Color.GRAY, Color.GREEN.darker(), Color.ORANGE, Color.YELLOW.darker(), Color.MAGENTA.darker(), Color.PINK };
        int colorIndex = 0;
        for (final Table table : this.allTables) {
            final List<Column> dateColumns = new ArrayList<Column>();
            for (final Column column : table.getColumns()) {
                if (column.getActualJavaType() != null && Date.class.isAssignableFrom(column.getActualJavaType())) {
                    dateColumns.add(column);
                }
            }
            if (!dateColumns.isEmpty()) {
                final Color color = colors[colorIndex++ % colors.length];
                final List<String> calDef = new ArrayList<String>();
                calDef.add('\"' + Util.guessToWords(table.getActualEntityName()) + '\"');
                calDef.add('\"' + table.getQualifiedName() + '\"');
                String cols = "[";
                boolean first = true;
                for (final Column column2 : dateColumns) {
                    if (first) {
                        first = false;
                    }
                    else {
                        cols += ", ";
                    }
                    cols = cols + '\"' + column2.getActualPropertyName() + '\"';
                }
                cols += "]";
                calDef.add(cols);
                calDef.add("new java.awt.Color(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")");
                calendarDefinitions.add(calDef);
            }
        }
        if (!calendarDefinitions.isEmpty()) {
            String calendarDefinitionsStr = "[";
            calendarDefinitionsStr += StringUtils.join((Collection)calendarDefinitions, ", ");
            calendarDefinitionsStr += "]";
            String baseName;
            File dir;
            int retries;
            for (baseName = "calendar-" + this.connectionProvider.getDatabase().getDatabaseName(), dir = new File(this.pagesDir, baseName), retries = 1; dir.exists(); dir = new File(this.pagesDir, baseName + "-" + retries)) {
                ++retries;
            }
            if (dir.mkdirs()) {
                final CalendarConfiguration configuration = new CalendarConfiguration();
                DispatcherLogic.saveConfiguration(dir, (Object)configuration);
                final Page page = new Page();
                page.setId(RandomUtil.createRandomId());
                String calendarTitle = "Calendar (" + this.connectionProvider.getDatabase().getDatabaseName() + ")";
                if (retries > 1) {
                    calendarTitle = calendarTitle + " - " + retries;
                }
                page.setTitle(calendarTitle);
                page.setDescription(calendarTitle);
                DispatcherLogic.savePage(dir, page);
                final File actionFile = new File(dir, "action.groovy");
                try {
                    final TemplateEngine engine = (TemplateEngine)new SimpleTemplateEngine();
                    final Template template = engine.createTemplate(ApplicationWizard.class.getResource("CalendarPage.groovy"));
                    final Map<String, Object> bindings = new HashMap<String, Object>();
                    bindings.put("calendarDefinitions", calendarDefinitionsStr);
                    final FileWriter fw = new FileWriter(actionFile);
                    template.make((Map)bindings).writeTo((Writer)fw);
                    IOUtils.closeQuietly((Writer)fw);
                }
                catch (Exception e) {
                    ApplicationWizard.logger.warn("Couldn't create calendar", (Throwable)e);
                    SessionMessages.addWarningMessage("Couldn't create calendar: " + e);
                    return;
                }
                final ChildPage childPage = new ChildPage();
                childPage.setName(dir.getName());
                childPage.setShowInNavigation(true);
                childPages.add(childPage);
            }
            else {
                ApplicationWizard.logger.warn("Couldn't create directory {}", (Object)dir.getAbsolutePath());
                SessionMessages.addWarningMessage(ElementsThreadLocals.getText("couldnt.create.directory", new Object[] { dir.getAbsolutePath() }));
            }
        }
    }
    
    protected void setupUserPages(final List<ChildPage> childPages, final Template template) throws Exception {
        if (!this.roots.contains(this.userTable)) {
            final File dir = new File(this.pagesDir, this.userTable.getActualEntityName());
            this.depth = 1;
            this.createCrudPage(dir, this.userTable, childPages, template);
        }
        final Configuration conf = this.portofinoConfiguration;
        final List<Reference> references = (List<Reference>)this.children.get((Object)this.userTable);
        if (references != null) {
            for (final Reference ref : references) {
                this.depth = 1;
                final Column fromColumn = ref.getActualFromColumn();
                final Column toColumn = ref.getActualToColumn();
                final Table fromTable = fromColumn.getTable();
                final Table toTable = toColumn.getTable();
                final String entityName = fromTable.getActualEntityName();
                final List<Column> pkColumns = (List<Column>)toTable.getPrimaryKey().getColumns();
                if (!pkColumns.contains(toColumn)) {
                    continue;
                }
                final String linkToUserProperty = fromColumn.getActualPropertyName();
                final String childQuery = "from " + entityName + " where " + linkToUserProperty + " = %{#securityUtils.primaryPrincipal.id}" + " order by id desc";
                String dirName = "my-" + entityName;
                final boolean multipleRoles = this.isMultipleRoles(fromTable, ref, references);
                if (multipleRoles) {
                    dirName = dirName + "-as-" + linkToUserProperty;
                }
                final File dir2 = new File(this.pagesDir, dirName);
                final String title = Util.guessToWords(dirName);
                final Map<String, String> bindings = new HashMap<String, String>();
                bindings.put("parentName", "securityUtils");
                bindings.put("parentProperty", "primaryPrincipal.id");
                bindings.put("linkToParentProperty", linkToUserProperty);
                final Page page = this.createCrudPage(dir2, fromTable, childQuery, childPages, template, bindings, title);
                if (page == null) {
                    continue;
                }
                final Group group = new Group();
                group.setName(SecurityLogic.getAnonymousGroup(conf));
                group.setAccessLevel(AccessLevel.DENY.name());
                final Permissions permissions = new Permissions();
                permissions.getGroups().add(group);
                page.setPermissions(permissions);
                DispatcherLogic.savePage(dir2, page);
            }
        }
    }
    
    protected void setupUsers() {
        try {
            final TemplateEngine engine = (TemplateEngine)new SimpleTemplateEngine();
            final Template secTemplate = engine.createTemplate(ApplicationWizard.class.getResource("Security.groovy"));
            final Map<String, String> bindings = new HashMap<String, String>();
            bindings.put("databaseName", this.connectionProvider.getDatabase().getDatabaseName());
            bindings.put("userTableEntityName", this.userTable.getActualEntityName());
            bindings.put("userIdProperty", this.userIdProperty);
            bindings.put("userNameProperty", this.userNameProperty);
            bindings.put("passwordProperty", this.userPasswordProperty);
            bindings.put("userEmailProperty", this.userEmailProperty);
            bindings.put("userTokenProperty", this.userTokenProperty);
            bindings.put("groupTableEntityName", (this.groupTable != null) ? this.groupTable.getActualEntityName() : "");
            bindings.put("groupIdProperty", StringUtils.defaultString(this.groupIdProperty));
            bindings.put("groupNameProperty", StringUtils.defaultString(this.groupNameProperty));
            bindings.put("userGroupTableEntityName", (this.userGroupTable != null) ? this.userGroupTable.getActualEntityName() : "");
            bindings.put("groupLinkProperty", StringUtils.defaultString(this.groupLinkProperty));
            bindings.put("userLinkProperty", StringUtils.defaultString(this.userLinkProperty));
            bindings.put("adminGroupName", StringUtils.defaultString(this.adminGroupName));
            bindings.put("hashIterations", "1");
            final String[] algoAndEncoding = this.encryptionAlgorithm.split(":");
            bindings.put("hashAlgorithm", '\"' + algoAndEncoding[0] + '\"');
            if (algoAndEncoding[1].equals("plaintext")) {
                bindings.put("hashFormat", "null");
            }
            else if (algoAndEncoding[1].equals("hex")) {
                bindings.put("hashFormat", "new org.apache.shiro.crypto.hash.format.HexFormat()");
            }
            else if (algoAndEncoding[1].equals("base64")) {
                bindings.put("hashFormat", "new org.apache.shiro.crypto.hash.format.Base64Format()");
            }
            final File gcp = (File)this.context.getServletContext().getAttribute("GROOVY_CLASS_PATH");
            final FileWriter fw = new FileWriter(new File(gcp, "Security.groovy"));
            secTemplate.make((Map)bindings).writeTo((Writer)fw);
            IOUtils.closeQuietly((Writer)fw);
        }
        catch (Exception e) {
            ApplicationWizard.logger.warn("Couldn't configure users", (Throwable)e);
            SessionMessages.addWarningMessage(ElementsThreadLocals.getText("couldnt.set.up.user.management._", new Object[] { e }));
        }
    }
    
    private boolean isMultipleRoles(final Table fromTable, final Reference ref, final Collection<Reference> references) {
        boolean multipleRoles = false;
        for (final Reference ref2 : references) {
            if (ref2 != ref && ref2.getActualFromColumn().getTable().equals(fromTable)) {
                multipleRoles = true;
                break;
            }
        }
        return multipleRoles;
    }
    
    protected Page createCrudPage(final File dir, final Table table, final List<ChildPage> childPages, final Template template) throws Exception {
        final String query = "from " + table.getActualEntityName() + " order by id desc";
        final String title = Util.guessToWords(table.getActualEntityName());
        final HashMap<String, String> bindings = new HashMap<String, String>();
        bindings.put("parentName", "");
        bindings.put("parentProperty", "nothing");
        bindings.put("linkToParentProperty", ApplicationWizard.NO_LINK_TO_PARENT);
        return this.createCrudPage(dir, table, query, childPages, template, bindings, title);
    }
    
    protected Page createCrudPage(final File dir, final Table table, final String query, final List<ChildPage> childPages, final Template template, final Map<String, String> bindings, final String title) throws Exception {
        if (dir.exists()) {
            SessionMessages.addWarningMessage(ElementsThreadLocals.getText("directory.exists.page.not.created._", new Object[] { dir.getAbsolutePath() }));
            return null;
        }
        if (dir.mkdirs()) {
            ApplicationWizard.logger.info("Creating CRUD page {}", (Object)dir.getAbsolutePath());
            final CrudConfiguration configuration = new CrudConfiguration();
            configuration.setDatabase(this.connectionProvider.getDatabase().getDatabaseName());
            configuration.setupDefaults();
            configuration.setQuery(query);
            final String variable = table.getActualEntityName();
            configuration.setVariable(variable);
            this.detectLargeResultSet(table, configuration);
            configuration.setName(table.getActualEntityName());
            int summ = 0;
            final String linkToParentProperty = bindings.get("linkToParentProperty");
            for (final Column column : table.getColumns()) {
                summ = this.setupColumn(column, configuration, summ, linkToParentProperty);
            }
            DispatcherLogic.saveConfiguration(dir, (Object)configuration);
            final Page page = new Page();
            page.setId(RandomUtil.createRandomId());
            page.setTitle(title);
            page.setDescription(title);
            final Collection<Reference> references = (Collection<Reference>)this.children.get((Object)table);
            if (references != null && this.depth < this.maxDepth) {
                final ArrayList<ChildPage> pages = (ArrayList<ChildPage>)page.getDetailLayout().getChildPages();
                ++this.depth;
                for (final Reference ref : references) {
                    this.createChildCrudPage(dir, template, variable, references, ref, pages);
                }
                --this.depth;
                Collections.sort(pages, new Comparator<ChildPage>() {
                    @Override
                    public int compare(final ChildPage o1, final ChildPage o2) {
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    }
                });
            }
            DispatcherLogic.savePage(dir, page);
            final File actionFile = new File(dir, "action.groovy");
            final FileWriter fileWriter = new FileWriter(actionFile);
            template.make((Map)bindings).writeTo((Writer)fileWriter);
            IOUtils.closeQuietly((Writer)fileWriter);
            ApplicationWizard.logger.debug("Creating _detail directory");
            final File detailDir = new File(dir, "_detail");
            if (!detailDir.isDirectory() && !detailDir.mkdir()) {
                ApplicationWizard.logger.warn("Could not create detail directory {}", (Object)detailDir.getAbsolutePath());
                SessionMessages.addWarningMessage(ElementsThreadLocals.getText("couldnt.create.directory", new Object[] { detailDir.getAbsolutePath() }));
            }
            final ChildPage childPage = new ChildPage();
            childPage.setName(dir.getName());
            childPage.setShowInNavigation(true);
            childPages.add(childPage);
            return page;
        }
        ApplicationWizard.logger.warn("Couldn't create directory {}", (Object)dir.getAbsolutePath());
        SessionMessages.addWarningMessage(ElementsThreadLocals.getText("couldnt.create.directory", new Object[] { dir.getAbsolutePath() }));
        return null;
    }
    
    protected int setupColumn(final Column column, final CrudConfiguration configuration, int columnsInSummary, final String linkToParentProperty) {
        if (column.getActualJavaType() == null) {
            ApplicationWizard.logger.debug("Column without a javaType, skipping: {}", (Object)column.getQualifiedName());
            return columnsInSummary;
        }
        final Table table = column.getTable();
        final boolean enabled = (linkToParentProperty == ApplicationWizard.NO_LINK_TO_PARENT || !column.getActualPropertyName().equals(linkToParentProperty)) && !this.isUnsupportedProperty(column);
        final boolean propertyIsUserPassword = table.getQualifiedName().equals(this.userTableName) && column.getActualPropertyName().equals(this.userPasswordProperty);
        final boolean inPk = DatabaseLogic.isInPk(column);
        final boolean inFk = DatabaseLogic.isInFk(column);
        final boolean inSummary = enabled && (inPk || columnsInSummary < this.maxColumnsInSummary) && !propertyIsUserPassword;
        boolean updatable = enabled && !column.isAutoincrement() && !inPk;
        boolean insertable = enabled && !column.isAutoincrement();
        if (!configuration.isLargeResultSet()) {
            this.detectBooleanColumn(table, column);
        }
        if (enabled && inPk && !inFk && Number.class.isAssignableFrom(column.getActualJavaType()) && !column.isAutoincrement()) {
            for (final PrimaryKeyColumn pkc : table.getPrimaryKey().getPrimaryKeyColumns()) {
                if (pkc.getActualColumn().equals(column)) {
                    pkc.setGenerator((Generator)new IncrementGenerator(pkc));
                    insertable = false;
                    break;
                }
            }
        }
        if (propertyIsUserPassword) {
            final Annotation annotation = DatabaseLogic.findAnnotation((Annotated)column, (Class)Password.class);
            if (annotation == null) {
                column.getAnnotations().add(new Annotation((Object)column, Password.class.getName()));
            }
            insertable = false;
            updatable = false;
        }
        if (!propertyIsUserPassword && column.getActualJavaType() == String.class && (column.getLength() == null || column.getLength() > 256) && this.isNewConnectionProvider()) {
            Annotation annotation = DatabaseLogic.findAnnotation((Annotated)column, (Class)Multiline.class);
            if (annotation == null) {
                annotation = new Annotation((Object)column, Multiline.class.getName());
                annotation.getValues().add("true");
                column.getAnnotations().add(annotation);
            }
        }
        final CrudProperty crudProperty = new CrudProperty();
        crudProperty.setEnabled(enabled);
        crudProperty.setName(column.getActualPropertyName());
        crudProperty.setInsertable(insertable);
        crudProperty.setUpdatable(updatable);
        if (inSummary) {
            crudProperty.setInSummary(true);
            crudProperty.setSearchable(true);
            ++columnsInSummary;
        }
        configuration.getProperties().add(crudProperty);
        return columnsInSummary;
    }
    
    protected boolean isUnsupportedProperty(final Column column) {
        return column.getJdbcType() == 2004 || column.getJdbcType() == -4;
    }
    
    protected void detectBooleanColumn(final Table table, final Column column) {
        if (this.detectedBooleanColumns.contains(column)) {
            return;
        }
        if (column.getJdbcType() == 4 || column.getJdbcType() == 3 || column.getJdbcType() == 2) {
            ApplicationWizard.logger.info("Detecting whether numeric column " + column.getQualifiedName() + " is boolean by examining " + "its values...");
            Connection connection = null;
            try {
                connection = this.connectionProvider.acquireConnection();
                final liquibase.database.Database implementation = DatabaseFactory.getInstance().findCorrectDatabaseImplementation((DatabaseConnection)new JdbcConnection(connection));
                String sql = "select count(" + implementation.escapeColumnName((String)null, (String)null, (String)null, column.getColumnName()) + ") " + "from " + implementation.escapeTableName((String)null, table.getSchemaName(), table.getTableName());
                PreparedStatement statement = connection.prepareStatement(sql);
                this.setQueryTimeout(statement, 1);
                statement.setMaxRows(1);
                ResultSet rs = statement.executeQuery();
                Long count = null;
                if (rs.next()) {
                    count = safeGetLong(rs, 1);
                }
                if (count == null || count < 10L) {
                    ApplicationWizard.logger.info("Cannot determine if numeric column {} is boolean, count is {}", (Object)column.getQualifiedName(), (Object)count);
                    return;
                }
                sql = "select distinct(" + implementation.escapeColumnName((String)null, (String)null, (String)null, column.getColumnName()) + ") " + "from " + implementation.escapeTableName((String)null, table.getSchemaName(), table.getTableName());
                statement = connection.prepareStatement(sql);
                this.setQueryTimeout(statement, 1);
                statement.setMaxRows(3);
                rs = statement.executeQuery();
                int valueCount = 0;
                boolean only0and1 = true;
                while (rs.next()) {
                    if (++valueCount > 2) {
                        only0and1 = false;
                        break;
                    }
                    final Long value = safeGetLong(rs, 1);
                    only0and1 &= (value != null && (value == 0L || value == 1L));
                }
                if (only0and1 && valueCount == 2) {
                    ApplicationWizard.logger.info("Column appears to be of boolean type.");
                    column.setJavaType(Boolean.class.getName());
                }
                else {
                    ApplicationWizard.logger.info("Column appears not to be of boolean type.");
                }
                statement.close();
            }
            catch (Exception e) {
                ApplicationWizard.logger.debug("Could not determine whether column " + column.getQualifiedName() + " is boolean", (Throwable)e);
                ApplicationWizard.logger.info("Could not determine whether column " + column.getQualifiedName() + " is boolean");
                try {
                    if (connection != null) {
                        connection.close();
                    }
                }
                catch (SQLException e2) {
                    ApplicationWizard.logger.error("Could not close connection", (Throwable)e2);
                }
            }
            finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                }
                catch (SQLException e3) {
                    ApplicationWizard.logger.error("Could not close connection", (Throwable)e3);
                }
            }
            this.detectedBooleanColumns.add(column);
        }
    }
    
    protected void detectLargeResultSet(final Table table, final CrudConfiguration configuration) {
        final Boolean lrs = this.largeResultSet.get(table);
        if (lrs != null) {
            configuration.setLargeResultSet((boolean)lrs);
            return;
        }
        Connection connection = null;
        try {
            ApplicationWizard.logger.info("Trying to detect whether table {} has many records...", (Object)table.getQualifiedName());
            connection = this.connectionProvider.acquireConnection();
            final liquibase.database.Database implementation = DatabaseFactory.getInstance().findCorrectDatabaseImplementation((DatabaseConnection)new JdbcConnection(connection));
            final String sql = "select count(*) from " + implementation.escapeTableName((String)null, table.getSchemaName(), table.getTableName());
            final PreparedStatement statement = connection.prepareStatement(sql);
            this.setQueryTimeout(statement, 1);
            statement.setMaxRows(1);
            final ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                final Long count = safeGetLong(rs, 1);
                if (count != null) {
                    if (count > 10000L) {
                        ApplicationWizard.logger.info("Table " + table.getQualifiedName() + " currently has " + count + " rows, which is bigger than " + "the threshold (" + 10000 + ") for large result sets. It will be " + "marked as largeResultSet = true and no autodetection based on table data will be " + "attempted, in order to keep the processing time reasonable.");
                        configuration.setLargeResultSet(true);
                    }
                    else {
                        ApplicationWizard.logger.info("Table " + table.getQualifiedName() + " currently has " + count + " rows, which is smaller than " + "the threshold (" + 10000 + ") for large result sets. It will be " + "analyzed normally.");
                    }
                }
                else {
                    ApplicationWizard.logger.warn("Could not determine number of records, assuming large result set");
                    configuration.setLargeResultSet(true);
                }
            }
            statement.close();
        }
        catch (Exception e) {
            ApplicationWizard.logger.error("Could not determine count", (Throwable)e);
            try {
                if (connection != null) {
                    connection.close();
                }
            }
            catch (SQLException e2) {
                ApplicationWizard.logger.error("Could not close connection", (Throwable)e2);
            }
        }
        finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            }
            catch (SQLException e3) {
                ApplicationWizard.logger.error("Could not close connection", (Throwable)e3);
            }
        }
        this.largeResultSet.put(table, configuration.isLargeResultSet());
    }
    
    protected void setQueryTimeout(final PreparedStatement statement, final int seconds) {
        try {
            statement.setQueryTimeout(seconds);
        }
        catch (Exception e) {
            ApplicationWizard.logger.debug("setQueryTimeout not supported", (Throwable)e);
        }
    }
    
    public static Long safeGetLong(final ResultSet rs, final int index) throws SQLException {
        final Object object = rs.getObject(index);
        if (object instanceof Number) {
            return ((Number)object).longValue();
        }
        return null;
    }
    
    protected void createChildCrudPage(final File dir, final Template template, final String parentName, final Collection<Reference> references, final Reference ref, final ArrayList<ChildPage> pages) throws Exception {
        final Column fromColumn = ref.getActualFromColumn();
        final Table fromTable = fromColumn.getTable();
        final String entityName = fromTable.getActualEntityName();
        final String parentProperty = ref.getActualToColumn().getActualPropertyName();
        final String linkToParentProperty = fromColumn.getActualPropertyName();
        final String childQuery = "from " + entityName + " where " + linkToParentProperty + " = %{#" + parentName + "." + parentProperty + "}" + " order by id desc";
        String childDirName = entityName;
        final boolean multipleRoles = this.isMultipleRoles(fromTable, ref, references);
        if (multipleRoles) {
            childDirName = childDirName + "-as-" + linkToParentProperty;
        }
        final File childDir = new File(new File(dir, "_detail"), childDirName);
        final String childTitle = Util.guessToWords(childDirName);
        final Map<String, String> bindings = new HashMap<String, String>();
        bindings.put("parentName", parentName);
        bindings.put("parentProperty", parentProperty);
        bindings.put("linkToParentProperty", linkToParentProperty);
        this.createCrudPage(childDir, fromTable, childQuery, pages, template, bindings, childTitle);
    }
    
    public SelectField getConnectionProviderField() {
        return this.connectionProviderField;
    }
    
    public Form getJndiCPForm() {
        return this.jndiCPForm;
    }
    
    public Form getJdbcCPForm() {
        return this.jdbcCPForm;
    }
    
    public ConnectionProvider getConnectionProvider() {
        return this.connectionProvider;
    }
    
    public String getConnectionProviderName() {
        return this.connectionProviderName;
    }
    
    public void setConnectionProviderName(final String connectionProviderName) {
        this.connectionProviderName = connectionProviderName;
    }
    
    public boolean isJdbc() {
        return this.connectionProviderType == null || this.connectionProviderType.equals("JDBC");
    }
    
    public boolean isJndi() {
        return StringUtils.equals(this.connectionProviderType, "JNDI");
    }
    
    public String getConnectionProviderType() {
        return this.connectionProviderType;
    }
    
    public void setConnectionProviderType(final String connectionProviderType) {
        this.connectionProviderType = connectionProviderType;
    }
    
    public Form getConnectionProviderForm() {
        return this.connectionProviderForm;
    }
    
    public TableForm getSchemasForm() {
        return this.schemasForm;
    }
    
    public List<SelectableSchema> getSelectableSchemas() {
        return this.selectableSchemas;
    }
    
    @LabelI18N("users.table")
    public String getUserTableName() {
        return this.userTableName;
    }
    
    public void setUserTableName(final String userTableName) {
        this.userTableName = userTableName;
    }
    
    @LabelI18N("groups.table")
    public String getGroupTableName() {
        return this.groupTableName;
    }
    
    public void setGroupTableName(final String groupTableName) {
        this.groupTableName = groupTableName;
    }
    
    public Form getUserAndGroupTablesForm() {
        return this.userAndGroupTablesForm;
    }
    
    public Form getUserManagementSetupForm() {
        return this.userManagementSetupForm;
    }
    
    @LabelI18N("username.property")
    public String getUserNameProperty() {
        return this.userNameProperty;
    }
    
    public void setUserNameProperty(final String userNameProperty) {
        this.userNameProperty = userNameProperty;
    }
    
    @LabelI18N("email.property")
    public String getUserEmailProperty() {
        return this.userEmailProperty;
    }
    
    public void setUserEmailProperty(final String userEmailProperty) {
        this.userEmailProperty = userEmailProperty;
    }
    
    @LabelI18N("token.property")
    public String getUserTokenProperty() {
        return this.userTokenProperty;
    }
    
    public void setUserTokenProperty(final String userTokenProperty) {
        this.userTokenProperty = userTokenProperty;
    }
    
    @LabelI18N("user.id.property")
    public String getUserIdProperty() {
        return this.userIdProperty;
    }
    
    public void setUserIdProperty(final String userIdProperty) {
        this.userIdProperty = userIdProperty;
    }
    
    @LabelI18N("password.property")
    public String getUserPasswordProperty() {
        return this.userPasswordProperty;
    }
    
    public void setUserPasswordProperty(final String userPasswordProperty) {
        this.userPasswordProperty = userPasswordProperty;
    }
    
    public String getGroupIdProperty() {
        return this.groupIdProperty;
    }
    
    public void setGroupIdProperty(final String groupIdProperty) {
        this.groupIdProperty = groupIdProperty;
    }
    
    @LabelI18N("user-group.join.table")
    public String getUserGroupTableName() {
        return this.userGroupTableName;
    }
    
    public void setUserGroupTableName(final String userGroupTableName) {
        this.userGroupTableName = userGroupTableName;
    }
    
    public String getGroupNameProperty() {
        return this.groupNameProperty;
    }
    
    public void setGroupNameProperty(final String groupNameProperty) {
        this.groupNameProperty = groupNameProperty;
    }
    
    @LabelI18N("property.that.links.to.group")
    public String getGroupLinkProperty() {
        return this.groupLinkProperty;
    }
    
    public void setGroupLinkProperty(final String groupLinkProperty) {
        this.groupLinkProperty = groupLinkProperty;
    }
    
    @LabelI18N("property.that.links.to.user")
    public String getUserLinkProperty() {
        return this.userLinkProperty;
    }
    
    public void setUserLinkProperty(final String userLinkProperty) {
        this.userLinkProperty = userLinkProperty;
    }
    
    @LabelI18N("name.of.the.administrators.group")
    public String getAdminGroupName() {
        return this.adminGroupName;
    }
    
    public void setAdminGroupName(final String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }
    
    @LabelI18N("password.encryption.algorithm")
    public String getEncryptionAlgorithm() {
        return this.encryptionAlgorithm;
    }
    
    public void setEncryptionAlgorithm(final String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }
    
    public String getGenerationStrategy() {
        return this.generationStrategy;
    }
    
    public void setGenerationStrategy(final String generationStrategy) {
        this.generationStrategy = generationStrategy;
    }
    
    public boolean isGenerateCalendar() {
        return this.generateCalendar;
    }
    
    public void setGenerateCalendar(final boolean generateCalendar) {
        this.generateCalendar = generateCalendar;
    }
    
    public BooleanField getGenerateCalendarField() {
        return this.generateCalendarField;
    }
    
    public Persistence getPersistence() {
        return this.persistence;
    }
    
    public Resolution preparePage() {
        return null;
    }
    
    public List<Step> getSteps() {
        final List<Step> steps = new ArrayList<Step>();
        steps.add(new Step("1", ElementsThreadLocals.getText("connect.to.your.database", new Object[0])));
        steps.add(new Step("2", ElementsThreadLocals.getText("select.the.database.schemas.to.import", new Object[0])));
        steps.add(new Step("3", ElementsThreadLocals.getText("set.up.user.management", new Object[0])));
        if (this.userTable != null) {
            steps.add(new Step("3a", ElementsThreadLocals.getText("customize.user.management", new Object[0])));
        }
        steps.add(new Step("4", ElementsThreadLocals.getText("generate.pages", new Object[0])));
        steps.add(new Step("5", ElementsThreadLocals.getText("build.the.application", new Object[0])));
        return steps;
    }
    
    public int getCurrentStepIndex() {
        return this.step;
    }
    
    static {
        NO_LINK_TO_PARENT = new String();
        JDBC_CP_FIELDS = new String[] { "databaseName", "driver", "url", "username", "password" };
        JNDI_CP_FIELDS = new String[] { "databaseName", "jndiResource" };
        logger = LoggerFactory.getLogger((Class)ApplicationWizard.class);
    }
    
    public static class Step
    {
        public final String number;
        public final String title;
        
        public Step(final String number, final String title) {
            this.number = number;
            this.title = title;
        }
        
        public String getNumber() {
            return this.number;
        }
        
        public String getTitle() {
            return this.title;
        }
    }
}
