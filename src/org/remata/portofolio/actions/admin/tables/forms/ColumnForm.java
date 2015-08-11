package org.remata.portofolio.actions.admin.tables.forms;

import org.remata.portofolio.database.*;
import java.math.*;
import org.remata.elements.reflection.*;
import org.apache.commons.beanutils.*;
import org.remata.portofolio.model.database.*;
import org.apache.commons.lang.*;
import org.remata.portofolio.model.*;
import java.util.*;
import org.remata.elements.annotations.*;

public class ColumnForm extends Column
{
    public static final String copyright = "Copyright (c) 2005-2015, Remata Web Portofolio";
    protected final Type type;
    protected final boolean inPk;
    protected Integer fieldSize;
    protected Integer maxLength;
    protected String typeOfContent;
    protected String stringFormat;
    protected boolean highlightLinks;
    protected String regexp;
    protected boolean fileBlob;
    protected BigDecimal minValue;
    protected BigDecimal maxValue;
    protected String decimalFormat;
    protected String dateFormat;
    public static final String[] KNOWN_ANNOTATIONS;
    
    public ColumnForm(final Column copyFrom, final PropertyAccessor columnAccessor, final Type type) {
        try {
            BeanUtils.copyProperties((Object)this, (Object)copyFrom);
        }
        catch (Exception e) {
            throw new Error(e);
        }
        this.type = type;
        this.inPk = DatabaseLogic.isInPk(copyFrom);
        final FieldSize fieldSizeAnn = (FieldSize)columnAccessor.getAnnotation((Class)FieldSize.class);
        if (fieldSizeAnn != null) {
            this.fieldSize = fieldSizeAnn.value();
        }
        final MaxLength maxLengthAnn = (MaxLength)columnAccessor.getAnnotation((Class)MaxLength.class);
        if (maxLengthAnn != null) {
            this.maxLength = maxLengthAnn.value();
        }
        final Multiline multilineAnn = (Multiline)columnAccessor.getAnnotation((Class)Multiline.class);
        if (multilineAnn != null && multilineAnn.value()) {
            this.typeOfContent = Multiline.class.getName();
        }
        final RichText richTextAnn = (RichText)columnAccessor.getAnnotation((Class)RichText.class);
        if (richTextAnn != null && richTextAnn.value()) {
            this.typeOfContent = RichText.class.getName();
        }
        if (columnAccessor.isAnnotationPresent((Class)Email.class)) {
            this.stringFormat = Email.class.getName();
        }
        if (columnAccessor.isAnnotationPresent((Class)CAP.class)) {
            this.stringFormat = CAP.class.getName();
        }
        if (columnAccessor.isAnnotationPresent((Class)CodiceFiscale.class)) {
            this.stringFormat = CodiceFiscale.class.getName();
        }
        if (columnAccessor.isAnnotationPresent((Class)PartitaIva.class)) {
            this.stringFormat = PartitaIva.class.getName();
        }
        if (columnAccessor.isAnnotationPresent((Class)Password.class)) {
            this.stringFormat = Password.class.getName();
        }
        if (columnAccessor.isAnnotationPresent((Class)Phone.class)) {
            this.stringFormat = Phone.class.getName();
        }
        final HighlightLinks hlAnn = (HighlightLinks)columnAccessor.getAnnotation((Class)HighlightLinks.class);
        if (hlAnn != null) {
            this.highlightLinks = hlAnn.value();
        }
        final RegExp regexpAnn = (RegExp)columnAccessor.getAnnotation((Class)RegExp.class);
        if (regexpAnn != null) {
            this.regexp = regexpAnn.value();
        }
        final FileBlob fileBlobAnn = (FileBlob)columnAccessor.getAnnotation((Class)FileBlob.class);
        if (fileBlobAnn != null) {
            this.fileBlob = true;
        }
        final MinDecimalValue minDecimalValueAnn = (MinDecimalValue)columnAccessor.getAnnotation((Class)MinDecimalValue.class);
        if (minDecimalValueAnn != null) {
            this.minValue = new BigDecimal(minDecimalValueAnn.value());
        }
        else {
            final MinIntValue minIntValueAnn = (MinIntValue)columnAccessor.getAnnotation((Class)MinIntValue.class);
            if (minIntValueAnn != null) {
                this.minValue = new BigDecimal(minIntValueAnn.value());
            }
        }
        final MaxDecimalValue maxDecimalValueAnn = (MaxDecimalValue)columnAccessor.getAnnotation((Class)MaxDecimalValue.class);
        if (maxDecimalValueAnn != null) {
            this.maxValue = new BigDecimal(maxDecimalValueAnn.value());
        }
        else {
            final MaxIntValue maxIntValueAnn = (MaxIntValue)columnAccessor.getAnnotation((Class)MaxIntValue.class);
            if (maxIntValueAnn != null) {
                this.maxValue = new BigDecimal(maxIntValueAnn.value());
            }
        }
        final DecimalFormat decimalFormatAnn = (DecimalFormat)columnAccessor.getAnnotation((Class)DecimalFormat.class);
        if (decimalFormatAnn != null) {
            this.decimalFormat = decimalFormatAnn.value();
        }
        final DateFormat dateFormatAnn = (DateFormat)columnAccessor.getAnnotation((Class)DateFormat.class);
        if (dateFormatAnn != null) {
            this.dateFormat = dateFormatAnn.value();
        }
    }
    
    public void copyTo(final Column column) {
        column.setJavaType(this.getJavaType());
        column.setPropertyName(StringUtils.defaultIfEmpty(this.getPropertyName(), (String)null));
        for (final String annotationClass : ColumnForm.KNOWN_ANNOTATIONS) {
            this.removeAnnotation(annotationClass, column.getAnnotations());
        }
        if (this.fieldSize != null) {
            final Annotation ann = new Annotation((Object)column, FieldSize.class.getName());
            ann.getValues().add(this.fieldSize.toString());
            column.getAnnotations().add(ann);
        }
        if (this.maxLength != null) {
            final Annotation ann = new Annotation((Object)column, MaxLength.class.getName());
            ann.getValues().add(this.maxLength.toString());
            column.getAnnotations().add(ann);
        }
        if (this.typeOfContent != null) {
            final Annotation ann = new Annotation((Object)column, this.typeOfContent);
            ann.getValues().add("true");
            column.getAnnotations().add(ann);
        }
        if (this.stringFormat != null) {
            final Annotation ann = new Annotation((Object)column, this.stringFormat);
            column.getAnnotations().add(ann);
        }
        if (this.highlightLinks) {
            final Annotation ann = new Annotation((Object)column, HighlightLinks.class.getName());
            ann.getValues().add("true");
            column.getAnnotations().add(ann);
        }
        if (!StringUtils.isEmpty(this.regexp)) {
            final Annotation ann = new Annotation((Object)column, RegExp.class.getName());
            ann.getValues().add(this.regexp);
            ann.getValues().add("elements.error.field.regexp.format");
            column.getAnnotations().add(ann);
        }
        if (this.fileBlob) {
            final Annotation ann = new Annotation((Object)column, FileBlob.class.getName());
            column.getAnnotations().add(ann);
        }
        if (this.minValue != null) {
            final Annotation ann = new Annotation((Object)column, MinDecimalValue.class.getName());
            ann.getValues().add(this.minValue.toString());
            column.getAnnotations().add(ann);
        }
        if (this.maxValue != null) {
            final Annotation ann = new Annotation((Object)column, MaxDecimalValue.class.getName());
            ann.getValues().add(this.maxValue.toString());
            column.getAnnotations().add(ann);
        }
        if (!StringUtils.isEmpty(this.decimalFormat)) {
            final Annotation ann = new Annotation((Object)column, DecimalFormat.class.getName());
            ann.getValues().add(this.decimalFormat);
            column.getAnnotations().add(ann);
        }
        if (!StringUtils.isEmpty(this.dateFormat)) {
            final Annotation ann = new Annotation((Object)column, DateFormat.class.getName());
            ann.getValues().add(this.dateFormat);
            column.getAnnotations().add(ann);
        }
    }
    
    protected void removeAnnotation(final String annotationClass, final List<Annotation> annotations) {
        final Iterator<Annotation> it = annotations.iterator();
        while (it.hasNext()) {
            final Annotation ann = it.next();
            if (ann.getType().equals(annotationClass)) {
                it.remove();
            }
        }
    }
    
    @Updatable(false)
    @Insertable(false)
    @Label("Name")
    public String getColumnName() {
        return super.getColumnName();
    }
    
    @FieldSize(4)
    @Updatable(false)
    @Insertable(false)
    public Integer getLength() {
        return super.getLength();
    }
    
    @Updatable(false)
    @Insertable(false)
    @Label("Length")
    public String getShortLength() {
        if (this.getLength() == null) {
            return null;
        }
        final String[] suffix = { "", "K", "M", "G", "T" };
        final java.text.DecimalFormat decimalFormat = new java.text.DecimalFormat("##0E0");
        final String result = decimalFormat.format(this.getLength());
        final int suffixIndex = Character.getNumericValue(result.charAt(result.length() - 1)) / 3;
        return result.replaceAll("E[0-9]", suffix[suffixIndex]);
    }
    
    @FieldSize(4)
    @Updatable(false)
    @Insertable(false)
    public Integer getScale() {
        return super.getScale();
    }
    
    @RegExp(value = "(_|$|[a-z]|[A-Z]|[\u0080-\ufffe])(_|$|[a-z]|[A-Z]|[\u0080-\ufffe]|[0-9])*", errorMessage = "invalid.property.name")
    public String getPropertyName() {
        return super.getPropertyName();
    }
    
    @Label("Type")
    @Updatable(false)
    @Insertable(false)
    public Type getType() {
        return this.type;
    }
    
    @Label("Class")
    @Select(nullOption = false)
    public String getJavaType() {
        return super.getJavaType();
    }
    
    @Label("Null")
    @Insertable(false)
    @Updatable(false)
    public boolean isReallyNullable() {
        return this.isNullable();
    }
    
    @Label("Autoincrement")
    @Insertable(false)
    @Updatable(false)
    public boolean isReallyAutoincrement() {
        return this.isAutoincrement();
    }
    
    @Label("In primary key")
    @Updatable(false)
    @Insertable(false)
    public boolean isInPk() {
        return this.inPk;
    }
    
    @MinIntValue(1)
    public Integer getFieldSize() {
        return this.fieldSize;
    }
    
    public void setFieldSize(final Integer fieldSize) {
        this.fieldSize = fieldSize;
    }
    
    public Integer getMaxLength() {
        return this.maxLength;
    }
    
    public void setMaxLength(final Integer maxLength) {
        this.maxLength = maxLength;
    }
    
    public String getTypeOfContent() {
        return this.typeOfContent;
    }
    
    public void setTypeOfContent(final String typeOfContent) {
        this.typeOfContent = typeOfContent;
    }
    
    public String getStringFormat() {
        return this.stringFormat;
    }
    
    public void setStringFormat(final String stringFormat) {
        this.stringFormat = stringFormat;
    }
    
    public boolean isHighlightLinks() {
        return this.highlightLinks;
    }
    
    public void setHighlightLinks(final boolean highlightLinks) {
        this.highlightLinks = highlightLinks;
    }
    
    @FieldSize(75)
    public String getRegexp() {
        return this.regexp;
    }
    
    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }
    
    public boolean isFileBlob() {
        return this.fileBlob;
    }
    
    public void setFileBlob(final boolean fileBlob) {
        this.fileBlob = fileBlob;
    }
    
    @PrecisionScale(scale = 10, precision = 100)
    @DecimalFormat("#.#####")
    public BigDecimal getMinValue() {
        return this.minValue;
    }
    
    public void setMinValue(final BigDecimal minValue) {
        this.minValue = minValue;
    }
    
    @PrecisionScale(scale = 10, precision = 100)
    @DecimalFormat("#.#####")
    public BigDecimal getMaxValue() {
        return this.maxValue;
    }
    
    public void setMaxValue(final BigDecimal maxValue) {
        this.maxValue = maxValue;
    }
    
    public String getDecimalFormat() {
        return this.decimalFormat;
    }
    
    public void setDecimalFormat(final String decimalFormat) {
        this.decimalFormat = decimalFormat;
    }
    
    public String getDateFormat() {
        return this.dateFormat;
    }
    
    public void setDateFormat(final String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String toString() {
        return "Not used.";
    }
    
    static {
        KNOWN_ANNOTATIONS = new String[] { FieldSize.class.getName(), MaxLength.class.getName(), Multiline.class.getName(), RichText.class.getName(), Email.class.getName(), CAP.class.getName(), CodiceFiscale.class.getName(), PartitaIva.class.getName(), Password.class.getName(), Phone.class.getName(), HighlightLinks.class.getName(), RegExp.class.getName(), FileBlob.class.getName(), MinDecimalValue.class.getName(), MinIntValue.class.getName(), MaxDecimalValue.class.getName(), MaxIntValue.class.getName(), DecimalFormat.class.getName(), DateFormat.class.getName() };
    }
}
