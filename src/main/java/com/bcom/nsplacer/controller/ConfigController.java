package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.model.Config;
import com.bcom.nsplacer.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController extends BaseController<Config> {

    private final ConfigService configService;

    @Autowired
    public ConfigController(ConfigService service) {
        super(service);
        configService = service;
    }
}
