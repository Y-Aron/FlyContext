package org.aron.context.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.aron.context.annotation.Configuration;
import org.aron.context.annotation.component.*;
import org.aron.context.error.AnnotationException;
import org.aron.context.error.BeanInstantiationException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author: Y-Aron
 * @create: 2019-02-07 21:40
 **/
@Slf4j
public class IocManager {

    // 类列表
    @Getter
    private Set<Class> classSet;

    /**
     * 类名 -> 实例
     */
    @Getter
    private Map<String, Object> ioc;

    private boolean isComponent;

    private IocManager() {
    }

    /**
     * 获取bean
     * 1. newInstance=true && clazz!=null -> 初始化新的实例，并重置ioc容器且返回新的对象实例
     * 2. beanName 存在；直接返回实例对象
     * 3. clazz != null；返回clazz类或其实现类或子类实例
     * @param beanName bean名称
     * @param clazz 目标类
     * @param newInstance 是否重新实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> clazz, boolean newInstance) throws BeanInstantiationException, AnnotationException {
        if (newInstance) {
            return (T) setBean(clazz, null, true);
        }
        if (beanName != null && this.ioc.containsKey(beanName)) {
            return (T) this.ioc.get(beanName);
        }
        if (clazz != null) {
            boolean instantiable = !unInstance(clazz);
            for (Object value : this.ioc.values()) {
                Class<?> targetClass = value.getClass();
                // 当类名一致且当前类是可实例的时候；直接返回实例对象
                if (targetClass.equals(clazz) && instantiable) {
                    return (T) value;
                }
                // 当clazz不可实例且targetClass属于clazz的实现类或子类
                if (!instantiable && clazz.isAssignableFrom(targetClass)) {
                    return (T) value;
                }
            }
        }
        return null;
    }
    public <T> T getBean(String beanName) throws BeanInstantiationException, AnnotationException {
        return getBean(beanName, null, false);
    }

    public <T> T getBean(Class<T> clazz) throws BeanInstantiationException, AnnotationException {
        return getBean(null, clazz, false);
    }


    public void removeBean(Object object) {
        this.ioc.forEach((k, v) -> {
            if (v.equals(object)) {
                this.ioc.remove(k);
            }
        });
    }

    /**
     * 将实例对象得方法返回值注入到ioc容器中
     * @param method 方法体
     * @param object 实例对象
     * @param alias 别名
     */
    public void setBean(Method method, Object object, String alias) throws AnnotationException, BeanInstantiationException {
        method.setAccessible(true);
        Class<?>[] parameters = method.getParameterTypes();
        Object[] paramValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            paramValues[i] = this.getBean(null, parameters[i], false);
            if (paramValues[i] == null) {
                throw new AnnotationException("methods whose arguments are not in the ioc container cannot be executed");
            }
        }
        try {
            Object result = method.invoke(object, paramValues);
            this.ioc.put(getBeanName(result.getClass(), alias), result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new BeanInstantiationException(e.getMessage());
        }
    }

    /**
     * 将实例对象注入到ioc容器中
     * @param instance 实例对象
     */
    public Object setBean(Class<?> clazz, Object instance, boolean isBean) throws AnnotationException, BeanInstantiationException {
        if (!isBean && clazz != null) {
            this.classSet.add(clazz);
        }
        if (instance == null && clazz != null) {
            String beanName = getBeanName(clazz, null);
            if (this.ioc.containsKey(beanName)) {
                throw new BeanInstantiationException("class["+ clazz +"] is" +
                        " are not allowed to continue injecting beans into the ioc container" );
            }
            instance = newInstance(clazz);
            if (instance == null) {
                for (Class aClass : this.classSet) {
                    if (clazz.isAssignableFrom(aClass) && !unInstance(aClass)) {
                        instance = newInstance(aClass);
                    }
                }
            }
        }
        if (instance != null) {
            clazz = clazz == null ? instance.getClass() : clazz;
            String beanName = this.getBeanName(clazz, null);
            if (!this.ioc.containsKey(beanName)) {
                this.ioc.put(beanName, instance);
                this.classSet.add(clazz);
                doAllInject();
            }
            return instance;
        }
        return null;
    }

    /**
     * 实例化IOC容器
     * 1. class 没有被@Component、@Controller、@Service、@Resource等修饰无须实例化
     * 2. class 已在ioc容器内则无需实例化 但要实现依赖注入
     * 3，实现成员变量存在@Autowired时自动依赖注入
     * @param classes 类名数组
     * @throws AnnotationException 注解异常
     * @throws BeanInstantiationException bean 实例化失败
     */
    public void doInstance(Class<?>... classes) throws AnnotationException, BeanInstantiationException {
        if (ArrayUtils.isNotEmpty(classes)) {
            this.classSet.addAll(Arrays.asList(classes));
        }
        for (Class<?> clazz : this.classSet) {
            String beanName = getBeanName(clazz, null);
            if (this.isComponent) {
                Object instance = newInstance(clazz);
                if (instance != null) {
                    this.ioc.put(beanName, instance);
                }
            }
        }
        doAllInject();
    }

    private void doAllInject() throws AnnotationException, BeanInstantiationException {
        for (Object value : this.ioc.values()) {
            doInject(value.getClass(), value);
        }
    }

    /**
     * 实现对象依赖注入
     */
    private void doInject(Class<?> clazz, Object instance) throws AnnotationException, BeanInstantiationException {
        Field[] fields = FieldUtils.getFieldsWithAnnotation(clazz, Autowired.class);
        if (fields.length > 0) {
            // 存在@Autowired注解 则表示自动注入
            for (Field field : fields) {
                inject(field, instance);
            }
        }
    }

    /**
     * 实现对象依赖注入
     * 1. 过滤static修饰的变量
     * 2. 过滤final修饰的变量
     * 3. 字段类型被@Component、@Controller、@Service、@Resource修饰的则注入
     * @param field 需要注入的字段
     * @param instance 对象实例
     */
    private void inject(Field field, Object instance) throws AnnotationException, BeanInstantiationException {
//        log.debug("----------开始依赖注入----------");
        // 过滤 static修饰的变量
        if (Modifier.isStatic(field.getModifiers())) {
            return;
        }
        // 过滤 final修饰的变量
        if (Modifier.isFinal(field.getModifiers())) {
            return;
        }
        field.setAccessible(true);

        try {
            // 判断当前字段是否已经赋值
            if (field.get(instance) == null) {
                // 获取对象类型 判断ioc容器中是否存在bean 初始化bean后给字段赋值
                Class<?> clazz = field.getType();
                Object object = this.getBean(clazz);
                if (object != null) {
                    field.set(instance, object);
                }
            }
        } catch (IllegalAccessException e) {
            throw new BeanInstantiationException(e.getMessage());
        }
//        log.debug("----------依赖注入完毕！----------");
    }

    /**
     * 判断类是否时接口 / 抽象类 / 基本类型 / 数组
     * @param clazz 类名
     */
    private boolean unInstance(final Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        int modifiers = clazz.getModifiers();
        // 接口无法实例化
        return Modifier.isInterface(modifiers)
                // 抽象类无法实例化
                || Modifier.isAbstract(modifiers)
                // 基本类型无法实例化
                || clazz.isPrimitive()
                // 数组类型无法实例化
                || clazz.isArray();
    }

    /**
     * 根据class实例化对象
     * 1. class为接口无法实例化
     * 2. class为抽象类无法实例化
     * 3. class为基本类型无法实例化
     * 4. class为数组无法实例化
     * 5. class必须有无参构造方法 否则无法实例化
     * @param clazz 类名
     * @throws BeanInstantiationException bean实例化失败
     */
    private Object newInstance(final Class<?> clazz) throws BeanInstantiationException {
        if (unInstance(clazz)) {
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeanInstantiationException(e.getMessage());
        }
    }

    /**
     * 初始化beanName
     * @param clazz 类对象
     * @param alias 别名
     */
    private String getBeanName(Class<?> clazz, String alias) throws AnnotationException {
        int count = 0;
        this.isComponent = false;
        if (clazz.isAnnotationPresent(Component.class)) {
            alias = clazz.getAnnotation(Component.class).value();
            this.isComponent = true;
            count++;
        }
        if (clazz.isAnnotationPresent(Service.class)) {
            alias = clazz.getAnnotation(Service.class).value();
            this.isComponent = true;
            count++;
        }
        if (clazz.isAnnotationPresent(Controller.class)) {
            alias = clazz.getAnnotation(Controller.class).value();
            this.isComponent = true;
            count++;
        }
        if (clazz.isAnnotationPresent(Resource.class)) {
            alias = clazz.getAnnotation(Resource.class).value();
            this.isComponent = true;
            count++;
        }
        if (clazz.isAnnotationPresent(Configuration.class)) {
            alias = clazz.getAnnotation(Configuration.class).value();
            this.isComponent = true;
            count++;
        }
        if (count <= 1) {
            return StringUtils.isNotBlank(alias) ? alias : clazz.getSimpleName();
        } else {
            throw new AnnotationException(clazz.getName() + " cannot have multiple bean component annotations");
        }
    }


    public static IocManager getInstance(Set<String> classSet) throws ClassNotFoundException {
        return Singleton.INSTANCE.getSingleton(classSet);
    }

    private enum Singleton {
        INSTANCE;
        private IocManager singleton;

        Singleton() {
            this.singleton = new IocManager();
            this.singleton.ioc = new ConcurrentHashMap<>();
        }

        public IocManager getSingleton(Set<String> classSet) throws ClassNotFoundException {
            this.singleton.classSet = new CopyOnWriteArraySet<>();
            for (String className : classSet) {
                this.singleton.classSet.add(ClassUtils.getClass(className));
            }
            return singleton;
        }
    }
}
