package com.datamatcher.server.repositories;

import com.datamatcher.server.utils.JSON;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public final class DropBoxRepo {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DbxClientV2 client;
    @Autowired
    public DropBoxRepo(@Value("${dropbox.access_token}") final String accessToken) throws DbxException {
        final DbxRequestConfig config = DbxRequestConfig.newBuilder("dataMining/shit").build();
        this.client = new DbxClientV2(config, accessToken);
    }

    public final List<DropBoxEntity> listDirectory(final String path){
        try {
            final List<DropBoxEntity> res = new ArrayList<>();
            ListFolderResult result = client.files().listFolder(path);
            for(;;) {
                for (final Metadata metadata : result.getEntries()) {
                    if (metadata instanceof FileMetadata){
                        res.add(new DropBoxEntity(DropBoxEntity.EntityType.FILE, metadata.getPathDisplay()));
                    } else if (metadata instanceof FolderMetadata) {
                        res.add(new DropBoxEntity(DropBoxEntity.EntityType.DIRECTORY, metadata.getPathDisplay()));
                    }
                }
                if (!result.getHasMore()) {
                    break;
                }
                result = client.files().listFolderContinue(result.getCursor());
            }
            return res;
        }catch (final Throwable cause){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list dropbox directory", cause);
        }
    }

    public final DropBoxEntity upload(final String path, final MultipartFile file){
        try(final InputStream inputStream = file.getInputStream()){
            final FileMetadata metadata = client.files().uploadBuilder(path).uploadAndFinish(inputStream);
            return new DropBoxEntity(DropBoxEntity.EntityType.FILE, metadata.getPathDisplay());
        }catch (final Throwable cause){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to dropbox", cause);
        }
    }

    public final boolean doesFileExists(final String path){
        try {
            final Metadata metadata = client.files().getMetadata(path);
            return metadata instanceof FileMetadata;
        }catch (final Throwable cause){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check if file exists in dropbox", cause);
        }
    }

    public final long getFileSize(final String path){
        try {
            final FileMetadata metadata = (FileMetadata) client.files().getMetadata(path);
            return metadata.getSize();
        }catch (final Throwable cause){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check file size in dropbox", cause);
        }
    }

    public final File getDropboxFile(final String path){
        try(final DbxDownloader<FileMetadata> downloader = client.files().download(path)){
            final File file = File.createTempFile("dropbox", "tmp");
            file.deleteOnExit();
            java.nio.file.Files.copy(
                    downloader.getInputStream(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return file;
        }catch (final Throwable cause){
            logger.error("Failed to download file '{}' from dropbox.", path, cause);
            throw new RuntimeException(cause);
        }
    }


    public static final class DropBoxEntity{
        public EntityType type;
        public String path;
        public DropBoxEntity(){}
        public DropBoxEntity(final EntityType type, final String path){
            this.type = type;
            this.path = path;
        }

        public static enum EntityType{
            FILE, DIRECTORY
        }
    }
}
