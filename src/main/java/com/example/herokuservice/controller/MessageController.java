package com.example.herokuservice.controller;

import com.example.herokuservice.model.Message;
import com.example.herokuservice.service.ConfigService;
import com.example.herokuservice.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/message")
public class MessageController extends BaseController<Message> {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService service) {
        super(service);
        messageService = service;
    }
}
