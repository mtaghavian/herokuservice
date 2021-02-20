package com.example.herokuservice;

import com.example.herokuservice.config.HttpInterceptor;
import com.example.herokuservice.dao.ConfigDao;
import com.example.herokuservice.model.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EnableScheduling
public class HerokuserviceApplication implements ApplicationContextAware {

    public static ObjectMapper jsonMapper;
    public static ExecutorService sendExecutor;
    private static ApplicationContext context;

    @Autowired
    private ConfigDao configDao;

    public static void main(String[] args) {
        SpringApplication.run(HerokuserviceApplication.class, args);
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    @PostConstruct
    public void starter() {
        if (configDao.findAll().isEmpty()) {
            Config config = new Config();
            config.setName("Param1");
            config.setValue("1");
            configDao.save(config);
        }
        jsonMapper = getBean(ObjectMapper.class);
        sendExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        context = ac;
    }

    @Configuration
    public class InterceptorConfig extends WebMvcConfigurerAdapter {

        @Autowired
        HttpInterceptor serviceInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(serviceInterceptor);
        }
    }
}
