package com.example.calculator;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<FileItem> fileItemList;
    private final OnItemClickListener onItemClickListener;
    private final OnItemLongClickListener onItemLongClickListener;

    // Constructor with listeners
    public FileAdapter(List<FileItem> fileItemList, OnItemClickListener onItemClickListener, OnItemLongClickListener onItemLongClickListener) {
        this.fileItemList = fileItemList;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
        Log.d("FileAdapter", "Initialized with " + fileItemList.size() + " files");
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileItemList.get(position);

        // Set file name and size safely
        holder.fileName.setText(fileItem.getFileName() != null ? fileItem.getFileName() : "Unknown");
        holder.fileSize.setText(fileItem.getFileSize() != null ? fileItem.getFileSize() : "0 KB");

        // Handle file type
        String fileType = fileItem.getFileType() != null ? fileItem.getFileType() : "unknown";
        holder.fileType.setText(fileType);

        // Set thumbnail using Glide for better performance
        Uri thumbnailUri = fileItem.getThumbnailUri();
        if (thumbnailUri != null) {
            Glide.with(holder.thumbnail.getContext())
                    .load(thumbnailUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_file) // Default icon while loading
                    .into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_file); // Default icon if no thumbnail
        }

        Log.d("FileAdapter", "Binding file: " + fileItem.getFileName() + " (Type: " + fileType + ")");

        // Set click listener for normal click
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(fileItem);
            }
        });

        // Set long-click listener for item long press
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(fileItem);
            }
            return true; // Returning true to indicate long click is handled
        });
    }

    @Override
    public int getItemCount() {
        return fileItemList != null ? fileItemList.size() : 0;
    }

    // Interface for item click
    public interface OnItemClickListener {
        void onItemClick(FileItem fileItem);
    }

    // Interface for item long click
    public interface OnItemLongClickListener {
        void onItemLongClick(FileItem fileItem);
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView fileName, fileType, fileSize;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.fileThumbnail);
            fileName = itemView.findViewById(R.id.fileName);
            fileType = itemView.findViewById(R.id.fileType);
            fileSize = itemView.findViewById(R.id.fileSize);
        }
    }
}
