package org.aron.context.controller;

import lombok.extern.slf4j.Slf4j;
import org.aron.context.annotation.component.Autowired;
import org.aron.context.annotation.component.Controller;
import org.aron.context.service.TestService;

@Slf4j
@Controller("tc2")
public class Test2Controller {

    @Autowired
    private TestService service;

    public Test2Controller() {
        log.debug("test2 controller init");
    }

    public void save() {
        log.debug("test2 controller save");
        service.save();
    }
}
