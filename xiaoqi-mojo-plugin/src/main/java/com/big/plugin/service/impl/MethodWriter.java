package com.big.plugin.service.impl;

import com.big.plugin.gen.java.JavaFile;
import com.big.plugin.nums.VisibilityEnum;
import com.big.plugin.service.ClassWriter;
import com.big.plugin.service.CodeWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MethodWriter implements CodeWriter {
    //方法名
    private String name;
    //返回类型
    private String returnType;
    //修饰符
    private VisibilityEnum visibility;
    //异常类
    private Set<String> thrownExceptions;
    //参数
    private Map<String, String> params;
    //注解
    private Set<String> annotations;
    //内容
    private CodeWriter content;
    //标记是否构造方法
    private boolean constructor;
    //修改的位置
    private int modifier = 0;

    @Override
    public void write(JavaFile file) {
        if (CollectionUtils.isNotEmpty(this.annotations)) {
            this.annotations.stream().forEach(annotation -> file.add("@" + annotation));
        }
        StringBuilder header = new StringBuilder()
                .append(this.visibility)
                .append(this.modifier > 0 ? this.modifiersToString(this.modifier) : "")
                .append(this.returnType)
                .append(this.constructor ? "" : " " + this.name)
                .append("(")
                .append(this.assembleParams())
                .append(")")
                .append(this.assembleThrowsClause());
        if (this.content != null) {
            file.add(header.toString());
            file.startBlock();
            this.content.write(file);
            file.endBlock();
            file.add("");
        } else {
            header.append(";");
            file.add(header.toString());
        }
    }

    private String assembleThrowsClause() {
        if (CollectionUtils.isNotEmpty(this.thrownExceptions)) {
            StringBuilder result = new StringBuilder(" throws ");
            result.append(this.thrownExceptions.stream().collect(Collectors.joining(",")));
            return result.toString();
        }
        return "";
    }

    private String assembleParams() {
        if (MapUtils.isNotEmpty(this.params)) {
            StringBuilder result = new StringBuilder();
            result.append(this.params.keySet().stream().map(key -> this.params.get(key) + " " + key).collect(Collectors.joining(",")));
            return result.toString();
        }
        return "";
    }

    public static String generateGetterMethodName(String qualifier, boolean booleanAttribute) {
        return (booleanAttribute ? "is" : "get") + ClassWriter.firstLetterUpperCase(qualifier);
    }

    public static String generateSetterMethodName(String qualifier) {
        return "set" + ClassWriter.firstLetterUpperCase(qualifier);
    }

    public static void writeTextToFile(JavaFile file, String plainText) {
        StringTokenizer st = new StringTokenizer(plainText, "\n");

        while (st.hasMoreTokens()) {
            String txt = st.nextToken().trim();
            if ("{".equals(txt)) {
                file.startBlock();
            } else if ("}".equals(txt)) {
                file.endBlock();
            } else {
                file.add(txt);
            }
        }
    }

    protected void writeContent(JavaFile file) {
        if (this.content != null) {
            this.content.write(file);
        }
    }

    public static String modifiersToString(int modifiers) {
        if (modifiers > 0) {
            StringBuilder result = new StringBuilder();
            if ((modifiers & 8) == 8) {
                result.append(" abstract ");
            }

            if ((modifiers & 2) == 2) {
                result.append(" static ");
            }

            if ((modifiers & 1) == 1) {
                result.append(" final ");
            }

            if ((modifiers & 4) == 4) {
                result.append(" synchronized ");
            }

            return result.toString();
        } else {
            return "";
        }
    }

    public void setContentPlain(final String plainText) {
        this.setContent(file -> MethodWriter.writeTextToFile(file, plainText));
    }

    public void addAnnotation(String annotation) {
        if (this.annotations == null) {
            this.annotations = new LinkedHashSet();
        }
        this.annotations.add(annotation);
    }

    public void addParameter(String name, String type) {
        if (this.params == null) {
            this.params = new LinkedHashMap();
        } else if (this.params.containsKey(name)) {
            throw new IllegalArgumentException("method " + this + " already contains parameter '" + name + "'");
        }
        this.params.put(name, type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public VisibilityEnum getVisibility() {
        return visibility;
    }

    public void setVisibility(VisibilityEnum visibility) {
        this.visibility = visibility;
    }

    public Set<String> getThrownExceptions() {
        return thrownExceptions;
    }

    public void setThrownExceptions(Set<String> thrownExceptions) {
        this.thrownExceptions = thrownExceptions;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public CodeWriter getContent() {
        return content;
    }

    public void setContent(CodeWriter content) {
        this.content = content;
    }

    public int getModifier() {
        return modifier;
    }

    public void setModifier(int modifier) {
        this.modifier = modifier;
    }
}
