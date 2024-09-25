package com.big.plugin.service.impl;

import com.big.plugin.nums.ClassTypeEnum;
import com.big.plugin.nums.VisibilityEnum;
import com.big.plugin.service.ClassWriter;
import org.codehaus.plexus.util.StringUtils;

import javax.persistence.ManyToMany;
import java.util.Arrays;

import static com.big.MojoConstants.*;


public class MapperWriter extends ClassWriter {
    private final Class modelClass;

    public MapperWriter(Class modelClass) {
        super(GENERATED + modelClass.getSimpleName() + MAPPER_SUFFIX);
        this.setClassType(ClassTypeEnum.INTERFACE);
        this.setPackageName(GENERATED_MAPPERS_PACKAGE_NAME);
        this.modelClass = modelClass;
        this.setVisibility(VisibilityEnum.PUBLIC);
    }


    @Override
    protected void fill() {
        this.writeAnnotations();
        this.writeMethods();
    }

    private void writeMethods() {
        MethodWriter findMethod = new MethodWriter();
        StringBuilder returnType = new StringBuilder();
        returnType.append(this.addRequiredImport("java.util.List"))
                .append("<")
                .append(this.addRequiredImport(this.modelClass.getName()))
                .append(">");
        findMethod.setReturnType(returnType.toString());
        findMethod.setName("find");
        this.addRequiredImport("java.util.Map");
        findMethod.addParameter("params", "Map<String, Object>");
        findMethod.setVisibility(VisibilityEnum.PACKAGE_PROTECTED);
        this.addMethod(findMethod);

        MethodWriter getTotalMethod = new MethodWriter();
        getTotalMethod.setReturnType("Long");
        getTotalMethod.setName("getTotal");
        getTotalMethod.addParameter("params", "Map<String, Object>");
        getTotalMethod.setVisibility(VisibilityEnum.PACKAGE_PROTECTED);
        this.addMethod(getTotalMethod);

        this.writeMethodForManyToMany();
    }

    private void writeMethodForManyToMany() {
        Arrays.stream(this.modelClass.getDeclaredFields())
                .filter(filed -> filed.getDeclaredAnnotation(ManyToMany.class) != null).forEach(field -> {
            MethodWriter findMethod = new MethodWriter();
            StringBuilder returnType = new StringBuilder();
            returnType.append(this.addRequiredImport(field.getType().getName()))
                    .append("<")
                    .append(this.addRequiredImport(this.modelClass.getName()))
                    .append(">");
            findMethod.setReturnType(returnType.toString());
            String methodName = new StringBuilder("find")
                    .append(StringUtils.capitalizeFirstLetter(field.getName())).toString();
            findMethod.setName(methodName);
            findMethod.addParameter("pk", this.addRequiredImport("java.lang.Long"));
            findMethod.setVisibility(VisibilityEnum.PACKAGE_PROTECTED);
            this.addMethod(findMethod);
        });
    }

    private void writeAnnotations() {
        this.addAnnotation(this.addRequiredImport("org.apache.ibatis.annotations.Mapper"));
    }
}
