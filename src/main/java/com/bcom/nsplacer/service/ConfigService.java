package com.bcom.nsplacer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bcom.nsplacer.dao.ConfigDao;

/**
 * @author masoud
 */
@Service
public class ConfigService extends BaseService {

    private final ConfigDao configDao;

    @Autowired
    public <E extends Object> ConfigService(ConfigDao dao) {
        super(dao);
        configDao = dao;
    }
    
}
