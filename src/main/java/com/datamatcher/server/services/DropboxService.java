package com.datamatcher.server.services;

import com.datamatcher.server.repositories.DropBoxRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.List;

@Service
public final class DropboxService {
    private final DropBoxRepo repo;
    @Autowired
    public DropboxService(final DropBoxRepo repo){
        this.repo = repo;
    }


    public final boolean isConnected(){
        return repo.isConnected();
    }
    public final void reauthorizeClient(final String code){
        repo.reauthorizeClient(code);
    }

    public final String getAuthorizationUrl(){
        return String.format("https://www.dropbox.com/oauth2/authorize?client_id=%s&response_type=code&redirect_uri=%s", repo.getAppKey(), repo.getRedirectUri());
    }

    public final DropBoxRepo.DropBoxEntity upload(final String path, final MultipartFile file){
        if (repo.doesFileExists(path)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File '" + path + "' already exists.");
        }
        return repo.upload(path, file);
    }

    public final List<DropBoxRepo.DropBoxEntity> listFolder(final String path){
        return repo.listDirectory(path);
    }

    public final File getFile(final String path){
        return repo.getDropboxFile(path);
    }
}
