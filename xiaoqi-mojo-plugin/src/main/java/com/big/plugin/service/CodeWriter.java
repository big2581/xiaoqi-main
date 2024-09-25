package com.big.plugin.service;


import com.big.plugin.gen.java.JavaFile;

/*
 * 自动生成文件 接口
 *
 * @author Yin
 * @date 2023-11-16 17:03
 */
public interface CodeWriter {
    /*
     * @apiNote 编写java文件
     * @param file 封装的文件对象
     * @author Yin
     * @date 2023/11/16 17:08
     */
    void write(JavaFile file);
}
