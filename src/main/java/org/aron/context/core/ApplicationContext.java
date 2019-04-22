package org.aron.context.core;

import org.aron.context.error.AnnotationException;
import org.aron.context.error.BeanInstantiationException;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ApplicationContext {

    void init() throws BeanInstantiationException, AnnotationException, ClassNotFoundException;

    void loadConfiguration() throws BeanInstantiationException, AnnotationException;

    /**
     * 实现实例化 classNames下的所有class
     */
    void doInstance(Set<String> classNames) throws BeanInstantiationException, AnnotationException, ClassNotFoundException;

    <T> T getBean(Class<T> clazz) throws BeanInstantiationException, AnnotationException;

    <T> T getBean(String beanName) throws BeanInstantiationException, AnnotationException;

    <T> T getBean(Class<T> clazz, boolean newInstance) throws BeanInstantiationException, AnnotationException;

    <T> T setBean(Class<T> clazz) throws AnnotationException, BeanInstantiationException;

    void setBean(Class<?> clazz, boolean newInstance) throws AnnotationException, BeanInstantiationException;

    <T> T setBean(Class<T> clazz, Object instance) throws AnnotationException, BeanInstantiationException;

    <T> T setBean(Object instance) throws AnnotationException, BeanInstantiationException;

    Map<Class<?>, Object> createBeanWithAnnotation(Class<? extends Annotation> annotation) throws AnnotationException, BeanInstantiationException;

    Object[] getBeanWithAnnotation(Class<? extends Annotation> annotation);

    Collection<Object> getAllBean();

    Set<Class<?>> getClassWithAnnotation(Class<? extends Annotation> annotation);

    void isloadConfiguration(boolean auto);

    void setScanPackages(String[] scanPackages);

    void setFilterPackages(String[] filterPackages);

    Set<Class<?>> getClassByPackage(String ... pkg) throws ClassNotFoundException;
}
