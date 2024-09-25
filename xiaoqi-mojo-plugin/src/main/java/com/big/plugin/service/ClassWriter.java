package com.big.plugin.service;


import com.big.plugin.gen.java.JavaFile;
import com.big.plugin.nums.ClassTypeEnum;
import com.big.plugin.nums.VisibilityEnum;
import com.big.plugin.service.impl.MethodWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.big.MojoConstants.FULL_CLASS_NAME_PATTERN;


public abstract class ClassWriter implements CodeWriter {
    //类名
    protected final String className;
    //导包
    private Map<String, String> requiredImports = new HashMap<>();
    //
    private Map<String, String> shortNamesMap = new HashMap<>();
    //包名
    private String packageName;
    //基础的类名
    private String extendsClass;
    //接口
    private List<String> interfaces;
    //注解
    private List<String> annotations;
    //构造方法
    private List<MethodWriter> constructors;
    //普通方法
    private List<MethodWriter> methods;
    //修饰符
    private VisibilityEnum visibility;
    //静态声明
    private List<CodeWriter> constantDeclarations;
    //声明
    private List<CodeWriter> declarations;
    //文件class 类型
    private ClassTypeEnum classType;

    protected ClassWriter(String className) {
        this.className = className;
    }

    protected abstract void fill();

    @Override
    public void write(JavaFile file) {
        this.fill();
        //所在包名
        if (this.getPackageName() != null) {
            file.add(new StringBuilder().append("package ").append(this.packageName).append(";").toString());
        }
        file.add("");
        //导包
        if (MapUtils.isNotEmpty(this.requiredImports)) {
            this.requiredImports.keySet().stream().forEach(importClass -> file.add("import " + importClass + ";"));
        }
        file.add("");
        // 添加注解
        if (CollectionUtils.isNotEmpty(this.annotations)) {
            this.annotations.stream().forEach(annotation -> file.add("@" + annotation));
        }
        //类所在的一行
        file.add(new StringBuilder()
                .append(this.visibility)
                .append(this.classType)
                .append(" ")
                .append(this.className)
                .append(this.extendsClass == null ? "" : " extends " + this.extendsClass)
                .append(CollectionUtils.isEmpty(interfaces) ? "" : " implements " + this.interfaces.stream().collect(Collectors.joining(",")))
                .toString());
        file.startBlock();

        if (CollectionUtils.isNotEmpty(this.constantDeclarations)) {
            this.constantDeclarations.stream().forEach(constantDeclaration -> constantDeclaration.write(file));
        }
        if (CollectionUtils.isNotEmpty(this.declarations)) {
            this.declarations.stream().forEach(declaration -> declaration.write(file));
        }
        file.add("");
        if (CollectionUtils.isNotEmpty(this.constructors)) {
            this.constructors.stream().forEach(constructor -> constructor.write(file));
        }
        if (CollectionUtils.isNotEmpty(this.methods)) {
            this.methods.stream().forEach(method -> method.write(file));
        }

        file.endBlock();
    }

    public static String firstLetterUpperCase(String word) {
        return StringUtils.isNotBlank(word) ? word.substring(0, 1).toUpperCase() + word.substring(1) : word;
    }

    public String addRequiredImport(String type) {
        if (type != null) {
            Pattern pattern = Pattern.compile(FULL_CLASS_NAME_PATTERN);
            Matcher matcher = pattern.matcher(type);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String pkg = matcher.group(1);
                String className = matcher.group(3);
                matcher.appendReplacement(result, this.trimAndAddClass(pkg, className));
            }
            matcher.appendTail(result);
            return result.toString();
        }
        return null;
    }

    private String trimAndAddClass(String pkg, String clazzName) {
        if (pkg != null && pkg.length() != 0) {
            String fullName = pkg + clazzName;
            String mappedShortName = this.requiredImports != null ? this.requiredImports.get(fullName) : null;
            if (mappedShortName == null) {
                String mappedFullName = this.shortNamesMap != null ? this.shortNamesMap.get(clazzName) : null;
                if (mappedFullName != null && !mappedFullName.equals(fullName)) {
                    this.requiredImports.put(fullName, fullName);
                    return fullName;
                } else {
                    this.requiredImports.put(fullName, clazzName);
                    if (this.shortNamesMap == null) {
                        this.shortNamesMap = new HashMap();
                    }
                    this.shortNamesMap.put(clazzName, fullName);
                    return clazzName;
                }
            } else {
                return mappedShortName;
            }
        } else {
            return clazzName;
        }
    }

    public void addAnnotation(String annotationClass) {
        if (this.annotations == null) {
            this.annotations = new ArrayList();
        }
        this.annotations.add(this.addRequiredImport(annotationClass));
    }

    public void addConstantDeclaration(final String declaration) {
        this.addConstantDeclaration(file -> MethodWriter.writeTextToFile(file, declaration));
    }

    public void addDeclaration(final String declaration, String typeToImport) {
        if (typeToImport != null) {
            this.addRequiredImport(typeToImport);
        }
        this.addDeclaration(file -> MethodWriter.writeTextToFile(file, declaration));
    }

    public void addDeclaration(CodeWriter writer) {
        if (this.declarations == null) {
            this.declarations = new ArrayList();
        }
        this.declarations.add(writer);
    }

    public void addInterface(String interfaceClass) {
        if (this.interfaces == null) {
            this.interfaces = new ArrayList();
        }
        this.interfaces.add(this.addRequiredImport(interfaceClass));
    }

    public void addConstantDeclaration(CodeWriter writer) {
        if (this.constantDeclarations == null) {
            this.constantDeclarations = new ArrayList();
        }
        this.constantDeclarations.add(writer);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


    public void setExtendsClass(String clazz) {
        this.extendsClass = this.addRequiredImport(clazz);
    }

    public void addMethod(MethodWriter method) {
        if (this.methods == null) {
            this.methods = new ArrayList<>();
        }
        this.methods.add(method);
    }

    public void addConstructors(MethodWriter method) {
        if (this.constructors == null) {
            this.constructors = new ArrayList<>();
        }
        this.constructors.add(method);
    }

    public void setVisibility(VisibilityEnum visibility) {
        this.visibility = visibility;
    }

    public String getClassName() {
        return className;
    }

    public ClassTypeEnum getClassType() {
        return classType;
    }

    public void setClassType(ClassTypeEnum classType) {
        this.classType = classType;
    }
}
