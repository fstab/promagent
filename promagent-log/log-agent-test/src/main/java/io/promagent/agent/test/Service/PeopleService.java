package io.promagent.agent.test.Service;

import org.springframework.stereotype.Service;

@Service
public class PeopleService {
    public Object getException() {
        throw new RuntimeException("PeopleService getException test");
    }
}
