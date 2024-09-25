package com.big.plugin.utils;

import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoGenUtil {
    private static final Pattern humpPattern = Pattern.compile("[A-Z]");


    public static void copyFile(InputStream source, File dest) throws IOException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = source.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            source.close();
            output.close();
        }
    }

    public static File clearDir(File dir) {
        checkParentFile(dir);
        if (!dir.exists()) {
            dir.mkdir();
        } else {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        return dir;
    }

    private static void checkParentFile(File dir) {
        if (!dir.getParentFile().exists()) {
            checkParentFile(dir.getParentFile());
            dir.getParentFile().mkdir();
        }
    }

    public static String humpToLine(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static Table getTableAnnotation(Class clazz) {
        Annotation annotation = clazz.getDeclaredAnnotation(Table.class);
        if (annotation == null) {
            return AutoGenUtil.getTableAnnotation(clazz.getSuperclass());
        }
        return (Table) annotation;
    }

    public static String getJoinTableName(Field field) {
        JoinTable joinTableAnn = field.getDeclaredAnnotation(JoinTable.class);
        String joinTableName = null;
        if (joinTableAnn != null) {
            joinTableName = joinTableAnn.name();
        } else {
            ManyToMany manyToManyAnn = field.getDeclaredAnnotation(ManyToMany.class);
            Class sourceClass = ReflectionUtil.getClassFromParameterizedType(field);
            Optional<Field> optional = ReflectionUtil.getDeclaredFields(sourceClass).stream()
                    .filter(sourceField -> manyToManyAnn.mappedBy().equals(sourceField.getName())).findFirst();
            if (optional.isPresent()) {
                joinTableAnn = optional.get().getDeclaredAnnotation(JoinTable.class);
                joinTableName = joinTableAnn.name();
            }
        }
        return joinTableName;
    }

    public static List<Field> getFieldsNeedToWrite(Class currentClass) {
        List<Field> fields = new ArrayList<>(Arrays.asList(currentClass.getDeclaredFields()));
        if (currentClass.getSuperclass().getDeclaredAnnotation(Entity.class) == null) {
            fields.addAll(Arrays.asList(currentClass.getSuperclass().getDeclaredFields()));
        }
        return fields;
    }


}
