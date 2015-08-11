package org.remata.portofolio.actions.admin.database;

import org.remata.portofolio.model.database.*;
import java.util.*;
import org.remata.portofolio.model.*;

public class DDLJsonUtils
{
    public static String getColumns(final Table table) {
        final StringBuffer buf = new StringBuffer();
        buf.append("{\n");
        buf.append("\"columns\": [\n");
        int i = 1;
        final int size = table.getColumns().size();
        for (final Column col : table.getColumns()) {
            buf.append("{\n");
            printJsonValue(buf, "name", col.getColumnName(), false);
            printJsonValue(buf, "colType", col.getColumnType(), false);
            printJsonValue(buf, "javaType", col.getJavaType(), true);
            buf.append("}\n");
            if (i != size) {
                buf.append(",");
            }
            ++i;
        }
        buf.append("]\n");
        buf.append("}\n");
        return buf.toString();
    }
    
    public static String getPk(final Table table) {
        final PrimaryKey pk = table.getPrimaryKey();
        final StringBuffer buf = new StringBuffer();
        buf.append("{\n");
        printJsonValue(buf, "pkName", pk.getPrimaryKeyName(), false);
        buf.append("\"columns\": [\n");
        int i = 1;
        final int size = pk.getPrimaryKeyColumns().size();
        for (final PrimaryKeyColumn col : pk.getPrimaryKeyColumns()) {
            buf.append("{\n");
            printJsonValue(buf, "name", col.getColumnName(), true);
            buf.append("}\n");
            if (i != size) {
                buf.append(",");
            }
            ++i;
        }
        buf.append("]\n");
        buf.append("}\n");
        return buf.toString();
    }
    
    public static String getForeignKey(final Table table) {
        final List<ForeignKey> fks = (List<ForeignKey>)table.getForeignKeys();
        final StringBuffer buf = new StringBuffer();
        buf.append("{\n");
        buf.append("\"fks\": [\n");
        int i = 1;
        final int sizeFks = fks.size();
        for (final ForeignKey fk : fks) {
            buf.append("{\n");
            printJsonValue(buf, "fkName", fk.getName(), false);
            buf.append("\"refs\": [\n");
            int j = 1;
            final int sizeRef = fk.getReferences().size();
            for (final Reference ref : fk.getReferences()) {
                buf.append("{\n");
                printJsonValue(buf, "toTable", fk.getToTable().getQualifiedName(), false);
                printJsonValue(buf, "from", ref.getFromColumn(), false);
                printJsonValue(buf, "to", ref.getToColumn(), true);
                buf.append("}\n");
                if (j != sizeRef) {
                    buf.append(",");
                }
                ++j;
            }
            buf.append("]\n");
            buf.append("}\n");
            if (i != sizeFks) {
                buf.append(",");
            }
            ++i;
        }
        buf.append("]\n");
        buf.append("}\n");
        return buf.toString();
    }
    
    public static String getAnnotations(final Table table) {
        final StringBuffer buf = new StringBuffer();
        buf.append("{\n");
        buf.append("\"annotations\": [\n");
        int i = 1;
        final int size = table.getAnnotations().size();
        for (final Annotation ann : table.getAnnotations()) {
            buf.append("{\n");
            printJsonValue(buf, "name", ann.getType(), false);
            String values = "";
            boolean first = true;
            for (final String value : ann.getValues()) {
                if (!first) {
                    values += ", ";
                    first = false;
                }
                values += value;
            }
            printJsonValue(buf, "values", values, true);
            buf.append("}\n");
            if (i != size) {
                buf.append(",");
            }
            ++i;
        }
        buf.append("]\n");
        buf.append("}\n");
        return buf.toString();
    }
    
    private static void printJsonValue(final StringBuffer buf, final String prop, final String value, final boolean last) {
        if (last) {
            buf.append("\"" + prop + "\": \"" + value + "\"\n");
        }
        else {
            buf.append("\"" + prop + "\": \"" + value + "\", \n");
        }
    }
}
