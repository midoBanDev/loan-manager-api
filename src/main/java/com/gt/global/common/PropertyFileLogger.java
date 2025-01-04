package com.gt.global.common;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PropertyFileLogger implements CommandLineRunner{

    private final Environment env;

    public PropertyFileLogger(Environment env) {
        this.env = env;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Active Profiles ===");    
        Arrays.stream(env.getActiveProfiles()).forEach(i -> System.out.println("Active Profile: " + i));
        
        System.out.println("=== Property Files ===");
        ((ConfigurableEnvironment) env).getPropertySources()
            .forEach(source -> System.out.println("Property Source: " + source.getName()));
    }


}
