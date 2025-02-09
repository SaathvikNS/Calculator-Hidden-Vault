package com.example.calculator;

import android.net.Uri;

import java.io.File;

public class FileItem {
    private String fileName;
    private File file;
    private String fileType;
    private String fileSize;
    private Uri thumbnailUri;

    public FileItem(String fileName, String fileType, String fileSize, File file) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.file = file;
        this.thumbnailUri = thumbnailUri;
    }

    public String getFileName() { return fileName; }
    public File getFile() {return file; }
    public String getFileType() { return fileType; }
    public String getFileSize() { return fileSize; }
    public Uri getThumbnailUri() { return thumbnailUri; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}