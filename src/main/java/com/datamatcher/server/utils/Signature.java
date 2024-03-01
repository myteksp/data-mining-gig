package com.datamatcher.server.utils;

import jakarta.xml.bind.DatatypeConverter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;

public final class Signature {

    public static final String getSignature(final Object object)  {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }catch (final Throwable cause){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate hash", cause);
        }
        md.update(JSON.toJson(object).getBytes());
        return DatatypeConverter.printHexBinary(md.digest());
    }
}
