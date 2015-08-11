package org.remata.portofolio.actions.admin.database;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.remata.portofolio.persistence.*;
import org.remata.portofolio.di.*;
import org.apache.commons.configuration.*;
import org.remata.portofolio.actions.admin.database.forms.*;
import org.remata.elements.text.*;
import java.util.*;
import org.remata.portofolio.database.platforms.*;
import org.remata.elements.forms.*;
import java.sql.*;
import org.remata.elements.*;
import org.remata.elements.messages.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.actions.admin.appwizard.*;
import org.remata.elements.configuration.*;
import org.apache.commons.lang.exception.*;
import org.remata.portofolio.buttons.annotations.*;
import org.remata.portofolio.model.database.*;
import org.slf4j.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/connection-providers")
public class ConnectionProvidersAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    public static final String URL_BINDING = "/actions/admin/connection-providers";
    public ConnectionProvider connectionProvider;
    public DatabasePlatform[] databasePlatforms;
    public DatabasePlatform databasePlatform;
    protected ConnectionProviderForm connectionProviderForm;
    public TableForm tableForm;
    public Form form;
    public Form detectedValuesForm;
    public TableForm schemasForm;
    public TableForm databasePlatformsTableForm;
    public String databaseName;
    public String[] selection;
    protected List<SelectableSchema> selectableSchemas;
    @Inject("org.remata.portofolio.modules.DatabaseModule.persistence")
    Persistence persistence;
    @Inject("portofinoConfiguration")
    Configuration configuration;
    public static final Logger logger;
    public static final String[] jdbcViewFields;
    public static final String[] jdbcEditFields;
    public static final String[] jndiViewFields;
    public static final String[] jndiEditFields;
    
    @DefaultHandler
    public Resolution execute() {
        if (this.databaseName == null) {
            return this.search();
        }
        return this.read();
    }
    
    public Resolution search() {
        final OgnlTextFormat hrefFormat = OgnlTextFormat.create("/actions/admin/connection-providers?databaseName=%{databaseName}");
        hrefFormat.setUrl(true);
        (this.tableForm = new TableFormBuilder((Class)ConnectionProviderTableForm.class).configFields(new String[] { "databaseName", "description", "status" }).configNRows(this.persistence.getModel().getDatabases().size()).configHrefTextFormat("databaseName", (TextFormat)hrefFormat).configMode(Mode.VIEW).build()).setSelectable(true);
        this.tableForm.setKeyGenerator((TextFormat)OgnlTextFormat.create("%{databaseName}"));
        final List<ConnectionProviderTableForm> tableFormObj = new ArrayList<ConnectionProviderTableForm>();
        for (final Database database : this.persistence.getModel().getDatabases()) {
            final ConnectionProvider connectionProvider = database.getConnectionProvider();
            tableFormObj.add(new ConnectionProviderTableForm(database.getDatabaseName(), connectionProvider.getDescription(), connectionProvider.getStatus()));
        }
        this.tableForm.readFromObject((Object)tableFormObj);
        final DatabasePlatformsRegistry manager = this.persistence.getDatabasePlatformsRegistry();
        this.databasePlatforms = manager.getDatabasePlatforms();
        (this.databasePlatformsTableForm = new TableFormBuilder((Class)DatabasePlatform.class).configFields(new String[] { "description", "standardDriverClassName", "status" }).configNRows(this.databasePlatforms.length).configMode(Mode.VIEW).build()).readFromObject((Object)this.databasePlatforms);
        return (Resolution)new ForwardResolution("/m/admin/connectionProviders/list.jsp");
    }
    
    public Resolution read() {
        this.connectionProvider = this.persistence.getConnectionProvider(this.databaseName);
        this.databasePlatform = this.connectionProvider.getDatabasePlatform();
        this.connectionProviderForm = new ConnectionProviderForm(this.connectionProvider.getDatabase());
        this.buildConnectionProviderForm(Mode.VIEW);
        this.form.readFromObject((Object)this.connectionProviderForm);
        if ("connected".equals(this.connectionProvider.getStatus())) {
            this.configureDetected();
        }
        return (Resolution)new ForwardResolution("/m/admin/connectionProviders/read.jsp");
    }
    
    private void buildConnectionProviderForm(final Mode mode) {
        String[] fields;
        if (this.connectionProvider instanceof JdbcConnectionProvider) {
            fields = ((mode == Mode.VIEW) ? ConnectionProvidersAction.jdbcViewFields : ConnectionProvidersAction.jdbcEditFields);
        }
        else {
            if (!(this.connectionProvider instanceof JndiConnectionProvider)) {
                throw new InternalError("Unknown connection provider type: " + this.connectionProvider.getClass().getName());
            }
            fields = ((mode == Mode.VIEW) ? ConnectionProvidersAction.jndiViewFields : ConnectionProvidersAction.jndiEditFields);
        }
        this.form = new FormBuilder((Class)ConnectionProviderForm.class).configFields(fields).configMode(mode).build();
    }
    
    protected void configureDetected() {
        (this.detectedValuesForm = new FormBuilder((Class)JdbcConnectionProvider.class).configFields(new String[] { "databaseProductName", "databaseProductVersion", "databaseMajorMinorVersion", "driverName", "driverVersion", "driverMajorMinorVersion", "JDBCMajorMinorVersion" }).configMode(Mode.VIEW).build()).readFromObject((Object)this.connectionProvider);
    }
    
    protected void configureEditSchemas() {
        try {
            final Connection conn = this.connectionProvider.acquireConnection();
            ConnectionProvidersAction.logger.debug("Reading database metadata");
            final DatabaseMetaData metadata = conn.getMetaData();
            final List<String> schemaNamesFromDb = (List<String>)this.connectionProvider.getDatabasePlatform().getSchemaNames(metadata);
            this.connectionProvider.releaseConnection(conn);
            final List<Schema> selectedSchemas = (List<Schema>)this.connectionProvider.getDatabase().getSchemas();
            this.selectableSchemas = new ArrayList<SelectableSchema>(schemaNamesFromDb.size());
            for (final String schemaName : schemaNamesFromDb) {
                boolean selected = false;
                for (final Schema schema : selectedSchemas) {
                    if (schemaName.equalsIgnoreCase(schema.getSchemaName())) {
                        selected = true;
                        break;
                    }
                }
                final SelectableSchema schema2 = new SelectableSchema(schemaName, selected);
                this.selectableSchemas.add(schema2);
            }
            (this.schemasForm = new TableFormBuilder((Class)SelectableSchema.class).configFields(new String[] { "selected", "schemaName" }).configMode(Mode.EDIT).configNRows(this.selectableSchemas.size()).build()).readFromObject((Object)this.selectableSchemas);
        }
        catch (Exception e) {
            ConnectionProvidersAction.logger.error("Coulnd't read schema names from db", (Throwable)e);
        }
    }
    
    @Button(list = "connectionProviders-read", key = "test", order = 3.0)
    public Resolution test() {
        (this.connectionProvider = this.persistence.getConnectionProvider(this.databaseName)).init(this.persistence.getDatabasePlatformsRegistry());
        final String status = this.connectionProvider.getStatus();
        if ("connected".equals(status)) {
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("connection.tested.successfully", new Object[0]));
        }
        else {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("connection.failed.status._.error.message._", new Object[] { status, this.connectionProvider.getErrorMessage() }));
        }
        return (Resolution)new RedirectResolution((Class)this.getClass()).addParameter("databaseName", new Object[] { this.databaseName });
    }
    
    @Button(list = "connectionProviders-search", key = "create.new", order = 1.0)
    public Resolution create() {
        return (Resolution)new RedirectResolution((Class)ApplicationWizard.class);
    }
    
    @Button(list = "connectionProviders-read", key = "edit", order = 2.0)
    public Resolution edit() {
        this.connectionProvider = this.persistence.getConnectionProvider(this.databaseName);
        this.databasePlatform = this.connectionProvider.getDatabasePlatform();
        this.connectionProviderForm = new ConnectionProviderForm(this.connectionProvider.getDatabase());
        this.buildConnectionProviderForm(Mode.EDIT);
        this.form.readFromObject((Object)this.connectionProviderForm);
        this.configureEditSchemas();
        return (Resolution)new ForwardResolution("/m/admin/connectionProviders/edit.jsp");
    }
    
    @Button(list = "connectionProviders-edit", key = "update", order = 1.0, type = " btn-primary ")
    public Resolution update() {
        this.connectionProvider = this.persistence.getConnectionProvider(this.databaseName);
        this.databasePlatform = this.connectionProvider.getDatabasePlatform();
        final Database database = this.connectionProvider.getDatabase();
        this.connectionProviderForm = new ConnectionProviderForm(database);
        this.buildConnectionProviderForm(Mode.EDIT);
        this.form.readFromObject((Object)this.connectionProviderForm);
        this.form.readFromRequest(this.context.getRequest());
        this.configureEditSchemas();
        boolean schemasValid = true;
        if (this.schemasForm != null) {
            this.schemasForm.readFromRequest(this.context.getRequest());
            schemasValid = this.schemasForm.validate();
        }
        if (this.form.validate() && schemasValid) {
            if (this.schemasForm != null) {
                this.schemasForm.writeToObject((Object)this.selectableSchemas);
                final List<Schema> selectedSchemas = (List<Schema>)database.getSchemas();
                final List<String> selectedSchemaNames = new ArrayList<String>(selectedSchemas.size());
                for (final Schema schema : selectedSchemas) {
                    selectedSchemaNames.add(schema.getSchemaName().toLowerCase());
                }
                for (final SelectableSchema schema2 : this.selectableSchemas) {
                    if (schema2.selected && !selectedSchemaNames.contains(schema2.schemaName.toLowerCase())) {
                        final Schema modelSchema = new Schema();
                        modelSchema.setSchemaName(schema2.schemaName);
                        modelSchema.setDatabase(database);
                        database.getSchemas().add(modelSchema);
                    }
                    else {
                        if (schema2.selected || !selectedSchemaNames.contains(schema2.schemaName.toLowerCase())) {
                            continue;
                        }
                        Schema toBeRemoved = null;
                        for (final Schema aSchema : database.getSchemas()) {
                            if (aSchema.getSchemaName().equalsIgnoreCase(schema2.schemaName)) {
                                toBeRemoved = aSchema;
                                break;
                            }
                        }
                        if (toBeRemoved == null) {
                            continue;
                        }
                        database.getSchemas().remove(toBeRemoved);
                    }
                }
            }
            this.form.writeToObject((Object)this.connectionProviderForm);
            try {
                this.connectionProvider.init(this.persistence.getDatabasePlatformsRegistry());
                this.persistence.initModel();
                this.persistence.saveXmlModel();
                CommonsConfigurationUtils.save(this.configuration);
                SessionMessages.addInfoMessage(ElementsThreadLocals.getText("connection.provider.updated.successfully", new Object[0]));
            }
            catch (Exception e) {
                final String msg = "Cannot save model: " + ExceptionUtils.getRootCauseMessage((Throwable)e);
                SessionMessages.addErrorMessage(msg);
                ConnectionProvidersAction.logger.error(msg, (Throwable)e);
            }
            return (Resolution)new RedirectResolution((Class)this.getClass()).addParameter("databaseName", new Object[] { this.databaseName });
        }
        return (Resolution)new ForwardResolution("/m/admin/connectionProviders/edit.jsp");
    }
    
    @Buttons({ @Button(list = "connectionProviders-edit", key = "cancel", order = 2.0), @Button(list = "connectionProviders-create", key = "cancel", order = 2.0) })
    public Resolution cancel() {
        return this.execute();
    }
    
    @Button(list = "connectionProviders-read", key = "delete", order = 6.0)
    public Resolution delete() {
        final String[] databaseNames = { this.databaseName };
        try {
            this.doDelete(databaseNames);
            this.persistence.initModel();
            this.persistence.saveXmlModel();
        }
        catch (Exception e) {
            final String msg = "Cannot save model: " + ExceptionUtils.getRootCauseMessage((Throwable)e);
            ConnectionProvidersAction.logger.error(msg, (Throwable)e);
            SessionMessages.addErrorMessage(msg);
        }
        return (Resolution)new RedirectResolution((Class)this.getClass());
    }
    
    @Button(list = "connectionProviders-search", key = "delete", order = 2.0)
    public Resolution bulkDelete() {
        if (null != this.selection && 0 != this.selection.length) {
            try {
                this.doDelete(this.selection);
                this.persistence.initModel();
                this.persistence.saveXmlModel();
            }
            catch (Exception e) {
                final String msg = "Cannot save model: " + ExceptionUtils.getRootCauseMessage((Throwable)e);
                ConnectionProvidersAction.logger.error(msg, (Throwable)e);
                SessionMessages.addErrorMessage(msg);
            }
        }
        else {
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("no.connection.providers.selected", new Object[0]));
        }
        return (Resolution)new RedirectResolution((Class)this.getClass());
    }
    
    protected void doDelete(final String[] databaseNames) {
        for (final String current : databaseNames) {
            if (current != null) {
                final Database database = DatabaseLogic.findDatabaseByName(this.persistence.getModel(), current);
                if (database == null) {
                    SessionMessages.addWarningMessage("Delete failed. Connection provider not found: " + current);
                }
                else {
                    this.persistence.getModel().getDatabases().remove(database);
                    SessionMessages.addInfoMessage("Connection provider deleted successfully: " + current);
                }
            }
        }
    }
    
    @Button(list = "connectionProviders-read", key = "synchronize", order = 4.0)
    public Resolution sync() {
        try {
            this.persistence.syncDataModel(this.databaseName);
            this.persistence.initModel();
            this.persistence.saveXmlModel();
            SessionMessages.addInfoMessage("Connection provider synchronized correctly");
        }
        catch (Exception e) {
            ConnectionProvidersAction.logger.error("Errore in sincronizzazione", (Throwable)e);
            SessionMessages.addErrorMessage("Synchronization error: " + ExceptionUtils.getRootCauseMessage((Throwable)e));
        }
        return (Resolution)new RedirectResolution((Class)this.getClass()).addParameter("databaseName", new Object[] { this.databaseName });
    }
    
    @Button(list = "connectionProviders-read", key = "run.wizard", order = 5.0)
    public Resolution runWizard() {
        final ConnectionProvider connectionProvider = this.persistence.getConnectionProvider(this.databaseName);
        return (Resolution)((RedirectResolution)((RedirectResolution)new RedirectResolution((Class)ApplicationWizard.class).addParameter("connectionProviderName", new Object[] { this.databaseName })).addParameter("configureConnectionProvider", new Object[0])).addParameter("connectionProviderType", new Object[] { (connectionProvider instanceof JdbcConnectionProvider) ? "JDBC" : "JNDI" });
    }
    
    @Buttons({ @Button(list = "connectionProviders-read", key = "return.to.list", order = 1.0), @Button(list = "connectionProviders-select-type-content-buttons", key = "return.to.list", order = 1.0) })
    public Resolution returnToList() {
        return (Resolution)new RedirectResolution((Class)ConnectionProvidersAction.class);
    }
    
    @Button(list = "connectionProviders-search", key = "return.to.pages", order = 3.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    public String getDatabaseName() {
        return this.databaseName;
    }
    
    public void setDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
    }
    
    public Form getDetectedValuesForm() {
        return this.detectedValuesForm;
    }
    
    public TableForm getSchemasForm() {
        return this.schemasForm;
    }
    
    public ConnectionProvider getConnectionProvider() {
        return this.connectionProvider;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)ConnectionProvidersAction.class);
        jdbcViewFields = new String[] { "databaseName", "driver", "url", "username", "password", "status", "errorMessage", "lastTested" };
        jdbcEditFields = new String[] { "databaseName", "driver", "url", "username", "password", "hibernateDialect", "trueString", "falseString" };
        jndiViewFields = new String[] { "databaseName", "jndiResource", "status", "errorMessage", "lastTested" };
        jndiEditFields = new String[] { "databaseName", "jndiResource", "hibernateDialect", "trueString", "falseString" };
    }
}
