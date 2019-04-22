package org.aron.context.core.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aron.commons.io.FileUtils;
import org.aron.context.core.AbstractApplicationContext;
import org.aron.context.error.AnnotationException;
import org.aron.context.error.BeanInstantiationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.util.*;

import static org.aron.commons.utils.Utils.convertPackageToPath;
import static org.aron.commons.utils.Utils.convertPathToClassName;

@Slf4j
public class AnnotationApplicationContext extends AbstractApplicationContext {

    private String rootPath;

    public AnnotationApplicationContext() { }

    public AnnotationApplicationContext(Class<?> clazz) {
        this.appClass = clazz;
    }

    @Override
    public void init() throws BeanInstantiationException, AnnotationException, ClassNotFoundException {
        log.debug("----------开始初始化IOC容器----------");
        // 0. 获取class根路径
        if (this.appClass != null) {
            rootPath = this.appClass.getResource("").getPath().replaceAll("/", "\\" + File.separator);
        } else {
            rootPath = this.getClass().getResource("/").getPath().replaceAll("/", "\\" + File.separator);
        }
        log.debug("root path: {}", rootPath);
        // 1. 初始化所有关联的类 扫描用户设定的包下面所有的类
        this.classNames = new HashSet<>(0);
        try {
            doScanPackage(scanPackages, filterPackages);
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException();
        }
        // 2. 通过反射机制实例化类并放入ioc容器中
        // key=beanName -> value=bean
        // beanName 默认是类名
        // 如果beanName已经在容器中, 则不在添加bean
        doInstance(classNames);
        this.showBean();
        log.debug("================================================================");
        this.getIocManager().getClassSet().forEach(clazz -> log.debug("{}", clazz));
        loadConfiguration();
        log.debug("----------初始化IOC容器完毕！----------");
    }

    @Override
    public Object[] getBeanWithAnnotation(Class<? extends Annotation> annotation) {
        List<Object> list = new ArrayList<>(0);
        for (Object value : this.getIocManager().getIoc().values()) {
            if (value.getClass().isAnnotationPresent(annotation)) {
                list.add(value);
            }
        }
        return list.toArray();
    }

    @Override
    public Map<Class<?>, Object> createBeanWithAnnotation(Class<? extends Annotation> annotation) throws AnnotationException, BeanInstantiationException {
        Map<Class<?>, Object> map = new HashMap<>(0);
        Set<Class> classSet = this.getIocManager().getClassSet();
        for (Class<?> clazz : classSet) {
            if (clazz.isAnnotationPresent(annotation)) {
                map.put(clazz, this.setBean(clazz));
            }
        }
        return map;
    }

    /**
     * 实现对项目目录的扫包
     * 1. 将包名转为文件路径
     * 2. 扫描指定的包下面所有的文件路径
     * 3. 将class路径加入到classNames中
     * @param scanPackages 指定的包名
     * @param filterPackages 过滤的包名
     */
    private void doScanPackage(String[] scanPackages, String[] filterPackages) throws FileNotFoundException {
        log.debug("----------开始扫包----------");
        // 0. 将包名转为文件路径
        scanPackages = convertPackageToPath(this.rootPath, scanPackages);
        filterPackages = convertPackageToPath(this.rootPath, filterPackages);

        if (ArrayUtils.isEmpty(scanPackages)) {
            // 不指定包则扫描指定class的包
            scanPackages = new String[]{rootPath};
        }
        // 1. 扫描指定的包下面所有的文件路径
        String[] array = FileUtils.listOnPath(rootPath, scanPackages, filterPackages, suffix);

        // 2. 将文件路径转化成class名称
        array = convertPathToClassName(this.getClass().getResource("/").getPath().replaceAll("/", "\\" + File.separator), array);

        if (ArrayUtils.isNotEmpty(array)) {
            // 3. 将class添加到classNames
            this.classNames.addAll(Arrays.asList(array));
        }
//        this.classNames.forEach(vol -> log.debug("class name: {}", vol));
        log.debug("----------扫包完毕！----------");
    }
}
