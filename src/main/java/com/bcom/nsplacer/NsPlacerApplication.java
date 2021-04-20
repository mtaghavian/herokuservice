package com.bcom.nsplacer;

import com.bcom.nsplacer.config.HttpInterceptor;
import com.bcom.nsplacer.dao.ConfigDao;
import com.bcom.nsplacer.dao.UserDao;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EnableScheduling
public class NsPlacerApplication implements ApplicationContextAware {

    public static final String adminUsername = "admin";

    public static ObjectMapper jsonMapper;
    private static ApplicationContext context;

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private UserDao userDao;

    public static void main(String[] args) {
        SpringApplication.run(NsPlacerApplication.class, args);
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> requiredType) {
        return context.getBean(requiredType);
    }

    @PostConstruct
    public void starter() {
        if (userDao.findAll().isEmpty()) {
            User user = new User();
            user.setUsername(adminUsername);
            user.setPassword(StreamUtils.hash("mnts"));
            user.setFirstname("Masoud");
            user.setLastname("Taghavian");
            userDao.save(user);

            user = new User();
            user.setUsername("placer");
            user.setPassword(StreamUtils.hash("bcom"));
            user.setFirstname("Masoud");
            user.setLastname("Taghavian");
            userDao.save(user);
        }
        jsonMapper = getBean(ObjectMapper.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        context = ac;
    }

    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(10000000000l);
        return multipartResolver;
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
