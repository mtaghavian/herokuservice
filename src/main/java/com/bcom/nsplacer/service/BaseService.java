package com.bcom.nsplacer.service;

import com.bcom.nsplacer.model.BaseModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author masoud
 */
public abstract class BaseService<E extends BaseModel> {

    private final JpaRepository<E, UUID> repo;

    public <E> BaseService(JpaRepository repo) {
        this.repo = repo;
    }

    public List<E> list() {
        List<E> all = repo.findAll();
        Collections.sort(all);
        return all;
    }

    public E create(E e) {
        return repo.save(e);
    }

    public E read(UUID id) {
        Optional<E> findById = repo.findById(id);
        return findById.isPresent() ? findById.get() : null;
    }

    public E update(E e) {
        return repo.save(e);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }

    public void deleteAll() {
        repo.deleteAll();
    }

}
