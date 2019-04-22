package org.aron.context;

import lombok.extern.slf4j.Slf4j;
import org.aron.context.annotation.component.Controller;
import org.aron.context.config.TestConfig;
import org.aron.context.controller.Test2Controller;
import org.aron.context.controller.TestController;
import org.aron.context.core.impl.AnnotationApplicationContext;
import org.aron.context.error.AnnotationException;
import org.aron.context.error.BeanInstantiationException;
import org.aron.context.service.TestService;
import org.junit.Test;

@Slf4j
public class Application {

    @Test
    public void test() throws AnnotationException, BeanInstantiationException, ClassNotFoundException {
        AnnotationApplicationContext context = new AnnotationApplicationContext(Application.class);
        context.isloadConfiguration(false);
        context.init();
        context.setBean(TestConfig.class);

    }

    public static void main(String[] args) throws AnnotationException, BeanInstantiationException, ClassNotFoundException {
        AnnotationApplicationContext context = new AnnotationApplicationContext(Test2Controller.class);
        context.isloadConfiguration(true);
        context.init();

        TestController controller = context.getBean(TestController.class);

        TestController controller1 = context.getBean(TestController.class);

        log.debug("{}, {}", controller, controller1);
        controller.save();

        TestService service = context.getBean(TestService.class);
        service.save();

        Test2Controller tc2 = context.getBean("tc2");
        tc2.save();

        Object[] objects = context.getBeanWithAnnotation(Controller.class);
        for (Object object : objects) {
            log.debug("o: {}", object);
        }
    }
}
