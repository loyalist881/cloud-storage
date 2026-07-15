package com.github.loyalist.storageservice.files.exception;

public class FileNotFoundStorageException extends RuntimeException {
    public FileNotFoundStorageException(String message) {
        super(message);
    }
}
