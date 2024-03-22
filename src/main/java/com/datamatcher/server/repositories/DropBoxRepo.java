package com.datamatcher.server.repositories;

import com.datamatcher.server.utils.JSON;
import com.dropbox.core.*;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public final class DropBoxRepo {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String appKey;
    private final String appSecret;
    private volatile DbxClientV2 _client;
    private final String database;
    private final String userName;
    private final String password;
    private final String uri;
    @Autowired
    public DropBoxRepo(
            @Value("${dropbox.appKey}") final String appKey,
            @Value("${dropbox.appSecret}") final String appSecret,
            @Value("${neo4j.uri}") final String uri,
            @Value("${neo4j.user}") final String userName,
            @Value("${neo4j.password}") final String password,
            @Value("${neo4j.db}") final String db) {
        this.uri = uri;
        this.userName = userName;
        this.password = password;
        this.database = db;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this._client = null;
    }

    public final String getRedirectUri(){
        return "http://localhost:8080/dropbox/authorize";
    }

    private final void saveCredentials(final String accessToken, final Long expiresAt, final String refreshToken){
        try (final Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(userName, password))) {
            try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
                session.executeWrite(tx -> tx.run("""
                        MERGE (n:DROPBOX_CREDENTIALS {appKey: $appKey})
                        SET n.accessToken = $accessToken
                        SET n.expiresAt = $expiresAt
                        SET n.refreshToken = $refreshToken
                        """, Map.of("appKey", appKey,
                        "accessToken", accessToken == null?"":accessToken,
                        "expiresAt", expiresAt == null?-1:expiresAt,
                        "refreshToken", refreshToken == null?"":refreshToken)).consume());
            } catch (final Throwable cause) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed create credentials record.", cause);
            }
        }
    }

    private final DbxCredential loadCredentials(){
        try (final Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(userName, password))) {
            try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
                final Node node = session.run("MATCH (n:DROPBOX_CREDENTIALS {appKey: '" + appKey + "'}) RETURN n;").single().get("n").asNode();
                final long expiresAt = node.get("expiresAt").asLong();
                final String accessToken = node.get("accessToken").asString().trim();
                final String refreshToken = node.get("refreshToken").asString().trim();
                return new DbxCredential(accessToken, expiresAt < 0?null:expiresAt, refreshToken.isBlank()?null:refreshToken, appKey);
            } catch (final Throwable cause) {
                logger.error("Failed to load dropbox credentials", cause);
                return null;
            }
        }
    }

    public final String getAppKey(){
        return appKey;
    }

    public final void reauthorizeClient(final String code){
        final DbxAppInfo appInfo = new DbxAppInfo(appKey, appSecret);
        final DbxRequestConfig config = DbxRequestConfig.newBuilder(appKey).build();
        final DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
        try {
            final DbxAuthFinish authFinish = webAuth.finishFromCode(code, getRedirectUri());
            final String accessToken = authFinish.getAccessToken();
            final String refreshToken = authFinish.getRefreshToken();
            final Long expiresAt = authFinish.getExpiresAt();
            saveCredentials(accessToken, expiresAt, refreshToken);
            final DbxCredential credential = new DbxCredential(accessToken, expiresAt, refreshToken, appKey);
            final DbxClientV2 client = this._client = new DbxClientV2(config, credential);
            logger.info("Dropbox connection test: " + client.check().user("test").getResult());
        }catch (final Throwable cause){
            logger.error("Auth failed: ", cause);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to authenticate dropbox", cause);
        }
    }

    public final boolean isConnected(){
        try {
            return getClient().check().user("test").getResult().equals("test");
        }catch (final Throwable cause){
            return false;
        }
    }

    private final ReentrantLock get_client_lock = new ReentrantLock(true);
    private final DbxClientV2 getClient(){
        final DbxClientV2 res = this._client;
        if (res != null){
            return res;
        }
        get_client_lock.lock();
        try{
            if (_client != null){
                return _client;
            }
            final DbxRequestConfig config = DbxRequestConfig.newBuilder(appKey).build();
            final DbxCredential credential = loadCredentials();
            if (credential == null){
                logger.error("No dropbox credentials found.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dropbox auth failed.");
            }
            final DbxClientV2 client = this._client = new DbxClientV2(config, credential);
            logger.info("Dropbox connection test: " + client.check().user("test").getResult());
            return client;
        }catch (final Throwable cause){
            logger.error("Dropbox auth failed.", cause);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Dropbox auth failed.", cause);
        }finally {
            get_client_lock.unlock();
        }
    }

    public final List<DropBoxEntity> listDirectory(final String path){
        return _listDirectory(path, 0);
    }
    private final List<DropBoxEntity> _listDirectory(final String path, final int retry){
        final DbxClientV2 client = getClient();
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
            if (retry > 3){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list dropbox directory", cause);
            }
            try {
                client.refreshAccessToken();
                return _listDirectory(path, retry + 1);
            }catch (final Throwable refreshFailure){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", cause);
            }
        }
    }

    public final DropBoxEntity upload(final String path, final File file){
        return _upload(path, file, 0);
    }
    public final DropBoxEntity upload(final String path, final MultipartFile file){
        return _upload(path, file, 0);
    }
    private final DropBoxEntity _upload(final String path, final File file, final int retry){
        final DbxClientV2 client = getClient();
        try(final InputStream inputStream = new FileInputStream(file)){
            final FileMetadata metadata = client.files().uploadBuilder(path).uploadAndFinish(inputStream);
            return new DropBoxEntity(DropBoxEntity.EntityType.FILE, metadata.getPathDisplay());
        }catch (final Throwable cause){
            if (retry > 3){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to dropbox", cause);
            }
            try {
                client.refreshAccessToken();
                return _upload(path, file, retry + 1);
            }catch (final Throwable refreshFailure){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", cause);
            }
        }
    }
    private final DropBoxEntity _upload(final String path, final MultipartFile file, final int retry){
        final DbxClientV2 client = getClient();
        try(final InputStream inputStream = file.getInputStream()){
            final FileMetadata metadata = client.files().uploadBuilder(path).uploadAndFinish(inputStream);
            return new DropBoxEntity(DropBoxEntity.EntityType.FILE, metadata.getPathDisplay());
        }catch (final Throwable cause){
            if (retry > 3){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to dropbox", cause);
            }
            try {
                client.refreshAccessToken();
                return _upload(path, file, retry + 1);
            }catch (final Throwable refreshFailure){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", cause);
            }
        }
    }

    public final boolean doesFileExists(final String path){
        return _doesFileExists(path, 0);
    }
    private final boolean _doesFileExists(final String path, final int retry){
        final DbxClientV2 client = getClient();
        try {
            final Metadata metadata = client.files().getMetadata(path);
            return metadata instanceof FileMetadata;
        }catch (final Throwable cause){
            if (retry > 3){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check if file exists in dropbox", cause);
            }
            try {
                client.refreshAccessToken();
                return _doesFileExists(path, retry + 1);
            }catch (final Throwable refreshFailure){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", cause);
            }
        }
    }

    public final long getFileSize(final String path){
        return _getFileSize(path, 0);
    }
    private final long _getFileSize(final String path, final int retry){
        final DbxClientV2 client = getClient();
        try {
            final FileMetadata metadata = (FileMetadata) client.files().getMetadata(path);
            return metadata.getSize();
        }catch (final Throwable cause){
            if (retry > 3){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check file size in dropbox", cause);
            }
            try {
                client.refreshAccessToken();
                return _getFileSize(path, retry + 1);
            }catch (final Throwable refreshFailure){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", cause);
            }
        }
    }
    public final File getDropboxFile(final String path){
        return _getDropboxFile(path, 0);
    }
    private final File _getDropboxFile(final String path, final int retry){
        final DbxClientV2 client = getClient();
        if (!doesFileExists(path)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File '" + path + "' not found.");
        }
        try(final DbxDownloader<FileMetadata> downloader = client.files().download(path)){
            final File file = File.createTempFile("dropbox", "tmp");
            file.deleteOnExit();
            java.nio.file.Files.copy(
                    downloader.getInputStream(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return file;
        }catch (final Throwable cause){
            if (retry > 3){
                logger.error("Failed to download file '{}' from dropbox.", path, cause);
                throw new RuntimeException(cause);
            }
            try {
                client.refreshAccessToken();
                return _getDropboxFile(path, retry + 1);
            }catch (final Throwable refreshFailure){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh token", cause);
            }
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
