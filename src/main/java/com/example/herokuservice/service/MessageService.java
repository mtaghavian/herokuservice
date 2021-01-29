package com.example.herokuservice.service;

import com.example.herokuservice.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.herokuservice.dao.MessageDao;

/**
 * @author masoud
 */
@Service
public class MessageService extends BaseService<Message> {

    private final MessageDao messageDao;

    @Autowired
    public MessageService(MessageDao dao) {
        super(dao);
        messageDao = dao;
    }

}
