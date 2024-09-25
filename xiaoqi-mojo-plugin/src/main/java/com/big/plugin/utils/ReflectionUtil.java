package com.big.plugin.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtil {
    public static List<Field> getDeclaredFields(Class clazz) {
        List<Field> fields = new ArrayList<>();

        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            } catch (Exception e) {
            }
        }
        return fields;
    }

    public static Class getClassFromParameterizedType(Field field) {
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        Class realClass = (Class) type.getActualTypeArguments()[0];
        return realClass;
    }
}

