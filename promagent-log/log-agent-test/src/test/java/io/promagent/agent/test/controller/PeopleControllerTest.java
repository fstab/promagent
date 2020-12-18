package io.promagent.agent.test.controller;

import com.ejlchina.okhttps.OkHttps;
import io.promagent.agent.test.LogAgentTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {LogAgentTestApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PeopleControllerTest {

    @Autowired
    public Environment environment;

    private String port;


    @BeforeEach
    public void setPort() {
        if (StringUtils.isEmpty(port)) {
            port = environment.getProperty("local.server.port");
        }
    }

    @Test
    public void getPeopleInfo() {
        String result = OkHttps.sync("http://127.0.0.1:" + port + "/people/12")
                .addHeader("sign","sign")
                .get()
                .getBody()
                .toString();
        System.out.println(result);
    }

    @Test
    public void getException() {
        String result = OkHttps.sync("http://127.0.0.1:" + port + "/people/getException")
                .addHeader("sign","sign")
                .get()
                .getBody()
                .toString();
        System.out.println(result);
    }

    @Test
    public void testParams() {
        String result = OkHttps.sync("http://127.0.0.1:" + port + "/people/12?name=name&age=12")
                .addHeader("sign","sign")
                .get()
                .getBody()
                .toString();
        System.out.println(result);
    }
}