package org.aron.context.annotation;

import java.lang.annotation.*;

/**
 * @author: Y-Aron
 * @create: 2019-01-09 16:54
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configuration {
    String value() default "";
}
