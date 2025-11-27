package com.coffrefort.client;

import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client d'API minimal (simulation) pour permettre l'exécution sans backend.
 * Remplacez progressivement les méthodes par de vrais appels HTTP vers votre API REST.
 */
public class ApiClient {
    private String baseUrl = "http://localhost:8080"; // à adapter
    private String authToken; // JWT ou autre

    public boolean login(String email, String password) {
        // Simulation: accepte tout couple non-vide
        if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
            this.authToken = "demo-" + UUID.randomUUID();
            return true;
        }
        return false;
    }

    public boolean isAuthenticated() {
        return authToken != null;
    }

    /**
     * Retourne une arborescence factice de dossiers/fichiers.
     */
    public List<NodeItem> listRoot() {
        List<NodeItem> root = new ArrayList<>();
        NodeItem docs = NodeItem.folder("Documents")
                .withFiles(List.of(
                        FileEntry.of("CV.pdf", 128_000, Instant.now().minusSeconds(86_400)),
                        FileEntry.of("Lettre_motivation.docx", 64_000, Instant.now().minusSeconds(123_000))
                ));
        NodeItem photos = NodeItem.folder("Photos")
                .addChild(NodeItem.folder("Vacances 2024").withFiles(List.of(
                        FileEntry.of("plage.jpg", 2_048_000, Instant.now().minusSeconds(55_000)),
                        FileEntry.of("coucher_soleil.jpg", 1_648_000, Instant.now().minusSeconds(45_000))
                )))
                .addChild(NodeItem.folder("Famille"));

        NodeItem racineFichiers = NodeItem.folder("Racine");
        racineFichiers.getFiles().add(FileEntry.of("todo.txt", 1_024, Instant.now().minusSeconds(3_600)));

        root.add(docs);
        root.add(photos);
        root.add(racineFichiers);
        return root;
    }

    /**
     * Quota simulé: 2 Go max, 350 Mo utilisés.
     */
    public Quota getQuota() {
        return new Quota(350L * 1024 * 1024, 2L * 1024 * 1024 * 1024);
    }
}
