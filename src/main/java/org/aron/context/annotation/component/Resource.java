package org.aron.context.annotation.component;


import java.lang.annotation.*;

/**
 * @author: Y-Aron
 * @create: 2019-01-09 11:03
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resource {
    String value() default "";
}
