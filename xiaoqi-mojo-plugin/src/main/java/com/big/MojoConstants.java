package com.big;

/**
 * 静态值
 *
 * @author Yin
 * @date 2023-11-16 14:40
 */
public interface MojoConstants {
    //需要生成的mybatis的查询语句的类包
    public static final String MODEL_PACKAGE_NAME = "com.big.models";
    //自定生成的mapper文件前缀
    public static final String GENERATED_MAPPERS_NAME_PREFIX = "com.big.mappers.generated.Gen";
    //自定义生成的mapper存放的路径
    public static final String GENERATED_MAPPERS_PACKAGE_NAME = "com.big.mappers.generated";
    //文件名称的正则表达式
    public static final String FULL_CLASS_NAME_PATTERN = "(([a-zA-Z]\\w*\\.)*)([A-Z]\\w*)";
    //mapper 文件的后缀
    public static final String MAPPER_SUFFIX = "Mapper";
    //所有插件生成的文件的前缀
    public static final String GENERATED = "Gen";
}
