package com.big.plugin.gen.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 定义文件类
 *
 * @author Yin
 * @date 2023-11-16 17:07
 */
public class JavaFile {
    //当前行的数据字符 一个字符代表一个位置
    private final List<String> lines = new ArrayList();
    //空位置 为了添加 "\t"
    private String indent = "";

    //单个添加
    public void add(String newLine) {
        this.lines.add(this.indent + newLine);
    }

    //批量添加
    public void addAll(List<String> newLines) {
        Iterator iter = newLines.iterator();

        while (iter.hasNext()) {
            String next = (String) iter.next();
            this.add(next);
        }

    }

    //添加括号 开始
    public void startBlock() {
        this.startBlock("{");
    }

    //
    public void startBlock(String bracket) {
        if (bracket != null) {
            this.add(bracket);
        }

        this.indent = this.indent + "\t";
    }

    //结束括号
    public void endBlock() {
        this.endBlock("}");
    }

    public void endBlock(String bracket) {
        this.indent = this.indent.substring("\t".length());
        if (bracket != null) {
            this.add(bracket);
        }
    }

    // 获取当前行的数据
    public List<String> getLines() {
        return Collections.unmodifiableList(this.lines);
    }
}
