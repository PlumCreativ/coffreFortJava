package com.coffrefort.client.model;

import java.time.Instant;

public class FileEntry {

    //propriétés
    private final int id; //=> pour pouvoir supprimer, renommer, télécharger
    private final String name;
    private final long size;
    private final String  createdAt;
    private final String updatedAt;

    //méthodes
    private FileEntry(int id, String name, long size, String  createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FileEntry of(int id, String name, long size, String  createdAt, String updatedAt) {
        return new FileEntry(id, name, size, createdAt, updatedAt);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public String getCreatedAt() { return createdAt; }

    // === compatibilité avec MainView (ancien code) ===
    // MainView utilise getUpdatedAt(), donc on lui donne ce qu'il veut :
    public String getUpdatedAt() {
        return createdAt;
    }

    //pour la TableView => colonne taille

    /**
     * Formate une taille en octets en unité lisible (B, KB, MB, GB, …) avec une décimale
     * @return
     */
    public String getFormattedSize() {
        long bytes = size;
        if (bytes < 1024) return bytes + " B";                //=> size "bytes"
        int exp = (int) (Math.log(bytes) / Math.log(1024));  // => exposant
        char unit = "KMGTPE".charAt(exp - 1);               //p.ex exp=1 -> "K"
        double val = bytes / Math.pow(1024, exp);            // Math.pow => val = 2048 / 1024 = 2.0 KB
        return String.format("%.1f %sB", val, unit);
    }

    //Pour la TableView => colonne date
    public String getUpdatedAtFormatted() {
        return updatedAt != null ? updatedAt : "";
    }
}
