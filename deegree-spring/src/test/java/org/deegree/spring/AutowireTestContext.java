package org.deegree.spring;

import org.deegree.spring.annotation.InjectResource;
import org.deegree.workspace.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutowireTestContext {
    
    interface TestResource extends Resource {
        
    }

    public static class TestBean {

        @InjectResource("test0")
        public TestResource testResource;
    }

    @Bean
    public TestBean testBean() {
        return new TestBean();
    }
}
