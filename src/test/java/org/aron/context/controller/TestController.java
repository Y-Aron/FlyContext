package org.aron.context.controller;

import lombok.extern.slf4j.Slf4j;
import org.aron.context.annotation.component.Autowired;
import org.aron.context.annotation.component.Controller;
import org.aron.context.service.TestService;

@Slf4j
@Controller("c1")
public class TestController {

    @Autowired
    private TestService service;

    public TestController() {
        log.debug("test controller init");
    }

    public void save() {
        log.debug("test controller save");
        service.save();
    }
}
