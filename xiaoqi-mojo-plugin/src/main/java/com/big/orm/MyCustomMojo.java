package com.big.orm;

import com.big.plugin.gen.java.JavaFile;
import com.big.plugin.gen.xml.MappingWriter;
import com.big.plugin.service.ClassWriter;
import com.big.plugin.service.impl.MapperWriter;
import com.big.plugin.utils.AutoGenUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import javax.persistence.Entity;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static com.big.MojoConstants.GENERATED;
import static com.big.MojoConstants.MODEL_PACKAGE_NAME;


/*
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * @phase process-sources
 */

/*
 * mvn 插件groupId:插件artifactId[:插件版本]:插件目标名称
 * mvn com.big:study-plugin-mojo-maven-plugin:2.0.0-SNAPSHOT:generate-custom
 * <p>
 * 要是用插件前缀的话  pom.xml 中 artifactId 格式[xxx-maven-plugin] 此时前缀：xxx 命令：mvn xxx:goal
 * 当前项目插件执行  mvn xiaoqi-plugin-mojo:generate-custom
 *
 * @author Yin
 * @apiNote @Mojo 注解用来标注这个类是一个目标类，maven对插件进行构建的时候会根据这个注解来找到这个插件的目标，这个注解中还有其他参数
 * @date 2023/11/16 14:16
 */
@Mojo(name = "generate-custom")
//根据实体类生成mybatis 需要的 mapper.xml interface namespace
public class MyCustomMojo extends AbstractMojo {

    //    @Parameter(name = "echo1", required = false, defaultValue = "hello custom maven mojo")
//    private String echoStr;
    //扫描定位的包 下面所有的类
    private static final String MODELS_DIR = ".src.main.java.com.big.models"
            .replace(".", File.separator);
    //定义mapper 包的位置
    private static final String MAPPERS_DIR = ".src.main.java.com.big.mappers.generated"
            .replace(".", File.separator);
    //定义mapping 的位置
    private static final String MAPPING_DIR = ".src.main.resources.mapping.generated"
            .replace(".", File.separator);

    private Map<String, List<Class>> classPathMapper = new HashMap<>();

    public void execute() throws MojoExecutionException {

        MavenProject mavenProject = (MavenProject) this.getPluginContext().get("project");

        if (mavenProject.isExecutionRoot()) {
            return;
        }

        this.getLog().info("Generating orm " + mavenProject.getName() + " components...");
        MavenProject parentProject = mavenProject.getParent();
        ArrayList<URL> urls = new ArrayList<>();

        for (MavenProject collectedProject : parentProject.getCollectedProjects()) {
            List<String> runtimeClasspathElements = null;
            try {
                runtimeClasspathElements = collectedProject.getRuntimeClasspathElements();
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = runtimeClasspathElements.get(i);
                    urls.add(new File(element).toURI().toURL());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                Thread.currentThread().getContextClassLoader());
        File modelsDir = new File(mavenProject.getBasedir() + MODELS_DIR);

        this.getLog().info("------------" + mavenProject.getBasedir() + MODELS_DIR);
        if (modelsDir.exists() && modelsDir.isDirectory()) {
            classPathMapper.put(mavenProject.getBasedir().getAbsolutePath(), this.getModelClasses(classLoader, modelsDir));
        }

        classPathMapper.forEach((path, modelClassList) -> {
            if (CollectionUtils.isNotEmpty(modelClassList)) {
                File mapperDir = AutoGenUtil.clearDir(new File(path + MAPPERS_DIR));
                File mappingDir = AutoGenUtil.clearDir(new File(path + MAPPING_DIR));

                for (Class modelClass : modelClassList) {
                    this.getLog().info("Handling entity:" + modelClass.getSimpleName());
                    MapperWriter mapperWriter = new MapperWriter(modelClass);
                    this.getLog().info("Writing mapper...");
                    this.writeJavaFile(mapperWriter, mapperDir);
                    this.getLog().info("Writing mapping...");
                    this.generateMapping(mappingDir, modelClass);
                }
            }
        });
    }

    private void writeJavaFile(ClassWriter writer, File targetDir) {
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        File targetFile = new File(targetDir, writer.getClassName() + ".java");
        if (!targetFile.exists()) {
            JavaFile src = new JavaFile();
            writer.write(src);
            this.writeToFile(src, targetFile);
        }
    }

    private void generateMapping(File targetDir, Class modelClass) {
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        File mappingFile = new File(targetDir, GENERATED + modelClass.getSimpleName() + "Mapping.xml");
        MappingWriter writer = new MappingWriter(modelClass, mappingFile);
        try {
            writer.write();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<Class> getModelClasses(URLClassLoader classLoader, File modelsDir) {
        List<Class> classList = new ArrayList<>();
        Arrays.stream(modelsDir.listFiles())
                .filter(modelClassFile -> modelClassFile.isFile())
                .forEach(modelClassFile -> {
                    try {
                        Class<?> clazz = classLoader.loadClass(MODEL_PACKAGE_NAME + '.' + StringUtils.substringBefore(modelClassFile.getName(), "."));
                        if (clazz.getDeclaredAnnotation(Entity.class) != null) {
                            classList.add(clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
        classList.sort((o1, o2) -> {
            if (o1.isAssignableFrom(o2)) return -1;
            if (o2.isAssignableFrom(o1)) return 1;
            if (o1.getSuperclass().getDeclaredAnnotation(Entity.class) != null) {
                return o2.getSuperclass().getDeclaredAnnotation(Entity.class) != null ? 0 : 1;
            }
            if (o2.getSuperclass().getDeclaredAnnotation(Entity.class) != null) {
                return o1.getSuperclass().getDeclaredAnnotation(Entity.class) != null ? 0 : -1;
            }
            return 0;
        });
        return classList;
    }

    private void writeToFile(JavaFile src, File targetFile) {
        try {
            PrintWriter writer = new PrintWriter(targetFile, "utf-8");
            Iterator var4 = src.getLines().iterator();

            while (var4.hasNext()) {
                String line = (String) var4.next();
                writer.println(line);
            }
            writer.close();
        } catch (FileNotFoundException var5) {
            throw new IllegalArgumentException("unexpected error : " + var5.getMessage(), var5);
        } catch (UnsupportedEncodingException var6) {
            throw new IllegalArgumentException("unexpected error : " + var6.getMessage(), var6);
        }
    }
}
