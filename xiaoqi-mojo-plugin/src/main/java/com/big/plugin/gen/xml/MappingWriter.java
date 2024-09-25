package com.big.plugin.gen.xml;


import com.big.orm.MyCustomMojo;
import com.big.plugin.utils.AutoGenUtil;
import com.big.plugin.utils.ReflectionUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.persistence.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.big.MojoConstants.GENERATED_MAPPERS_NAME_PREFIX;
import static com.big.MojoConstants.MAPPER_SUFFIX;


public class MappingWriter {
    private final Class modelClass;
    private final File mappingFile;
    private final List<Field> manyToManyFields;
    private final List<Class> fieldOrder;
    private final List<Field> fieldsToWrite;
    private Class superEntityClass = null;

    public MappingWriter(Class modelClass, File mappingFile) {
        this.modelClass = modelClass;
        this.mappingFile = mappingFile;
        this.manyToManyFields = new ArrayList<>();
        this.fieldOrder = Arrays.asList(Id.class, Column.class, Convert.class,
                OneToOne.class, ManyToOne.class, OneToMany.class, ManyToMany.class);
        this.fieldsToWrite = AutoGenUtil.getFieldsNeedToWrite(this.modelClass);
        if (this.modelClass.getSuperclass().getDeclaredAnnotation(Entity.class) != null) {
            this.superEntityClass = this.modelClass.getSuperclass();
        }
    }

    public void write() throws IOException, JDOMException {
        if (!mappingFile.exists()) {
            AutoGenUtil.copyFile(MyCustomMojo.class.getClassLoader().getResourceAsStream("MappingTemplate.xml"), mappingFile);
        }
        InputStream inputStream = new FileInputStream(mappingFile);
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(new NoOpEntityResolver());
            Document doc = builder.build(inputStream);
            Element rootEle = doc.getRootElement();
            rootEle.setAttribute("namespace",
                    GENERATED_MAPPERS_NAME_PREFIX + modelClass.getSimpleName() + MAPPER_SUFFIX);
            this.createResultMapEle(rootEle);
            this.createSelectEle(rootEle);
            this.createSelectEleForManyToMany(rootEle);
            XMLOutputter out = new XMLOutputter();
            out.setFormat(Format.getPrettyFormat());
            out.output(doc, new FileOutputStream(mappingFile.getAbsoluteFile()));

            if (superEntityClass != null) {
                this.modifySuperClassMapping();
            }
        } finally {
            inputStream.close();
        }
    }

    //mybatis通过discriminator实现继承映射
    private void modifySuperClassMapping() throws IOException, JDOMException {
        FilenameFilter pathFilter = (dir, name) -> {
            if (ArrayUtils.contains(new String[]{"target", "logs", ".idea", ".mvn", ".git"}, name)) return false;
            return true;
        };

        File rootPath = new File(System.getProperty("user.dir"));
        File superClassMapping = this.searchFile(rootPath, pathFilter,
                "Gen" + superEntityClass.getSimpleName() + "Mapping.xml");
        if (superClassMapping != null) {
            InputStream inputStream = new FileInputStream(superClassMapping);
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(new NoOpEntityResolver());
            Document doc = builder.build(inputStream);
            Element rootEle = doc.getRootElement();
            Element resultMapEle = rootEle.getChildren().stream()
                    .filter(child -> child.getName().equals("resultMap")
                            && child.getAttributeValue("id").equals(superEntityClass.getSimpleName() + "ResultMap"))
                    .findFirst().get();
            Element discriminatorEle;
            Annotation inheritance = superEntityClass.getDeclaredAnnotation(Inheritance.class);
            if (resultMapEle.getChild("discriminator") != null) {
                discriminatorEle = resultMapEle.getChild("discriminator");
            } else {
                discriminatorEle = new Element("discriminator");
                discriminatorEle.setAttribute("javaType", "java.lang.String");
                if (inheritance != null && ((Inheritance) inheritance).strategy() == InheritanceType.TABLE_PER_CLASS) {
                    discriminatorEle.setAttribute("column", "tableName");
                } else {
                    discriminatorEle.setAttribute("column", "dtype");
                }
                resultMapEle.addContent(discriminatorEle);
            }

            Element caseEle = new Element("case");
            if (inheritance != null && ((Inheritance) inheritance).strategy() == InheritanceType.TABLE_PER_CLASS) {
                caseEle.setAttribute("value", AutoGenUtil.getTableAnnotation(this.modelClass).name());
            } else {
                caseEle.setAttribute("value", this.modelClass.getSimpleName());
            }
            caseEle.setAttribute("resultMap", new StringBuilder(GENERATED_MAPPERS_NAME_PREFIX)
                    .append(modelClass.getSimpleName())
                    .append(MAPPER_SUFFIX)
                    .append(".")
                    .append(this.modelClass.getSimpleName())
                    .append("ResultMap").toString());
            discriminatorEle.addContent(caseEle);

            XMLOutputter out = new XMLOutputter();
            out.setFormat(Format.getPrettyFormat());
            out.output(doc, new FileOutputStream(superClassMapping.getAbsoluteFile()));
        }
    }

    private File searchFile(File path, FilenameFilter pathFilter, String fileName) {
        File targetFile = null;
        if (path.isDirectory()) {
            for (File file : path.listFiles(pathFilter)) {
                targetFile = this.searchFile(file, pathFilter, fileName);
                if (targetFile != null) return targetFile;
            }
        } else if (path.isFile()) {
            if (path.getName().equals(fileName)) {
                targetFile = path;
            }
        }
        return targetFile;
    }

    private void createSelectEleForManyToMany(Element rootEle) {
        for (Field field : this.manyToManyFields) {
            JoinTable joinTableAnn = field.getDeclaredAnnotation(JoinTable.class);
            Element element = new Element("select");
            Class targetClass = ReflectionUtil.getClassFromParameterizedType(field);
            String parameterizedType = targetClass.getSimpleName();
            StringBuilder idAttr = new StringBuilder("find")
                    .append(StringUtils.capitalizeFirstLetter(field.getName()));
            element.setAttribute("id", idAttr.toString());
            element.setAttribute("parameterType", "Long");
            String targetResultMap = new StringBuilder(GENERATED_MAPPERS_NAME_PREFIX)
                    .append(parameterizedType).append(MAPPER_SUFFIX).append(".")
                    .append(parameterizedType).append("ResultMap")
                    .toString();
            element.setAttribute("resultMap", targetResultMap);
            String alias = parameterizedType.substring(0, 1).toLowerCase();
            String joinTableName = AutoGenUtil.getJoinTableName(field);
            Table tableAnn = AutoGenUtil.getTableAnnotation(targetClass);
            StringBuilder sqlStrBuilder = new StringBuilder("select ")
                    .append(alias)
                    .append(".* from ")
                    .append(joinTableName)
                    .append(" as rel join ")
                    .append(tableAnn.name())
                    .append(" as ")
                    .append(alias)
                    .append(" on rel.")
                    .append(joinTableAnn == null ? "source_pk" : "target_pk")
                    .append(" = ")
                    .append(alias)
                    .append(".pk")
                    .append(" where rel.")
                    .append(joinTableAnn == null ? "target_pk" : "source_pk")
                    .append(" = #{pk}");
            if (targetClass.getDeclaredAnnotation(Table.class) == null) {
                sqlStrBuilder.append(" AND dtype = '").append(targetClass.getSimpleName()).append("'");
            }
            element.addContent(sqlStrBuilder.toString());
            rootEle.addContent(element);
        }
    }

    private void createResultMapEle(Element rootEle) {
        Optional<Element> optional = rootEle.getChildren("resultMap").stream()
                .filter(element -> (this.modelClass.getSimpleName() + "ResultMap").equals(element.getAttributeValue("id")))
                .findFirst();
        Element resultMapEle;
        if (optional.isPresent()) {
            resultMapEle = optional.get();
            rootEle.removeContent(resultMapEle);
        }
        resultMapEle = this.newResultMapEle();
        rootEle.addContent(resultMapEle);
        this.createResultMap(resultMapEle);
    }

    private void createResultMap(Element resultMapEle) {
        this.fieldsToWrite.stream()
                .sorted((o1, o2) -> {
                    int index1 = -1;
                    int index2 = -1;
                    for (Annotation annotation : o1.getAnnotations()) {
                        if (this.fieldOrder.indexOf(annotation.annotationType()) > -1) {
                            index1 = this.fieldOrder.indexOf(annotation.annotationType());
                        }
                    }
                    for (Annotation annotation : o2.getAnnotations()) {
                        if (this.fieldOrder.indexOf(annotation.annotationType()) > -1) {
                            index2 = this.fieldOrder.indexOf(annotation.annotationType());
                        }
                    }
                    return index1 - index2;
                })
                .forEach(field -> {
                    Element resultEle = null;
                    if (field.getDeclaredAnnotation(Id.class) != null) {
                        resultEle = new Element("result");
                        resultEle.setAttribute("property", field.getName());
                        resultEle.setAttribute("column", field.getName());
                    }
                    if (field.getDeclaredAnnotation(Column.class) != null) {
                        Column columnAnn = field.getDeclaredAnnotation(Column.class);
                        if (org.apache.commons.lang3.StringUtils.isNotBlank(columnAnn.name())) {
                            resultEle = new Element("result");
                            resultEle.setAttribute("property", field.getName());
                            resultEle.setAttribute("column", columnAnn.name());
                        } else if (field.getName().contains("_")) {
                            resultEle = new Element("result");
                            resultEle.setAttribute("property", field.getName());
                            resultEle.setAttribute("column", field.getName());
                        }
                    }
                    if (field.getDeclaredAnnotation(Convert.class) != null) {
                        Convert convertAnn = field.getDeclaredAnnotation(Convert.class);
                        resultEle = new Element("result");
                        resultEle.setAttribute("property", field.getName());
                        resultEle.setAttribute("column", field.getName());
                        resultEle.setAttribute("typeHandler", convertAnn.converter().getName());
                    }
                    if (field.getDeclaredAnnotation(OneToOne.class) != null
                            || field.getDeclaredAnnotation(ManyToOne.class) != null) {
                        resultEle = new Element("association");
                        resultEle.setAttribute("fetchType", "lazy");
                        resultEle.setAttribute("property", field.getName());
                        Inheritance inheritance = field.getType().getAnnotation(Inheritance.class);
                        if (inheritance != null && inheritance.strategy() == InheritanceType.TABLE_PER_CLASS) {
                            resultEle.setAttribute("column", "pk=" + field.getName() + "_pk" + ", tableName=" + field.getName() + "Table");
                        } else {
                            resultEle.setAttribute("column", "pk=" + field.getName() + "_pk");
                        }
                        resultEle.setAttribute("select", GENERATED_MAPPERS_NAME_PREFIX + field.getType().getSimpleName() + "Mapper.find");
                    }
                    if (field.getDeclaredAnnotation(OneToMany.class) != null) {
                        OneToMany oneToManyAnn = field.getDeclaredAnnotation(OneToMany.class);
                        resultEle = new Element("collection");
                        resultEle.setAttribute("property", field.getName());
                        resultEle.setAttribute("ofType", ReflectionUtil.getClassFromParameterizedType(field).getName());
                        resultEle.setAttribute("column", oneToManyAnn.mappedBy() + "=pk");
                        resultEle.setAttribute("select", GENERATED_MAPPERS_NAME_PREFIX
                                + ReflectionUtil.getClassFromParameterizedType(field).getSimpleName() + "Mapper.find");
                    }
                    if (field.getDeclaredAnnotation(ManyToMany.class) != null) {
                        resultEle = new Element("collection");
                        resultEle.setAttribute("property", field.getName());
                        resultEle.setAttribute("ofType", ReflectionUtil.getClassFromParameterizedType(field).getName());
                        resultEle.setAttribute("column", "pk");
                        StringBuilder selectAttr = new StringBuilder("find")
                                .append(StringUtils.capitalizeFirstLetter(field.getName()));
                        resultEle.setAttribute("select", selectAttr.toString());
                        manyToManyFields.add(field);
                    }
                    if (resultEle != null) {
                        resultMapEle.addContent(resultEle);
                    }
                });

    }

    private Element newResultMapEle() {
        Element element = new Element("resultMap");
        element.setAttribute("id", this.modelClass.getSimpleName() + "ResultMap");
        element.setAttribute("type", this.modelClass.getName());
        if (superEntityClass != null) {
            element.setAttribute("extends", new StringBuilder(GENERATED_MAPPERS_NAME_PREFIX)
                    .append(superEntityClass.getSimpleName())
                    .append(MAPPER_SUFFIX)
                    .append(".")
                    .append(superEntityClass.getSimpleName())
                    .append("ResultMap").toString());
        }
        return element;
    }

    private void createSelectEle(Element rootEle) {
        Optional<Element> optional = rootEle.getChildren("select").stream()
                .filter(element -> "find".equals(element.getAttributeValue("id"))).findFirst();
        Element selectEle;
        if (optional.isPresent()) {
            selectEle = optional.get();
            rootEle.removeContent(selectEle);
        }
        this.newSelectElement(rootEle);
    }

    private void createSelectSQL(Element selectEle, Element selectTotalEle) {
        Table tableAnn = AutoGenUtil.getTableAnnotation(this.modelClass);
        Annotation inheritance = this.modelClass.getDeclaredAnnotation(Inheritance.class);
        if (inheritance != null
                && ((Inheritance) inheritance).strategy() == InheritanceType.TABLE_PER_CLASS) {
            //处理继承关系中的分表情况
            selectEle.addContent("select * ");
            Element ifEle = new Element("if");
            ifEle.setAttribute("test", "tableName != null");
            ifEle.setText(", '${tableName}' as tableName");
            selectEle.addContent(ifEle);
            selectEle.addContent("from");
            Element chooseEle = new Element("choose");
            Element whenEle = new Element("when");
            whenEle.setAttribute("test", "tableName != null");
            whenEle.setText("${tableName}");
            chooseEle.addContent(whenEle);
            Element otherwiseEle = new Element("otherwise");
            otherwiseEle.setText(tableAnn.name());
            chooseEle.addContent(otherwiseEle);
            selectEle.addContent(chooseEle);
        } else {
            selectEle.addContent("select * from " + tableAnn.name());
        }
        selectTotalEle.addContent("select count(*) from " + tableAnn.name());
        this.createWhereEle(selectEle);
        this.createWhereEle(selectTotalEle);
        this.addOrderByEle(selectEle);
        this.addPageableEle(selectEle);
    }

    private void createWhereEle(Element parentEle) {
        Element whereEle = new Element("where");
        parentEle.addContent(whereEle);
        List<Field> columns = ReflectionUtil.getDeclaredFields(this.modelClass).stream()
                .filter(filed -> filed.getDeclaredAnnotation(Column.class) != null
                        || filed.getDeclaredAnnotation(Id.class) != null
                        || filed.getDeclaredAnnotation(OneToOne.class) != null
                        || filed.getDeclaredAnnotation(ManyToOne.class) != null)
                .collect(Collectors.toList());
        columns.stream().forEach(column -> {
            Element ifEle = new Element("if");
            ifEle.setAttribute("test", column.getName() + " != null");
            StringBuilder textBuilder = new StringBuilder("AND ");
            Column columnAnn = column.getDeclaredAnnotation(Column.class);
            OneToOne oneToOneAnn = column.getDeclaredAnnotation(OneToOne.class);
            ManyToOne manyToOneAnn = column.getDeclaredAnnotation(ManyToOne.class);
            if (columnAnn != null && org.apache.commons.lang3.StringUtils.isNotBlank(columnAnn.name())) {
                textBuilder.append(columnAnn.name());
            } else if (oneToOneAnn != null || manyToOneAnn != null) {
                textBuilder.append(column.getName()).append("_pk");
            } else {
                textBuilder.append(column.getName());
            }
            textBuilder.append(" = #{").append(column.getName()).append("}");
            ifEle.setText(textBuilder.toString());
            whereEle.addContent(ifEle);
        });
        if (this.modelClass.getDeclaredAnnotation(Table.class) == null) {
            Element ifEle = new Element("if");
            ifEle.setAttribute("test", "true");
            ifEle.setText("AND dtype = '" + this.modelClass.getSimpleName() + "'");
            whereEle.addContent(ifEle);
        }
    }

    private void addPageableEle(Element selectEle) {
        Element pageableEle = new Element("if");
        selectEle.addContent(pageableEle);
        pageableEle.setAttribute("test", "pageable != null and pageable.start != null and pageable.end != null");
        pageableEle.addContent("limit #{pageable.start}, #{pageable.end}");
    }

    private void addOrderByEle(Element selectEle) {
        Element orderByEle = new Element("if");
        selectEle.addContent(orderByEle);
        orderByEle.setAttribute("test",
                "orderBy != null and orderBy.fields != null and orderBy.sort != null");
        orderByEle.addContent("ORDER BY ");
        Element forEachEle = new Element("foreach");
        orderByEle.addContent(forEachEle);
        forEachEle.setAttribute("collection", "orderBy.fields");
        forEachEle.setAttribute("item", "field");
        forEachEle.setAttribute("separator", ",");
        forEachEle.addContent("${field}");
        orderByEle.addContent(" ${orderBy.sort}");
    }

    private void newSelectElement(Element rootEle) {
        Element selectEle = new Element("select");
        selectEle.setAttribute("id", "find");
        selectEle.setAttribute("parameterType", "Map");
        selectEle.setAttribute("resultMap", this.modelClass.getSimpleName() + "ResultMap");

        Element selectTotalEle = new Element("select");
        selectTotalEle.setAttribute("id", "getTotal");
        selectTotalEle.setAttribute("parameterType", "Map");
        selectTotalEle.setAttribute("resultType", "Long");

        rootEle.addContent(selectEle);
        rootEle.addContent(selectTotalEle);

        this.createSelectSQL(selectEle, selectTotalEle);
    }

    public class NoOpEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        }
    }
}
