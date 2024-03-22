package com.datamatcher.server.controllers;


import com.datamatcher.server.repositories.DropBoxRepo;
import com.datamatcher.server.services.DropboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dropbox")
public final class DropboxController {
    private final DropboxService service;
    @Autowired
    public DropboxController(final DropboxService service){
        this.service = service;
    }
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public final DropBoxRepo.DropBoxEntity upload(@RequestParam(value = "path", defaultValue = "", required = false)final String path,
                                                  @RequestPart("file") final MultipartFile file){
        return service.upload(path, file);
    }

    @GetMapping(value = "/authorize")
    public final RedirectView authorize(@RequestParam(value = "code") final String code){
        service.reauthorizeClient(code);
        final RedirectView redirectView = new RedirectView();
        redirectView.setContextRelative(true);
        redirectView.setUrl("/");
        return redirectView;
    }


    @GetMapping(value = "/connectionStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public final Map<Object, Object> connectionStatus(){
        return Map.of("isConnected", service.isConnected());
    }

    @GetMapping(value = "/getAuthorizationUrl", produces = MediaType.TEXT_PLAIN_VALUE)
    public final String getAuthorizationUrl(){
        return service.getAuthorizationUrl();
    }

    @GetMapping(value = "/listFolder", produces = MediaType.APPLICATION_JSON_VALUE)
    public final List<DropBoxRepo.DropBoxEntity> listFolder(@RequestParam(value = "path", defaultValue = "", required = false) final String path){
        return service.listFolder(path);
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public final ResponseEntity<StreamingResponseBody> download(@RequestParam(value = "path") final String path) throws FileNotFoundException {
        final HttpHeaders httpHeaders = new HttpHeaders();
        final File file = service.getFile(path);
        final InputStream inputStream = new FileInputStream(file);
        httpHeaders.set(HttpHeaders.LAST_MODIFIED, String.valueOf(file.lastModified()));
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
        httpHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));

        final StreamingResponseBody responseBody = outputStream -> {
            int numberOfBytesToWrite;
            byte[] data = new byte[64 * 1024];
            while ((numberOfBytesToWrite = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, numberOfBytesToWrite);
            }
            inputStream.close();
        };

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }
}
