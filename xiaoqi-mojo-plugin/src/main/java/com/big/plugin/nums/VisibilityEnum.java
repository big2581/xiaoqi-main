package com.big.plugin.nums;

//修饰符枚举
public enum VisibilityEnum {
    PUBLIC("public "),
    PROTECTED("protected "),
    PRIVATE("private "),
    PACKAGE_PROTECTED("");

    final String javaStr;

    private VisibilityEnum(String java) {
        this.javaStr = java;
    }

    @Override
    public String toString() {
        return this.javaStr;
    }
}