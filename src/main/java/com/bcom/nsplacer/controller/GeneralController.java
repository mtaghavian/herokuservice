package com.bcom.nsplacer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/general")
public class GeneralController {

    @GetMapping("/specs")
    public String specs(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        sb.append("availableProcessors: " + runtime.availableProcessors()).append("\n");
        sb.append("freeMemory: " + runtime.freeMemory()).append("\n");
        sb.append("maxMemory: " + runtime.maxMemory()).append("\n");
        sb.append("totalMemory: " + runtime.totalMemory()).append("\n");
        File file = new File(".");
        sb.append("getFreeSpace: " + file.getFreeSpace()).append("\n");
        sb.append("getUsableSpace: " + file.getUsableSpace()).append("\n");
        sb.append("getTotalSpace: " + file.getTotalSpace()).append("\n");
        for (Object prop : System.getProperties().keySet()) {
            if (("" + prop).contains(".path") || ("" + prop).contains("line.separator")) {
                continue;
            }
            sb.append(prop + ": " + System.getProperty(prop.toString())).append("\n");
        }
        return sb.toString();
    }
}
