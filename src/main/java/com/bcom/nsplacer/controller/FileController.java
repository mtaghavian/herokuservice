package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.dao.FileDataDao;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.FileEntry;
import com.bcom.nsplacer.service.FileEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileEntryService fileEntryService;

    @Autowired
    private ServletContext servletContext;

    public FileController() {
    }

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @GetMapping(value = "/download/{id}")
    public ResponseEntity<InputStreamResource> download(HttpServletRequest request, HttpServletResponse response, @PathVariable UUID id) {
        try {
            FileEntry info = fileEntryService.read(id);
            if (info == null) {
                throw new RuntimeException("File not found! Id = " + id);
            }
            MediaType mediaType = getMediaType(info.getName());
            InputStreamResource resource = new InputStreamResource(fileEntryService.getInputStream(info.getId()));
            ResponseEntity<InputStreamResource> body = ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; "
                            + "filename=\"" + info.getName() + "\"; "
                            + "filename*=UTF-8''" + URLEncoder.encode(info.getName(), "UTF-8").replace("+", "%20"))
                    .contentLength(info.getLength())
                    .body(resource);
            return body;
        } catch (Exception ioex) {
            throw new RuntimeException("Exception while reading file: " + id);
        }
    }

    @PostMapping(value = "/upload")
    @ResponseBody
    public String uploadFile(HttpServletRequest request, @RequestParam("file") MultipartFile[] file) {
        UUID id = null;
        try {
            FileEntry info = new FileEntry();
            info.setName(file[0].getOriginalFilename());
            fileEntryService.create(info);
            id = info.getId();
            FileEntryService.FileDataOutputStream os = fileEntryService.getOutputStream(info.getId());
            StreamUtils.copy(file[0].getInputStream(), os, false, true);
            info.setLength(os.length());
            fileEntryService.update(info);
            return "File uploaded successfully!";
        } catch (Exception e) {
            if (id != null) {
                fileEntryService.delete(id);
            }
            return "File uploading failed!";
        }
    }

    @GetMapping("/list")
    public List<FileEntry> list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return fileEntryService.list();
    }

    @GetMapping("/delete/{id}")
    public void delete(HttpServletRequest request, HttpServletResponse response, @PathVariable UUID id) {
        fileEntryService.delete(id);
    }
}
