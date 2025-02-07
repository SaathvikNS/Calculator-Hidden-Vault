package com.example.calculator;

import android.net.Uri;

public class FileItem {
    private String fileName;
    private String fileType;
    private String fileSize;
    private Uri thumbnailUri;

    public FileItem(String fileName, String fileType, String fileSize) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.thumbnailUri = thumbnailUri;
    }

    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getFileSize() { return fileSize; }
    public Uri getThumbnailUri() { return thumbnailUri; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}