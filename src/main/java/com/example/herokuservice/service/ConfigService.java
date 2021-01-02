package com.example.herokuservice.service;

import com.example.herokuservice.model.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.herokuservice.dao.ConfigDao;

/**
 * @author masoud
 */
@Service
public class ConfigService extends BaseService<Config> {

    private final ConfigDao configDao;

    @Autowired
    public ConfigService(ConfigDao dao) {
        super(dao);
        configDao = dao;
    }
}
