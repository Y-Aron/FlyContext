package org.aron.context.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.aron.context.annotation.Configuration;
import org.aron.context.annotation.component.Autowired;
import org.aron.context.annotation.component.Bean;
import org.aron.context.error.AnnotationException;
import org.aron.context.error.BeanInstantiationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public abstract class AbstractApplicationContext implements ApplicationContext {
    protected static final String suffix = "class";

    protected boolean loadConfiguration = false;

    protected String[] scanPackages;

    protected String[] filterPackages;

    protected Set<String> classNames;

    @Getter
    protected Class<?> appClass;

    @Getter
    private IocManager iocManager;

    @Override
    public void setScanPackages(String[] scanPackages) {
        this.scanPackages = scanPackages;
    }

    @Override
    public void setFilterPackages(String[] filterPackages) {
        this.filterPackages = filterPackages;
    }

    @Override
    public void isloadConfiguration(boolean auto) {
        this.loadConfiguration = auto;
    }

    @Override
    public void doInstance(Set<String> classNames) throws BeanInstantiationException, AnnotationException, ClassNotFoundException {
        iocManager = IocManager.getInstance(classNames);
        iocManager.doInstance();
    }

    @Override
    public void loadConfiguration() throws BeanInstantiationException, AnnotationException {
        if (!loadConfiguration) { return; }
        log.debug("----------开始加载@Configuration的注解类----------");
        Object[] objects = this.getBeanWithAnnotation(Configuration.class);
        for (Object object : objects) {
            Method[] methods = object.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Bean.class) || method.isAnnotationPresent(Autowired.class)) {
                    // 过滤静态方法
                    if (Modifier.isStatic(method.getModifiers())) {
                        throw new AnnotationException("@Bean cannot be configured on a static modified method");
                    }
                    Class<?> clazz = method.getReturnType();
                    // bean对象已经存在于ioc容器中 抛出异常
                    if (this.getBean(clazz) != null) {
                        throw new BeanInstantiationException(clazz + " is already exists in the ioc container");
                    }
                    Bean bean;
                    String alias = null;
                    if ((bean = method.getAnnotation(Bean.class)) != null) {
                        alias = bean.value();
                    }
                    this.iocManager.setBean(method, object, alias);
                }
            }
            this.iocManager.removeBean(object);
        }
        log.debug("----------加载@Configuration的注解类完毕！----------");
    }

    public void showBean() {
        this.iocManager.getIoc().forEach((name, bean) -> log.debug("beanName: {}, bean: {}", name, bean));
    }

    @Override
    public <T> T setBean(Object instance) throws AnnotationException, BeanInstantiationException {
        return this.setBean(null, instance);
    }

    @Override
    public <T> T setBean(Class<T> clazz) throws AnnotationException, BeanInstantiationException {
        return this.setBean(clazz, null);
    }

    @Override
    public void setBean(Class<?> clazz, boolean newInstance) throws AnnotationException, BeanInstantiationException {
        this.iocManager.setBean(clazz, null, newInstance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T setBean(Class<T> clazz, Object instance) throws AnnotationException, BeanInstantiationException {
        return (T) this.iocManager.setBean(clazz, instance, false);
    }

    @Override
    public <T> T getBean(Class<T> clazz) throws BeanInstantiationException, AnnotationException {
        return this.iocManager.getBean(clazz);
    }

    @Override
    public <T> T getBean(String beanName) throws BeanInstantiationException, AnnotationException {
        return this.iocManager.getBean(beanName);
    }

    @Override
    public <T> T getBean(Class<T> clazz, boolean newInstance) throws BeanInstantiationException, AnnotationException {
        return this.iocManager.getBean(null, clazz, newInstance);
    }

    @Override
    public Collection<Object> getAllBean() {
        return this.iocManager.getIoc().values();
    }

    @Override
    public Set<Class<?>> getClassWithAnnotation(Class<? extends Annotation> annotation) {
        Set<Class<?>> classSet = new HashSet<>(0);
        for (Class clazz : this.iocManager.getClassSet()) {
            if (clazz.isAnnotationPresent(annotation)) {
                classSet.add(clazz);
            }
        }
        return classSet;
    }


    @Override
    public Set<Class<?>> getClassByPackage(String ... packages) throws ClassNotFoundException {
        Set<Class<?>> classSet = new HashSet<>(0);
        if (ArrayUtils.isEmpty(packages)) {
            return classSet;
        }
        for (String className : this.classNames) {
            for (String packageName : packages) {
                if (className.startsWith(packageName)) {
                    classSet.add(ClassUtils.getClass(className));
                }
            }
        }
        return classSet;
    }
}
