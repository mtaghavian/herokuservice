package com.bcom.nsplacer.service;

import com.bcom.nsplacer.dao.MessageDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author masoud
 */
@Service
public class MessageService extends BaseService {

    private final MessageDao messageDao;

    @Autowired
    public <E extends Object> MessageService(MessageDao dao) {
        super(dao);
        messageDao = dao;
    }

}
