package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.model.BaseModel;
import com.bcom.nsplacer.service.BaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author masoud
 */
public abstract class BaseController<E extends BaseModel> {

    private final BaseService<E> service;

    public BaseController(BaseService<E> service) {
        this.service = service;
    }

    @GetMapping("/list")
    public List<E> list(HttpServletRequest request, HttpServletResponse response) {
        return service.list();
    }

    @PostMapping("/create")
    public E create(HttpServletRequest request, HttpServletResponse response, @RequestBody E e) {
        return service.create(e);
    }

    @GetMapping("/read/{id}")
    public E read(HttpServletRequest request, HttpServletResponse response, @PathVariable UUID id) {
        return service.read(id);
    }

    @PostMapping("/update")
    public E update(HttpServletRequest request, HttpServletResponse response, @RequestBody E e) {
        return service.update(e);
    }

    @GetMapping("/delete/{id}")
    public void delete(HttpServletRequest request, HttpServletResponse response, @PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/deleteAll")
    public void deleteAll(HttpServletRequest request, HttpServletResponse response) {
        service.deleteAll();
    }

}
