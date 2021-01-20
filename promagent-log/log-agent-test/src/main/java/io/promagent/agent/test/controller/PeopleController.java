package io.promagent.agent.test.controller;

import io.promagent.agent.test.Service.PeopleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PeopleController {

    @Autowired
    private PeopleService peopleService;

    @RequestMapping(value = "/people/{people_id}")
    public String getPeopleInfo(@PathVariable(value = "people_id") String peopleId) {
        return "hello world, this is people info of " + peopleId;
    }

    @RequestMapping(value = "/people/getException")
    public Object getException() {
        return peopleService.getException();
    }
}
