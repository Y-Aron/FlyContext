package org.aron.context.annotation.component;

import java.lang.annotation.*;

/**
 * @author: Y-Aron
 * @create: 2019-01-09 16:56
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    String value() default "";
}
