package com.example.herokuservice.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.herokuservice.model.Config;
import com.example.herokuservice.service.ConfigService;

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
