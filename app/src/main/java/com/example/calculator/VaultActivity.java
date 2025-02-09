package com.example.calculator;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class VaultActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<FileItem> fileItemList;
    private static final int PICK_FILE_REQUEST = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        recyclerView = findViewById(R.id.recyclerViewVault);
        FloatingActionButton fabAddFile = findViewById(R.id.fabAddFile);

        // Initialize the list of files
        fileItemList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileItemList, new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FileItem fileItem) {
                File vaultDir = new File(getFilesDir(), "vault");
                File encryptedFile = fileItem.getFile();

                if (encryptedFile != null && encryptedFile.exists()) {
                    try {
                        File metadataFile = new File(vaultDir, fileItem.getFile().getName() + ".meta");

                        if (!metadataFile.exists()) {
                            Toast.makeText(VaultActivity.this, "Metadata file not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<String> metadataLines = Files.readAllLines(metadataFile.toPath());

                        if (metadataLines.size() > 3) {
                            String base64SecretKey = metadataLines.get(3).trim();
                            byte[] keyBytes = Base64.decode(base64SecretKey, Base64.DEFAULT);
                            SecretKey secretKey = new SecretKeySpec(keyBytes, EncryptionHelper.ALGORITHM);

                            File decryptedFile = new File(getCacheDir(), "decrypted_" + System.currentTimeMillis());
                            EncryptionHelper.decryptFile(encryptedFile, decryptedFile, secretKey);
                            openFile(decryptedFile);
                            new Handler().postDelayed(() -> decryptedFile.delete(), 60000);

                        } else {
                            Toast.makeText(VaultActivity.this, "Metadata file is corrupted", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(VaultActivity.this, "Decryption failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(VaultActivity.this, "File doesn't exist", Toast.LENGTH_SHORT).show();
                }
            }
        }, new FileAdapter.OnItemLongClickListener() {  // Correct method signature for long-click listener
            @Override
            public void onItemLongClick(FileItem fileItem) {
                showFileOptionsDialog(fileItem);  // Long click handling
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fileAdapter);

        // Load actual saved files
        loadVaultFiles();  // ⬅️ This will load real encrypted files

        // FAB action to add a file
        fabAddFile.setOnClickListener(v -> openFilePicker());
    }

    private void openFile(FileItem fileItem) {
        File vaultDir = new File(getFilesDir(), "vault");
        File encryptedFile = new File(vaultDir, fileItem.getFileName());

        if (encryptedFile.exists()) {
            File decryptedFile = new File(getCacheDir(), "decrypted_" + System.currentTimeMillis());

            try {
                // Retrieve the stored secret key
                SecretKey secretKey = EncryptionHelper.generateKey(); // Load actual stored key
                EncryptionHelper.decryptFile(encryptedFile, decryptedFile, secretKey);

                // Open the decrypted file
                openFile(decryptedFile);

                // Schedule deletion after viewing (optional)
                new Handler().postDelayed(() -> decryptedFile.delete(), 60000); // Delete after 1 min

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Decryption failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, "com.example.calculator.fileprovider", file); // EXACTLY the same as in manifest
            Intent intent = new Intent(Intent.ACTION_VIEW);

            String mimeType = getMimeType(file); // Get MIME type (see helper function below)
            intent.setDataAndType(fileUri, mimeType);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // VERY IMPORTANT
            startActivity(Intent.createChooser(intent, "Open with"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show(); // Handle errors
        }
    }

    // Helper function to get MIME type (Important!)
    private String getMimeType(File file) {
        String type = null;
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(file.getName());
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if(type == null){
                type = "*/*";
            }
        }else{
            type = "*/*";
        }
        return type;
    }

    private void showFileOptionsDialog(FileItem fileItem) {
        new AlertDialog.Builder(this)
                .setTitle("Manage File")
                .setItems(new String[]{"Delete", "Rename", "Unhide"}, (dialog, which) -> {
                    if (which == 0) {
                        deleteFileFromVault(fileItem);
                    } else if (which == 1) {
                        renameFile(fileItem);
                    } else if (which == 2) {
                        unhideFile(fileItem);
                    }
                })
                .show();
    }


    private void deleteFileFromVault(FileItem fileItem) {
        File vaultDir = new File(getFilesDir(), "vault");
        File fileToDelete = fileItem.getFile();
        File metaFileToDelete = new File(fileItem.getFile() + ".meta");

        Log.d("Delete File", "Attempting to delete: " + fileToDelete.getAbsolutePath());
        Log.d("Delete File", "Can write: " + fileToDelete.canWrite());
        Log.d("Delete File", "Is directory: " + fileToDelete.isDirectory());
        Log.d("Delete File", "Is file: " + fileToDelete.isFile());
        Log.d("Delete File", "File exists: " + fileToDelete.exists());


        if ((fileToDelete.exists() && metaFileToDelete.exists()) && (fileToDelete.delete() && metaFileToDelete.delete())) {
            fileItemList.remove(fileItem);
            fileAdapter.notifyDataSetChanged();
            Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }

    private void renameFile(FileItem fileItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename File");

        // Create input field with the current file name
        EditText input = new EditText(this);
        input.setText(fileItem.getFileName());  // Display current original name
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newFileName = input.getText().toString().trim();  // Get new file name

            if (newFileName.isEmpty()) {
                Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            File vaultDir = new File(getFilesDir(), "vault");

            Log.d("Rename", "name is: " + fileItem.getFile().getName());
            // Extract encrypted filename from full path
            String encryptedFileName = new File(String.valueOf(fileItem.getFile())).getName();

            // Locate the metadata file
            File metadataFile = new File(vaultDir, encryptedFileName + ".meta");

            Log.d("RenameDebug", "Looking for metadata file: " + metadataFile.getAbsolutePath());

            if (!metadataFile.exists()) {
                Toast.makeText(this, "Metadata file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Read the metadata file
                List<String> metadataLines = new ArrayList<>(Files.readAllLines(metadataFile.toPath()));

                if (!metadataLines.isEmpty()) {
                    metadataLines.set(0, newFileName); // Update the first line with the new name
                }

                // Write updated metadata back to the file
                Files.write(metadataFile.toPath(), metadataLines);

                // Update FileItem with the new display name
                fileItem.setFileName(newFileName);

                // Refresh UI
                fileAdapter.notifyDataSetChanged();

                Toast.makeText(this, "File renamed successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e("RenameError", "Error updating metadata", e);
                Toast.makeText(this, "Error updating metadata", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void unhideFile(FileItem fileItem) {
        File vaultDir = new File(getFilesDir(), "vault");
        File encryptedFile = new File(vaultDir, fileItem.getFile().getName());
        Log.d("unhidelogs", "encrypted file found: " + fileItem.getFile().getName());

        if (encryptedFile.exists()) {
            try {
                // Retrieve the stored secret key from the 4th line of the metadata file
                File metadataFile = new File(vaultDir, fileItem.getFile().getName() + ".meta");

                Log.d("unhidelogs", "shitA: metadata file found");

                if (!metadataFile.exists()) {
                    Toast.makeText(this, "Metadata file not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Read the metadata file
                List<String> metadataLines = Files.readAllLines(metadataFile.toPath());
                Log.d("unhidelogs", "size: " + metadataLines.size());

                if (metadataLines.size() > 3) { // Ensure there are at least 4 lines
                    String base64SecretKey = metadataLines.get(3).trim(); // 4th line contains the secret key (Base64 encoded)
                    Log.d("unhidelogs", "Base64: " + base64SecretKey);
                    // Decode the Base64 string to get the byte array
                    byte[] keyBytes = Base64.decode(base64SecretKey, Base64.DEFAULT);

                    // Generate SecretKey using the decoded bytes
                    SecretKey secretKey = new SecretKeySpec(keyBytes, EncryptionHelper.ALGORITHM);
                    Log.d("unhidelogs", "Unhide time secret key: " + secretKey);

                    // SAF: Create a new file in the Downloads folder
                    File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileItem.getFileName());
                    if (!downloadsDir.exists()) {
                        downloadsDir.createNewFile();
                    }

                    // Decrypt the file using SAF-created file
                    EncryptionHelper.decryptFile(encryptedFile, downloadsDir, secretKey);

                    // Remove the encrypted file from the vault
                    if (encryptedFile.delete()) {
                        File metadataFileToDelete = new File(vaultDir, fileItem.getFile().getName() + ".meta");
                        if (metadataFileToDelete.exists()) {
                            metadataFileToDelete.delete();
                        }
                        // Remove from file list and notify adapter
                        fileItemList.remove(fileItem);
                        fileAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "File unhidden successfully", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Metadata file is corrupted or incomplete", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to unhide file", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void loadVaultFiles() {
        File vaultDir = new File(getFilesDir(), "vault");
        if (!vaultDir.exists()) {
            vaultDir.mkdir();
        }

        File[] files = vaultDir.listFiles();
        fileItemList.clear(); // Clear the list before reloading

        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".meta")) {
                    continue;  // Skip metadata files
                }

                // Read metadata for the encrypted file
                File metadataFile = new File(vaultDir, file.getName() + ".meta");
                String originalFileName = file.getName();
                String fileType = "Unknown Type";
                String fileSize = "Unknown Size";

                if (metadataFile.exists()) {
                    try {
                        byte[] buffer = new byte[(int) metadataFile.length()];
                        FileInputStream fis = new FileInputStream(metadataFile);
                        fis.read(buffer);
                        fis.close();

                        // Extract file name, type, and size from metadata
                        String metadata = new String(buffer);
                        String[] metadataParts = metadata.split("\n");
                        if (metadataParts.length >= 3) {
                            originalFileName = metadataParts[0];  // Original file name
                            fileType = metadataParts[1];  // File type
                            fileSize = metadataParts[2];  // File size
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Add the file item to the list, including the actual file reference
                fileItemList.add(new FileItem(originalFileName, fileType, fileSize, file));
            }
        }

        runOnUiThread(() -> {
            System.out.println("Notifying adapter. Total files: " + fileItemList.size());
            fileAdapter.notifyDataSetChanged();  // Update UI
        });
    }



    private String getOriginalFileName(File encryptedFile) {
        File metadataFile = new File(encryptedFile.getParent(), encryptedFile.getName() + ".meta");
        if (metadataFile.exists()) {
            try {
                byte[] buffer = new byte[(int) metadataFile.length()];
                FileInputStream fis = new FileInputStream(metadataFile);
                fis.read(buffer);
                fis.close();
                return new String(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return encryptedFile.getName(); // If no metadata, return encrypted name
    }


    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allows all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                saveFileToVault(fileUri);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    private void saveFileToVault(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            File vaultDir = new File(getFilesDir(), "vault");
            if (!vaultDir.exists()) {
                vaultDir.mkdir();
            }

            // Retrieve file metadata: name, type, size
            String originalFileName = getFileName(fileUri);
            String mimeType = getContentResolver().getType(fileUri);  // Get file type
            String fileSize = getFileSize(fileUri);  // Get file size

            if (mimeType == null) {
                mimeType = "*/*"; // Default if type is unknown
            }

            String encryptedFileName = "encrypted_" + System.currentTimeMillis();
            File encryptedFile = new File(vaultDir, encryptedFileName);
            SecretKey secretKey = EncryptionHelper.generateKey();

            String base64EncodedKey = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
            Log.d("unhidelogs", "Savetime Secret key: " + base64EncodedKey);

            // Save metadata: original file name, type, size, and MIME type
            File metadataFile = new File(vaultDir, encryptedFileName + ".meta");
            try (FileOutputStream fos = new FileOutputStream(metadataFile)) {
                String metadata = originalFileName + "\n" + mimeType + "\n" + fileSize + "\n" + base64EncodedKey; // Include MIME type
                fos.write(metadata.getBytes());
                fos.close();
                Log.d("Metadata", "Metadata written: " + metadata); // Log the metadata
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Temporarily store the file before encryption
            File tempFile = new File(getCacheDir(), "tempFile");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) { // Use try-with-resources
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            } // Resources are automatically closed
            inputStream.close(); // Close the input stream


            // Encrypt and save
            EncryptionHelper.encryptFile(tempFile, encryptedFile, secretKey);
            tempFile.delete();

            // Debugging: Log success
            System.out.println("File encrypted and saved as: " + encryptedFile.getAbsolutePath());

            runOnUiThread(() -> {
                loadVaultFiles(); // Refresh UI after saving the file
                Toast.makeText(this, "File encrypted and saved!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
            System.out.println("Encryption failed: " + e.getMessage());
        }
    }
    private String getFileSize(Uri uri) {
        long size = 0;
        int rep = 0;
        String[] sizes = {"b", "kb", "mb", "gb", "tb", "pb"};
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex);
                        while(size > 1000){
                            size = size / 1000;
                            rep++;
                        }
                    }
                }
            }
        }
        String final_Size = size + sizes[rep];
        return final_Size;
    }

}
