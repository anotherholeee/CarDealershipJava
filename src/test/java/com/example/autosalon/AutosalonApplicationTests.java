package com.example.autosalon;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Unit-test suite: integration context requires DB config")
class AutosalonApplicationTests {

    @Test
    void contextLoads() {
    }
}