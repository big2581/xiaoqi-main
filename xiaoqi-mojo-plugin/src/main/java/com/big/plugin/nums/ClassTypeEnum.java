package com.big.plugin.nums;

// java 的各种类型
public enum ClassTypeEnum {
    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum"),
    ANNOTATION("@interface");

    final String javaStr;

    private ClassTypeEnum(String java) {
        this.javaStr = java;
    }

    public String toString() {
        return this.javaStr;
    }
}
