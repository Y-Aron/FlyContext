package org.aron.context.annotation.component;

import java.lang.annotation.*;

/**
 * @author: Y-Aron
 * @create: 2019-01-03 13:18
 **/
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}
