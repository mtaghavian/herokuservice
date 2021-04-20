package com.bcom.nsplacer.dao;

import com.bcom.nsplacer.model.Session;
import com.bcom.nsplacer.model.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author masoud
 */
@Component
public class SessionDao {

    private final Map<String, Session> map = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Scheduled(fixedRate = 86400000, initialDelay = 86400000)
    public void clearExpiredSessions() {
        List<Session> expired = findExpired(System.currentTimeMillis() - 86400000);
        deleteAll(expired);
    }

    public void deleteById(String id) {
        try {
            lock.writeLock().lock();
            map.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void save(Session session) {
        try {
            lock.writeLock().lock();
            map.put(session.getId(), session);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Session findById(String id) {
        try {
            lock.readLock().lock();
            return map.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void deleteAll(List<Session> list) {
        try {
            lock.writeLock().lock();
            list.stream().forEach(x -> map.remove(x.getId()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Session> findExpired(long l) {
        try {
            lock.readLock().lock();
            List<Session> list = new ArrayList<>();
            for (String id : map.keySet()) {
                if (map.get(id).getLastModified() < l) {
                    list.add(map.get(id));
                }
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Session> findByUsername(String username) {
        try {
            lock.readLock().lock();
            List<Session> list = new ArrayList<>();
            for (String id : map.keySet()) {
                User user = map.get(id).getUser();
                if ((user != null) && (user.getUsername().equals(username))) {
                    list.add(map.get(id));
                }
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }
}
