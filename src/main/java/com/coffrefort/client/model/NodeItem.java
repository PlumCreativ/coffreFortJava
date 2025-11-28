package com.coffrefort.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un dossier (noeud) contenant éventuellement des fichiers et des sous-dossiers.
 */
public class NodeItem {

    //propriétés
    public int id;
    private final String name;
    private final List<NodeItem> children = new ArrayList<>();

    // pour compatibilité avec l'ancien code (MainView)
    private final List<FileEntry> files = new ArrayList<>();


    //méthodes
    public NodeItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static NodeItem folder(int id, String name) {
        return new NodeItem(id, name);
    }

    public NodeItem addChild(NodeItem child) {
        this.children.add(child);
        return this;
    }

    public void setId(int id) { this.id = id; }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<NodeItem> getChildren() { return children; }


    @Override
    public String toString() {
        return name;
    }


    // === compatibilité avec le code d'exemple (MainView) ===
    public NodeItem withFiles(List<FileEntry> list) {
        this.files.clear();
        if (list != null) {
            this.files.addAll(list);
        }
        return this;
    }

    public List<FileEntry> getFiles() {
        return files;
    }

}
