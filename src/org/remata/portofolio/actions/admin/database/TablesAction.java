package org.remata.portofolio.actions.admin.database;

import org.remata.portofolio.stripes.*;
import org.apache.shiro.authz.annotation.*;
import org.remata.portofolio.security.*;
import org.remata.portofolio.di.*;
import org.remata.elements.*;
import org.remata.elements.messages.*;
import net.sourceforge.stripes.action.*;
import org.remata.portofolio.actions.admin.tables.forms.*;
import org.remata.portofolio.buttons.annotations.*;
import org.remata.portofolio.buttons.*;
import org.remata.portofolio.logic.*;
import org.remata.elements.text.*;
import java.util.*;
import org.remata.portofolio.model.database.*;
import java.io.*;
import javax.xml.bind.*;
import java.text.*;
import org.remata.portofolio.reflection.*;
import org.remata.elements.options.*;
import org.remata.elements.forms.*;
import org.remata.portofolio.database.*;
import org.remata.elements.reflection.*;
import org.remata.elements.fields.*;
import org.apache.commons.lang.*;
import java.math.*;
import java.sql.*;
import org.remata.portofolio.persistence.*;
import org.remata.portofolio.model.*;
import org.remata.elements.annotations.*;
import org.slf4j.*;

@RequiresAuthentication
@RequiresAdministrator
@UrlBinding("/actions/admin/tables/{databaseName}/{schemaName}/{tableName}/{columnName}")
public class TablesAction extends AbstractActionBean
{
    public static final String copyright = "Copyright (c) 2005-2015, ManyDesigns srl";
    public static final String BASE_ACTION_PATH = "/actions/admin/tables";
    @Inject("org.remata.portofolio.modules.DatabaseModule.persistence")
    public Persistence persistence;
    protected String databaseName;
    protected String schemaName;
    protected String tableName;
    protected String columnName;
    protected String returnUrl;
    protected String shortName;
    protected String selectionProviderName;
    protected final Map<String, String> fkOnePropertyNames;
    protected final Map<String, String> fkManyPropertyNames;
    protected Table table;
    protected Column column;
    protected List<String> sortedColumnNames;
    protected List<ColumnForm> decoratedColumns;
    protected DatabaseSelectionProvider databaseSelectionProvider;
    protected Form tableForm;
    protected TableForm columnsTableForm;
    protected Form columnForm;
    protected Field shortNameField;
    protected Form dbSelectionProviderForm;
    protected String selectedTabId;
    public static final Logger logger;
    
    public TablesAction() {
        this.fkOnePropertyNames = new HashMap<String, String>();
        this.fkManyPropertyNames = new HashMap<String, String>();
    }
    
    @DefaultHandler
    public Resolution execute() {
        if (this.tableName == null) {
            return this.search();
        }
        if (this.columnName == null) {
            return this.editTable();
        }
        return this.editColumn();
    }
    
    public Resolution search() {
        return (Resolution)new ForwardResolution("/m/admin/tables/list.jsp");
    }
    
    public Resolution editTable() {
        this.setupTableForm(Mode.EDIT);
        this.setupColumnsForm(Mode.EDIT);
        this.tableForm.readFromRequest(this.context.getRequest());
        for (final ForeignKey fk : this.table.getForeignKeys()) {
            this.fkOnePropertyNames.put(fk.getName(), fk.getOnePropertyName());
            this.fkManyPropertyNames.put(fk.getName(), fk.getManyPropertyName());
        }
        return (Resolution)new ForwardResolution("/m/admin/tables/edit-table.jsp");
    }
    
    public Resolution editColumn() {
        this.setupTableForm(Mode.HIDDEN);
        this.tableForm.readFromRequest(this.context.getRequest());
        this.setupColumnForm();
        return (Resolution)new ForwardResolution("/m/admin/tables/edit-column.jsp");
    }
    
    @Button(key = "save", list = "table-edit", order = 1.0, type = " btn-primary ")
    public Resolution saveTable() {
        final org.remata.portofolio.actions.admin.tables.forms.TableForm tf = this.setupTableForm(Mode.EDIT);
        this.setupColumnsForm(Mode.EDIT);
        this.tableForm.readFromRequest(this.context.getRequest());
        this.columnsTableForm.readFromRequest(this.context.getRequest());
        if (this.validateTableForm() && this.columnsTableForm.validate()) {
            this.tableForm.writeToObject((Object)tf);
            tf.copyTo(this.table);
            this.table.setEntityName(StringUtils.defaultIfEmpty(this.table.getEntityName(), (String)null));
            this.table.setJavaClass(StringUtils.defaultIfEmpty(this.table.getJavaClass(), (String)null));
            this.table.setShortName(StringUtils.defaultIfEmpty(this.table.getShortName(), (String)null));
            this.columnsTableForm.writeToObject((Object)this.decoratedColumns);
            for (final Column column : this.table.getColumns()) {
                for (final ColumnForm columnForm : this.decoratedColumns) {
                    if (columnForm.getColumnName().equals(column.getColumnName())) {
                        columnForm.copyTo(column);
                    }
                }
            }
            Collections.sort((List<Object>)this.table.getColumns(), (Comparator<? super Object>)new Comparator<Column>() {
                @Override
                public int compare(final Column o1, final Column o2) {
                    final int i1 = TablesAction.this.sortedColumnNames.indexOf(o1.getColumnName());
                    final int i2 = TablesAction.this.sortedColumnNames.indexOf(o2.getColumnName());
                    return Integer.valueOf(i1).compareTo(Integer.valueOf(i2));
                }
            });
            for (final ForeignKey fk : this.table.getForeignKeys()) {
                fk.setOnePropertyName((String)this.fkOnePropertyNames.get(fk.getName()));
                fk.setManyPropertyName((String)this.fkManyPropertyNames.get(fk.getName()));
            }
            try {
                this.saveModel();
                for (final Table otherTable : this.table.getSchema().getTables()) {
                    for (final ForeignKey fk2 : otherTable.getForeignKeys()) {
                        if (fk2.getFromTable().equals(this.table) || (!fk2.getFromTable().equals(this.table) && fk2.getToTable().equals(this.table))) {
                            for (final Reference ref : fk2.getReferences()) {
                                final Column fromColumn = ref.getActualFromColumn();
                                final Column toColumn = ref.getActualToColumn();
                                if (fromColumn.getActualJavaType() != toColumn.getActualJavaType()) {
                                    SessionMessages.addWarningMessage(ElementsThreadLocals.getText("detected.type.mismatch.between.column._.and.column._", new Object[] { fromColumn.getQualifiedName(), fromColumn.getActualJavaType().getName(), toColumn.getQualifiedName(), toColumn.getActualJavaType().getName(), fk2.getName() }));
                                }
                            }
                        }
                    }
                }
                SessionMessages.consumeWarningMessages();
            }
            catch (Exception e) {
                TablesAction.logger.error("Could not save model", (Throwable)e);
                SessionMessages.addErrorMessage(e.toString());
            }
            return (Resolution)((RedirectResolution)((RedirectResolution)new RedirectResolution((Class)TablesAction.class, "editTable").addParameter("databaseName", new Object[] { this.databaseName })).addParameter("schemaName", new Object[] { this.schemaName })).addParameter("tableName", new Object[] { this.tableName });
        }
        return (Resolution)new ForwardResolution("/m/admin/tables/edit-table.jsp");
    }
    
    protected boolean validateTableForm() {
        if (this.tableForm.validate()) {
            final Field javaClassField = this.tableForm.findFieldByPropertyName("javaClass");
            final String javaClass = javaClassField.getStringValue();
            if (!StringUtils.isBlank(javaClass)) {
                try {
                    final ClassLoader classLoader = (ClassLoader)this.context.getServletContext().getAttribute("org.remata.portofolio.application.classLoader");
                    Class.forName(javaClass, true, classLoader);
                }
                catch (ClassNotFoundException e) {
                    javaClassField.getErrors().add(ElementsThreadLocals.getText("class.not.found._", new Object[0]));
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    @Button(key = "cancel", list = "table-edit", order = 2.0)
    public Resolution returnToTables() {
        return (Resolution)new RedirectResolution("/actions/admin/tables");
    }
    
    @Button(key = "save", list = "column-edit", order = 1.0, type = " btn-primary ")
    public Resolution saveColumn() {
        this.setupTableForm(Mode.HIDDEN);
        this.tableForm.readFromRequest(this.context.getRequest());
        final ColumnForm cf = this.setupColumnForm();
        this.columnForm.readFromRequest(this.context.getRequest());
        if (this.saveToColumnForm(this.columnForm, cf)) {
            cf.copyTo(this.column);
            try {
                this.saveModel();
                for (final Table otherTable : this.table.getSchema().getTables()) {
                    for (final ForeignKey fk : otherTable.getForeignKeys()) {
                        for (final Reference ref : fk.getReferences()) {
                            final Column fromColumn = ref.getActualFromColumn();
                            final Column toColumn = ref.getActualToColumn();
                            if ((fromColumn.equals(this.column) || toColumn.equals(this.column)) && fromColumn.getActualJavaType() != toColumn.getActualJavaType()) {
                                Column otherColumn;
                                if (fromColumn.equals(this.column)) {
                                    otherColumn = toColumn;
                                }
                                else {
                                    otherColumn = fromColumn;
                                }
                                SessionMessages.addWarningMessage(ElementsThreadLocals.getText("detected.type.mismatch.with.column._", new Object[] { otherColumn.getQualifiedName(), otherColumn.getActualJavaType().getName(), fk.getName() }));
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                TablesAction.logger.error("Could not save model", (Throwable)e);
                SessionMessages.addErrorMessage(e.toString());
            }
        }
        this.setupColumnForm();
        this.columnForm.readFromRequest(this.context.getRequest());
        this.saveToColumnForm(this.columnForm, cf);
        return (Resolution)((RedirectResolution)((RedirectResolution)((RedirectResolution)new RedirectResolution((Class)TablesAction.class, "editColumn").addParameter("databaseName", new Object[] { this.databaseName })).addParameter("schemaName", new Object[] { this.schemaName })).addParameter("tableName", new Object[] { this.tableName })).addParameter("columnName", new Object[] { this.columnName });
    }
    
    @Buttons({ @Button(key = "cancel", list = "column-edit", order = 2.0), @Button(key = "cancel", list = "table-short-name", order = 2.0), @Button(key = "cancel", list = "table-selection-provider", order = 3.0) })
    public Resolution returnToTable() {
        final RedirectResolution resolution = new RedirectResolution("/actions/admin/tables/" + this.databaseName + "/" + this.schemaName + "/" + this.tableName);
        resolution.addParameter("selectedTabId", new Object[] { this.selectedTabId });
        return (Resolution)resolution;
    }
    
    @Button(key = "layouts.admin.tables.addSelectionProvider", list = "table-selection-providers")
    public Resolution addSelectionProvider() {
        this.table = this.findTable();
        this.databaseSelectionProvider = new DatabaseSelectionProvider(this.table);
        final DatabaseSelectionProviderForm databaseSelectionProviderForm = this.setupDbSelectionProviderForm(Mode.CREATE);
        return this.doEditSelectionProvider(databaseSelectionProviderForm);
    }
    
    @Button(key = "delete", list = "table-selection-provider", order = 2.0)
    @Guard(test = "getSelectionProviderName() != null", type = GuardType.VISIBLE)
    public Resolution removeSelectionProvider() {
        this.table = this.findTable();
        final ModelSelectionProvider sp = DatabaseLogic.findSelectionProviderByName(this.table, this.selectionProviderName);
        this.table.getSelectionProviders().remove(sp);
        try {
            this.saveModel();
        }
        catch (Exception e) {
            TablesAction.logger.error("Could not save model", (Throwable)e);
            SessionMessages.addErrorMessage(e.toString());
        }
        return this.editTable();
    }
    
    public Resolution editSelectionProvider() {
        this.table = this.findTable();
        this.databaseSelectionProvider = (DatabaseSelectionProvider)DatabaseLogic.findSelectionProviderByName(this.table, this.selectionProviderName);
        final DatabaseSelectionProviderForm databaseSelectionProviderForm = this.setupDbSelectionProviderForm(Mode.CREATE);
        return this.doEditSelectionProvider(databaseSelectionProviderForm);
    }
    
    protected Resolution doEditSelectionProvider(final DatabaseSelectionProviderForm databaseSelectionProviderForm) {
        this.setupTableForm(Mode.HIDDEN);
        this.tableForm.readFromRequest(this.context.getRequest());
        return (Resolution)new ForwardResolution("/m/admin/tables/edit-db-selection-provider.jsp");
    }
    
    protected DatabaseSelectionProviderForm setupDbSelectionProviderForm(final Mode mode) {
        final SelectionProvider databaseChooser = (SelectionProvider)SelectionProviderLogic.createSelectionProvider("database", (Collection)this.persistence.getModel().getDatabases(), (Class)Database.class, (TextFormat[])null, new String[] { "databaseName" });
        this.dbSelectionProviderForm = new FormBuilder((Class)DatabaseSelectionProviderForm.class).configFields(new String[] { "name", "toDatabase", "hql", "sql", "columns" }).configSelectionProvider(databaseChooser, new String[] { "toDatabase" }).configMode(mode).build();
        final DatabaseSelectionProviderForm databaseSelectionProviderForm = new DatabaseSelectionProviderForm(this.databaseSelectionProvider);
        final List<String> refCols = new ArrayList<String>();
        for (final Reference ref : this.databaseSelectionProvider.getReferences()) {
            refCols.add(ref.getFromColumn());
        }
        databaseSelectionProviderForm.setColumns(StringUtils.join((Collection)refCols, ", "));
        this.dbSelectionProviderForm.readFromObject((Object)databaseSelectionProviderForm);
        return databaseSelectionProviderForm;
    }
    
    @Button(key = "save", list = "table-selection-provider", order = 1.0, type = " btn-primary ")
    public Resolution saveSelectionProvider() {
        this.table = this.findTable();
        final Mode mode = (this.selectionProviderName == null) ? Mode.CREATE : Mode.EDIT;
        if (this.selectionProviderName == null) {
            this.databaseSelectionProvider = new DatabaseSelectionProvider(this.table);
        }
        else {
            this.databaseSelectionProvider = (DatabaseSelectionProvider)DatabaseLogic.findSelectionProviderByName(this.table, this.selectionProviderName);
        }
        final DatabaseSelectionProviderForm databaseSelectionProviderForm = this.setupDbSelectionProviderForm(mode);
        this.dbSelectionProviderForm.readFromRequest(this.context.getRequest());
        if (!this.dbSelectionProviderForm.validate()) {
            return this.doEditSelectionProvider(databaseSelectionProviderForm);
        }
        this.dbSelectionProviderForm.writeToObject((Object)databaseSelectionProviderForm);
        if ((!StringUtils.isEmpty(databaseSelectionProviderForm.getSql()) && !StringUtils.isEmpty(databaseSelectionProviderForm.getHql())) || (StringUtils.isEmpty(databaseSelectionProviderForm.getSql()) && StringUtils.isEmpty(databaseSelectionProviderForm.getHql()))) {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("please.fill.exactly.one.of.the.fields.hql.sql", new Object[0]));
            return this.doEditSelectionProvider(databaseSelectionProviderForm);
        }
        final String[] refCols = StringUtils.split(databaseSelectionProviderForm.getColumns(), ",");
        final List<Column> columns = new ArrayList<Column>();
        for (final String c : refCols) {
            final Column col = DatabaseLogic.findColumnByName(this.table, c.trim());
            if (col == null) {
                SessionMessages.addErrorMessage(ElementsThreadLocals.getText("column._.not.found", new Object[] { c }));
                return this.doEditSelectionProvider(databaseSelectionProviderForm);
            }
            columns.add(col);
        }
        if (!StringUtils.equals(databaseSelectionProviderForm.getName(), this.selectionProviderName)) {
            TablesAction.logger.debug("Selection provider name changed or is new, checking for duplicates");
            if (DatabaseLogic.findSelectionProviderByName(this.table, databaseSelectionProviderForm.getName()) != null) {
                final String message = ElementsThreadLocals.getText("selection.provider._.already.exists", new Object[] { databaseSelectionProviderForm.getName() });
                SessionMessages.addErrorMessage(message);
                return this.doEditSelectionProvider(databaseSelectionProviderForm);
            }
            if (this.selectionProviderName == null) {
                TablesAction.logger.debug("Selection provider is new, adding");
                this.table.getSelectionProviders().add(this.databaseSelectionProvider);
            }
        }
        databaseSelectionProviderForm.copyTo(this.databaseSelectionProvider);
        this.databaseSelectionProvider.getReferences().clear();
        for (final Column col2 : columns) {
            final Reference ref = new Reference((HasReferences)this.databaseSelectionProvider);
            ref.setFromColumn(col2.getColumnName());
            this.databaseSelectionProvider.getReferences().add(ref);
        }
        try {
            this.saveModel();
        }
        catch (Exception e) {
            TablesAction.logger.error("Could not save model", (Throwable)e);
            SessionMessages.addErrorMessage(e.toString());
        }
        this.selectedTabId = "tab-fk-sp";
        return this.editTable();
    }
    
    protected void saveModel() throws IOException, JAXBException {
        this.persistence.initModel();
        this.persistence.saveXmlModel();
        SessionMessages.addInfoMessage(ElementsThreadLocals.getText("model.saved.successfully", new Object[0]));
    }
    
    protected boolean saveToColumnForm(final Form columnForm, final ColumnForm cf) {
        if (columnForm.validate()) {
            columnForm.writeToObject((Object)cf);
            if (!StringUtils.isEmpty(cf.getDateFormat())) {
                try {
                    new SimpleDateFormat(cf.getDateFormat());
                }
                catch (Exception e) {
                    final String message = ElementsThreadLocals.getText("invalid.date.format.string", new Object[0]);
                    columnForm.findFieldByPropertyName("dateFormat").getErrors().add(message);
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    protected org.remata.portofolio.actions.admin.tables.forms.TableForm setupTableForm(final Mode mode) {
        this.table = this.findTable();
        this.tableForm = new FormBuilder((Class)org.remata.portofolio.actions.admin.tables.forms.TableForm.class).configFields(new String[] { "entityName", "javaClass", "shortName", "hqlQuery" }).configMode(mode).build();
        final org.remata.portofolio.actions.admin.tables.forms.TableForm tf = new org.remata.portofolio.actions.admin.tables.forms.TableForm(this.table);
        this.tableForm.readFromObject((Object)tf);
        return tf;
    }
    
    protected void setupColumnsForm(final Mode mode) {
        final Type[] types = this.persistence.getConnectionProvider(this.table.getDatabaseName()).getTypes();
        this.decoratedColumns = new ArrayList<ColumnForm>(this.table.getColumns().size());
        final TableAccessor tableAccessor = new TableAccessor(this.table);
        for (final Column column : this.table.getColumns()) {
            PropertyAccessor columnAccessor;
            try {
                columnAccessor = tableAccessor.getProperty(column.getActualPropertyName());
            }
            catch (NoSuchFieldException e) {
                throw new Error(e);
            }
            final ColumnForm cf = this.decorateColumn(column, columnAccessor, types);
            if (cf != null) {
                this.decoratedColumns.add(cf);
            }
            else {
                SessionMessages.addWarningMessage(ElementsThreadLocals.getText("skipped.column._.with.unknown.type._", new Object[] { column.getColumnName(), column.getColumnType(), column.getJdbcType() }));
            }
        }
        final DefaultSelectionProvider typesSP = new DefaultSelectionProvider("columnType", 3);
        for (final ColumnForm columnForm : this.decoratedColumns) {
            this.configureTypesSelectionProvider(typesSP, columnForm);
        }
        (this.columnsTableForm = new TableFormBuilder((Class)ColumnForm.class).configFields(new String[] { "columnName", "propertyName", "javaType", "type", "shortLength", "scale", "reallyNullable" }).configSelectionProvider((SelectionProvider)typesSP, new String[] { "columnName", "type", "javaType" }).configNRows(this.decoratedColumns.size()).configMode(mode).build()).setSelectable(false);
        this.columnsTableForm.setCondensed(true);
        for (int i = 0; i < this.decoratedColumns.size(); ++i) {
            final TableForm.Row row = this.columnsTableForm.getRows()[i];
            final Column column2 = this.decoratedColumns.get(i);
            final Field columnNameField = row.findFieldByPropertyName("columnName");
            columnNameField.setHref(this.context.getRequest().getContextPath() + this.getActionPath() + "/" + column2.getColumnName());
        }
        this.columnsTableForm.readFromObject((Object)this.decoratedColumns);
    }
    
    protected ColumnForm setupColumnForm() {
        this.table = this.findTable();
        this.column = this.findColumn();
        final TableAccessor tableAccessor = new TableAccessor(this.table);
        PropertyAccessor columnAccessor;
        try {
            columnAccessor = tableAccessor.getProperty(this.column.getActualPropertyName());
        }
        catch (NoSuchFieldException e) {
            throw new Error(e);
        }
        final Type[] types = this.persistence.getConnectionProvider(this.table.getDatabaseName()).getTypes();
        final ColumnForm cf = this.decorateColumn(this.column, columnAccessor, types);
        final DefaultSelectionProvider typesSP = new DefaultSelectionProvider("columnType", 3);
        this.configureTypesSelectionProvider(typesSP, cf);
        final DefaultSelectionProvider stringFormatSP = new DefaultSelectionProvider("stringFormat");
        stringFormatSP.appendRow((Object)Email.class.getName(), "Email", true);
        stringFormatSP.appendRow((Object)Password.class.getName(), "Password", true);
        stringFormatSP.appendRow((Object)CAP.class.getName(), "CAP", true);
        stringFormatSP.appendRow((Object)PartitaIva.class.getName(), "Partita IVA", true);
        stringFormatSP.appendRow((Object)CodiceFiscale.class.getName(), "Codice Fiscale", true);
        stringFormatSP.appendRow((Object)Phone.class.getName(), "Phone", true);
        final DefaultSelectionProvider typeOfContentSP = new DefaultSelectionProvider("typeOfContent");
        typeOfContentSP.appendRow((Object)Multiline.class.getName(), "Multiline", true);
        typeOfContentSP.appendRow((Object)RichText.class.getName(), "RichText", true);
        this.columnForm = new FormBuilder((Class)ColumnForm.class).configFieldSetNames(new String[] { "Properties", "Annotations" }).configFields(new String[][] { { "columnName", "propertyName", "javaType", "type", "length", "scale", "reallyNullable", "reallyAutoincrement", "inPk" }, this.getApplicableAnnotations(this.column.getActualJavaType()) }).configSelectionProvider((SelectionProvider)typesSP, new String[] { "columnName", "type", "javaType" }).configSelectionProvider((SelectionProvider)stringFormatSP, new String[] { "stringFormat" }).configSelectionProvider((SelectionProvider)typeOfContentSP, new String[] { "typeOfContent" }).build();
        final SelectField typeOfContentField = (SelectField)this.columnForm.findFieldByPropertyName("typeOfContent");
        if (typeOfContentField != null) {
            typeOfContentField.setComboLabel("Plain");
        }
        this.columnForm.readFromObject((Object)cf);
        return cf;
    }
    
    protected void configureTypesSelectionProvider(final DefaultSelectionProvider typesSP, final ColumnForm columnForm) {
        final Type type = columnForm.getType();
        final Class[] javaTypes = this.getAvailableJavaTypes(type, columnForm.getLength());
        final Integer precision = columnForm.getLength();
        final Integer scale = columnForm.getScale();
        Class defaultJavaType = Type.getDefaultJavaType(columnForm.getJdbcType(), precision, scale);
        if (defaultJavaType == null) {
            defaultJavaType = Object.class;
        }
        typesSP.appendRow(new Object[] { columnForm.getColumnName(), type, null }, new String[] { columnForm.getColumnName(), type.getTypeName() + " (JDBC: " + type.getJdbcType() + ")", "Auto (" + defaultJavaType.getSimpleName() + ")" }, true);
        try {
            final Class existingType = Class.forName(columnForm.getJavaType());
            if (!ArrayUtils.contains((Object[])javaTypes, (Object)existingType)) {
                typesSP.appendRow(new Object[] { columnForm.getColumnName(), type, null }, new String[] { columnForm.getColumnName(), type.getTypeName() + " (JDBC: " + type.getJdbcType() + ")", existingType.getSimpleName() }, true);
            }
        }
        catch (Exception e) {
            TablesAction.logger.debug("Invalid Java type", (Throwable)e);
        }
        for (final Class c : javaTypes) {
            typesSP.appendRow(new Object[] { columnForm.getColumnName(), type, c.getName() }, new String[] { columnForm.getColumnName(), type.getTypeName() + " (JDBC: " + type.getJdbcType() + ")", c.getSimpleName() }, true);
        }
    }
    
    protected ColumnForm decorateColumn(final Column column, final PropertyAccessor columnAccessor, final Type[] types) {
        Type type = null;
        for (final Type candidate : types) {
            if (candidate.getJdbcType() == column.getJdbcType() && candidate.getTypeName().equalsIgnoreCase(column.getColumnType())) {
                type = candidate;
                break;
            }
        }
        if (type == null) {
            for (final Type candidate : types) {
                if (candidate.getJdbcType() == column.getJdbcType()) {
                    type = candidate;
                    break;
                }
            }
        }
        ColumnForm cf = null;
        if (type != null) {
            cf = new ColumnForm(column, columnAccessor, type);
        }
        return cf;
    }
    
    protected Class[] getAvailableJavaTypes(final Type type, final Integer length) {
        if (type.isNumeric()) {
            return new Class[] { Integer.class, Long.class, Byte.class, Short.class, Float.class, Double.class, BigInteger.class, BigDecimal.class, Boolean.class };
        }
        if (type.getDefaultJavaType() == String.class) {
            if (length != null && length < 256) {
                return new Class[] { String.class, Boolean.class };
            }
            return new Class[] { String.class };
        }
        else {
            if (type.getDefaultJavaType() == Timestamp.class) {
                return new Class[] { Timestamp.class, Date.class };
            }
            if (type.getDefaultJavaType() == Date.class) {
                return new Class[] { Date.class, Timestamp.class };
            }
            final Class defaultJavaType = type.getDefaultJavaType();
            if (defaultJavaType != null) {
                return new Class[] { defaultJavaType };
            }
            return new Class[] { Object.class };
        }
    }
    
    protected String[] getApplicableAnnotations(final Class type) {
        if (Number.class.isAssignableFrom(type)) {
            return new String[] { "fieldSize", "minValue", "maxValue", "decimalFormat" };
        }
        if (String.class.equals(type)) {
            return new String[] { "fieldSize", "typeOfContent", "stringFormat", "regexp", "highlightLinks", "fileBlob" };
        }
        if (java.util.Date.class.isAssignableFrom(type)) {
            return new String[] { "fieldSize", "dateFormat" };
        }
        return new String[0];
    }
    
    public Table findTable() {
        final Table table = DatabaseLogic.findTableByName(this.persistence.getModel(), this.databaseName, this.schemaName, this.tableName);
        if (table == null) {
            throw new ModelObjectNotFoundError(this.databaseName + "." + this.schemaName + "." + this.tableName);
        }
        return table;
    }
    
    public Column findColumn() {
        final Column column = DatabaseLogic.findColumnByName(this.table, this.columnName);
        if (column == null) {
            throw new ModelObjectNotFoundError(this.table.getQualifiedName() + "." + column);
        }
        return column;
    }
    
    @Button(list = "tables-list", key = "return.to.pages", order = 3.0)
    public Resolution returnToPages() {
        return (Resolution)new RedirectResolution("/");
    }
    
    public String getBaseActionPath() {
        return "/actions/admin/tables";
    }
    
    public String getActionPath() {
        String path = "/actions/admin/tables";
        if (this.tableName != null) {
            path = path + "/" + this.databaseName + "/" + this.schemaName + "/" + this.tableName;
            if (this.columnName != null) {
                path = path + "/" + this.columnName;
            }
        }
        return path;
    }
    
    public List<Table> getAllTables() {
        final List<Table> tables = (List<Table>)DatabaseLogic.getAllTables(this.persistence.getModel());
        Collections.sort(tables, new Comparator<Table>() {
            @Override
            public int compare(final Table o1, final Table o2) {
                final int dbComp = o1.getDatabaseName().compareToIgnoreCase(o2.getDatabaseName());
                if (dbComp != 0) {
                    return dbComp;
                }
                final int schemaComp = o1.getSchemaName().compareToIgnoreCase(o2.getSchemaName());
                if (schemaComp == 0) {
                    return o1.getTableName().compareToIgnoreCase(o2.getTableName());
                }
                return schemaComp;
            }
        });
        return tables;
    }
    
    public Model getModel() {
        return this.persistence.getModel();
    }
    
    public String getDatabaseName() {
        return this.databaseName;
    }
    
    public void setDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
    }
    
    public String getSchemaName() {
        return this.schemaName;
    }
    
    public void setSchemaName(final String schemaName) {
        this.schemaName = schemaName;
    }
    
    public String getTableName() {
        return this.tableName;
    }
    
    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }
    
    public String getColumnName() {
        return this.columnName;
    }
    
    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }
    
    public Table getTable() {
        return this.table;
    }
    
    public Column getColumn() {
        return this.column;
    }
    
    public Form getTableForm() {
        return this.tableForm;
    }
    
    public TableForm getColumnsTableForm() {
        return this.columnsTableForm;
    }
    
    public Form getColumnForm() {
        return this.columnForm;
    }
    
    @FieldSize(75)
    public String getShortName() {
        return this.shortName;
    }
    
    public void setShortName(final String shortName) {
        this.shortName = shortName;
    }
    
    public Field getShortNameField() {
        return this.shortNameField;
    }
    
    public String getSelectedTabId() {
        return this.selectedTabId;
    }
    
    public void setSelectedTabId(final String selectedTabId) {
        this.selectedTabId = selectedTabId;
    }
    
    public DatabaseSelectionProvider getDatabaseSelectionProvider() {
        return this.databaseSelectionProvider;
    }
    
    public Form getDbSelectionProviderForm() {
        return this.dbSelectionProviderForm;
    }
    
    public String getSelectionProviderName() {
        return this.selectionProviderName;
    }
    
    public void setSelectionProviderName(final String selectionProviderName) {
        this.selectionProviderName = selectionProviderName;
    }
    
    public Persistence getPersistence() {
        return this.persistence;
    }
    
    public List<String> getSortedColumnNames() {
        return this.sortedColumnNames;
    }
    
    public void setSortedColumnNames(final List<String> sortedColumnNames) {
        this.sortedColumnNames = sortedColumnNames;
    }
    
    public List<ColumnForm> getDecoratedColumns() {
        return this.decoratedColumns;
    }
    
    public Map<String, String> getFkOnePropertyNames() {
        return this.fkOnePropertyNames;
    }
    
    public Map<String, String> getFkManyPropertyNames() {
        return this.fkManyPropertyNames;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)TablesAction.class);
    }
}
