package com.example.calculator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

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
                openFile(fileItem);
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
        Uri fileUri = FileProvider.getUriForFile(this, "com.yourapp.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")) {
            intent.setDataAndType(fileUri, "image/*");
        } else if (file.getName().endsWith(".mp4")) {
            intent.setDataAndType(fileUri, "video/*");
        } else if (file.getName().endsWith(".pdf")) {
            intent.setDataAndType(fileUri, "application/pdf");
        } else {
            intent.setDataAndType(fileUri, "*/*");
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open with"));
    }

    private void showFileOptionsDialog(FileItem fileItem) {
        new AlertDialog.Builder(this)
                .setTitle("Manage File")
                .setItems(new String[]{"Delete", "Rename"}, (dialog, which) -> {
                    if (which == 0) {
                        deleteFileFromVault(fileItem);
                    } else {
                        renameFile(fileItem);
                    }
                })
                .show();
    }

    private void deleteFileFromVault(FileItem fileItem) {
        File vaultDir = new File(getFilesDir(), "vault");
        File fileToDelete = new File(vaultDir, fileItem.getFileName());

        if (fileToDelete.exists() && fileToDelete.delete()) {
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

        EditText input = new EditText(this);
        input.setText(fileItem.getFileName());
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            File vaultDir = new File(getFilesDir(), "vault");
            File oldFile = new File(vaultDir, fileItem.getFileName());
            File newFile = new File(vaultDir, input.getText().toString());

            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                fileItem.setFileName(newFile.getName());
                fileAdapter.notifyDataSetChanged();
                Toast.makeText(this, "File renamed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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
                fileItemList.add(new FileItem(file.getName(), "Encrypted File", "Unknown Size"));
                System.out.println("Vault File Found: " + file.getName()); // Debugging log
            }
        }

        runOnUiThread(() -> {
            System.out.println("Notifying adapter. Total files: " + fileItemList.size());
            fileAdapter.notifyDataSetChanged();  // Update UI
        });
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

    private void saveFileToVault(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            File vaultDir = new File(getFilesDir(), "vault");
            if (!vaultDir.exists()) {
                vaultDir.mkdir();
            }

            File encryptedFile = new File(vaultDir, "encrypted_" + System.currentTimeMillis());

            SecretKey secretKey = EncryptionHelper.generateKey();

            // Temporarily store the file before encryption
            File tempFile = new File(getCacheDir(), "tempFile");
            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();

            // Encrypt and save
            EncryptionHelper.encryptFile(tempFile, encryptedFile, secretKey);
            tempFile.delete();

            // Debugging: Log success
            System.out.println("File encrypted and saved as: " + encryptedFile.getAbsolutePath());

            runOnUiThread(() -> {
                loadVaultFiles(); // ⬅️ Refresh UI after saving the file
                Toast.makeText(this, "File encrypted and saved!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
            System.out.println("Encryption failed: " + e.getMessage());
        }
    }
}
