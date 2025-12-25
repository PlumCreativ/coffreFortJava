package com.coffrefort.client;

import com.coffrefort.client.config.AppProperties;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;
import com.coffrefort.client.model.ShareItem;
import com.coffrefort.client.util.JsonUtils;
import com.coffrefort.client.util.JwtUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ApiClient {

    //propriétés
    private static ApiClient INSTANCE;
    private final HttpClient httpClient;
    private final String baseUrl;
    private String authToken;
    private final HttpClient http = HttpClient.newHttpClient();


    //méthodes
    // Constructeur par défaut avec URL localhost
    public ApiClient() {
        this("http://localhost:9081");
    }

    // Constructeur avec URL personnalisée
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.authToken = null;
    }

    /**
     * Authentification utilisateur avec email et mot de passe
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return Le token JWT si succès, null sinon
     * @throws Exception En cas d'erreur réseau ou serveur
     */
    public String login(String email, String password) throws Exception {
        String url = baseUrl + "/auth/login";



        // Construction du body JSON
        String jsonBody = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                email, password
        );

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Envoi de la requête
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        String responseBody = response.body();

        System.out.println("Login - Status: " + statusCode);
        System.out.println("Login - Response: " + responseBody);

        // Gestion des erreurs HTTP
        if (statusCode == 401) {
            throw new AuthenticationException("Identifiants invalides.");
        } else if (statusCode == 400) {
            String errorMsg = JsonUtils.extractJsonField(responseBody, "error");
            throw new AuthenticationException(
                    errorMsg != null ? errorMsg : "Requête invalide."
            );
        } else if (statusCode != 200) {
            String errorMsg = JsonUtils.extractJsonField(responseBody, "error");
            throw new Exception(
                    errorMsg != null ? errorMsg : "Erreur serveur (code " + statusCode + ")."
            );
        }

        // Extraction du token JWT
        String token = JsonUtils.extractJsonField(responseBody, "jwt");
        if (token == null || token.isEmpty()) {
            throw new Exception("Token JWT non reçu du serveur.");
        }

        setAuthToken(token);

        return token;
    }

    /**
     * Inscription d'un nouvel utilisateur puis connexion automatique
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @param quotaTotal
     * //@param isAdmin => backend qui décide
     * @return Le token JWT si succès
     * @throws Exception En cas d'erreur
     */
    public String register(String email, String password, int quotaTotal) throws Exception{
        // auth/register
        String registerUrl = baseUrl + "/auth/register";

        String registerJson = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"quota_total\":\"%d\"}",
                email, password, quotaTotal
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(registerUrl))
                .header("Accept", "application/json")
                .header ("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(registerJson))
                .build();

        HttpResponse<String> registerResponse = http.send(request, HttpResponse.BodyHandlers.ofString());

        int regStatus = registerResponse.statusCode();
        String regBody = registerResponse.body();
        System.out.println("Register Status: " + regStatus);
        System.out.println("Register Response: " + regBody);

        // Pour une inscription, l'API peut renvoyer 200 ou 201 (Created)
        if(regStatus < 200 || regStatus >= 300) {

            //Erreur d'inscritption
            String apiError = JsonUtils.extractJsonField(regBody, "error");

            if(apiError == null || apiError.isEmpty()) {
                apiError = "Inscription refusée par le serveur (code " + regStatus + ").";
            }
            throw new RegistrationException(apiError); //=> il ne faut pas return après!!
        }

        // /auth/login
        String loginUrl = baseUrl + "/auth/login";

        String LoginJson = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                email, password
        );

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("Accept", "application/json")
                .header ("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(LoginJson))
                .build();

        HttpResponse<String> loginResponse = http.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        int logStatus = loginResponse.statusCode();
        String logBody = loginResponse.body();
        System.out.println("Login Status: " + logStatus);
        System.out.println("Login Response: " + logBody);

        if(logStatus != 200){

            //Erreur de connexion
            String apiError = JsonUtils.extractJsonField(logBody, "error");
            if(apiError == null || apiError.isEmpty()) {
                apiError = "Connexion automatique échouée (code " + logStatus + ").";
            }
            throw new RegistrationException(apiError);
        }

        //récupération de token
        String token =  JsonUtils.extractJsonField(logBody, "jwt");
        if(token == null || token.isEmpty()) {
            throw new RegistrationException("Connexion réussi mais aucun token renvoyé par le serveur.");
        }

        setAuthToken(token);

        return token;
    }

    /**
     *
     * @return
     */
    public static ApiClient getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ApiClient();
        }
        return INSTANCE;
    }

    /**
     * Définir manuellement le token (pour restauration depuis persistance)
     * stocker authToken en mémoire
     */
    public void setAuthToken(String token) {
        this.authToken = token;
        if (token != null) {
            AppProperties.set("auth.token", token);

            String email = JwtUtils.extractEmail(token);
            if (email != null) {
                AppProperties.set("auth.email", email);
            }

            String userId = JwtUtils.extractUserID(token);
            if (userId != null) {
                AppProperties.set("auth.userId", userId);
            }
            System.out.println("setAuthToken() userId = " + userId);
        }
    }

    /**
     * Créer un dossier
     * @param name
     * @param parentFolder
     * @return
     * @throws Exception
     */
    public boolean createFolder(String name, NodeItem parentFolder) throws Exception{
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (authToken null).");
        }

        String userIdStr = AppProperties.get("auth.userId");
        System.out.println("createFolder() userIdStr = " + userIdStr);

        if(userIdStr == null || userIdStr.isEmpty()) {
            throw new IllegalStateException("auth.userId non défini dans AppProperties.");
        }

        int userId = Integer.parseInt(userIdStr);

        Integer parentId = null; // => null => dossier à la racine
        if(parentFolder != null && parentFolder.getId() != 0) {
            parentId = parentFolder.getId();
        }

        //construction de json
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"user_id\": ").append(userId).append(",");

        if(parentId == null) {
            sb.append("\"parent_id\": null,");
        }else{
            sb.append("\"parent_id\": ").append(parentId).append(",");
        }

        sb.append("\"name\": \"").append(escapeJson(name)).append("\"");
        sb.append("}");
//        String jsonBody = "{"
//                + "\"user_id\": " + userId + ","
//                + "\"parent_id\": " + parentId + ","
//                + "\"name\": \"" + escapeJson(name) + "\""
//                + "}";

        String jsonBody = sb.toString();
        System.out.println("POST /folders body = " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/folders"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status == 201) {
            System.out.println("Dossier créé: " + response.body());
            return true;
        }

        System.err.println("Erreur création dossier. Status=" + status + " body=" + response.body());
        return false;
    }


    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    /**
     * uploader un fichier dans le dossier racine par défaut
     * @param file
     * @return
     * @throws Exception
     */
    public boolean uploadFile(File file) throws Exception{
        //s'il n'y a pas dossier => passer en null
        return uploadFile(file, null);
    }



    /**
     * * uploader un ou des fichier(s)
     * @param file fichier local
     * @param folderId  id du dossier cible (peut être null pour racine si ton backend le gère)
     * @return
     * @throws Exception
     */
    public boolean uploadFile(File file, Integer folderId) throws Exception {
        if (file == null || !file.exists()) {
            throw new Exception("Fichier invalide");
        }

        // Vérifier token
        String token = this.authToken;
        if (token == null || token.isEmpty()) {
            token = AppProperties.get("auth.token"); // fallback
        }
        if (token == null || token.isEmpty()) {
            throw new AuthenticationException("Utilisateur non connecté (token manquant).");
        }

        String url = baseUrl + "/files";
        String boundary = "----CryptoVaultBoundary" + UUID.randomUUID();

        // Construire le body multipart
        byte[] body = buildMultipartBody(file, boundary, folderId);

        // Faire la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String responseBody = response.body();

        System.out.println("UPLOAD Status: " + status);
        System.out.println("UPLOAD Response: " + responseBody);

        if (status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }

        if (status < 200 || status >= 300) {
            String apiError = JsonUtils.extractJsonField(responseBody, "error");
            if (apiError == null || apiError.isEmpty()) {
                apiError = "Upload refusé (code " + status + ")";
            }
            throw new Exception(apiError);
        }

        return true;
    }

     /**
     * @param file
     * @param boundary
     * @return
     * @throws Exception
     */
    private byte[] buildMultipartBody(File file, String boundary, Integer folderId) throws Exception {
        String CRLF = "\r\n";

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream"; // fallback
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // ---- Part "folder_id" (si fourni)
        if (folderId != null) {
            String folderPart =
                    "--" + boundary + CRLF +
                            "Content-Disposition: form-data; name=\"folder_id\"" + CRLF + CRLF +
                            folderId + CRLF;

            output.write(folderPart.getBytes(StandardCharsets.UTF_8));
        }

        // ---- Part "file"
        String filePartHeader =
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + CRLF +
                        "Content-Type: " + mimeType + CRLF + CRLF;

        output.write(filePartHeader.getBytes(StandardCharsets.UTF_8));

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        output.write(fileBytes);

        // Fin de la part fichier
        output.write(CRLF.getBytes(StandardCharsets.UTF_8));

        // ---- Fin multipart
        String ending = "--" + boundary + "--" + CRLF;
        output.write(ending.getBytes(StandardCharsets.UTF_8));

        return output.toByteArray();
    }


    /**
     * Récupération de tous les folders d'un utilisateur
     * @return
     * @throws Exception
     */
    public NodeItem listRoot() throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/folders"))
                //.header("Content-Type", "application/json") //=> lehet hogy le kell venni
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status != 200) {
            System.err.println("Erreur listRoot. Status=" + status + " body=" + response.body());
            throw new IllegalStateException("Erreur HTTP " + status + " lors du chargement de l'arborescence");
        }

        String body = response.body();
        System.out.println("GET /folders => " + body);

        // Construire l'arbre de NodeItem
        return buildFolderTreeFromJson(body);
    }

    /**
     * Récupération des fichier d'un folder précis lié à une client
     * @param folderId
     * @return
     * @throws Exception
     */
    public List<FileEntry> listFiles(int folderId) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/files?folder=" + folderId))
                //.header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status != 200) {
            System.err.println("Erreur listFiles. Status=" + status + " body=" + response.body());
            throw new IllegalStateException("Erreur HTTP " + status + " lors du chargement des fichiers");
        }

        String body = response.body();
        System.out.println("GET /files?folder=" + folderId + " => " + body);

        // Construire l'arbre de NodeItem
        return parseFiles(body);
    }




    /**
     * Déconnexion - Supprime le token
     */
    public void logout() {
        this.authToken = null;
        AppProperties.remove("auth.token");
        AppProperties.remove("auth.email");
        System.out.println("Déconnexion effectuée.");
    }

    /**
     * Vérifie si l'utilisateur est authentifié
     */
    public boolean isAuthenticated() {
        return this.authToken != null && !this.authToken.isEmpty();
    }

    /**
     * Retourne le token d'authentification actuel
     */
    public String getAuthToken() {
        return this.authToken;
    }

    /**
     * récupération quota depuis d'une requête API
     */
    public Quota getQuota() throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/me/quota"))
                //.header("Content-Type", "application/json") //=> lehet hogy le kell venni
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("GET /me/quota status=" + status);
        System.out.println("GET /me/quota body=" + body);

        if(status != 200){
            String apiError = JsonUtils.extractJsonField(body, "error");

            if(apiError == null || apiError.isEmpty()){
                apiError = "Erreur quota (code " + status + ")";
            }
            throw  new Exception(apiError);
        }

        String usedStr = JsonUtils.extractJsonNumberField(body, "used_bytes");
        String totalStr = JsonUtils.extractJsonNumberField(body, "total_bytes");


        long used = (usedStr != null && !usedStr.isEmpty()) ? Long.parseLong(usedStr) : 0;
        long total = (totalStr != null && !totalStr.isEmpty()) ? Long.parseLong(totalStr) : 0;

        return new Quota(used, total);
    }

    /**
     * Exception personnalisée pour les erreurs d'authentification
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * Exception pour les erreurs d'inscription
     */
    public static class RegistrationException extends Exception {
        public RegistrationException(String message) {
            super(message);
        }
    }

    public Boolean deleteFile(int id) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/files/" + id))
                //.header("Content-Type", "application/json") //=> lehet hogy le kell venni
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        //204 => requête réussi, pas besoin de quitter la page
        if(status == 200 || status == 204) {
            return true;
        }

        if(status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }
        System.out.println("Erreur pendant la suppression du fichier. Status=" + status + " body=" + response.body());
        return false;

    }


    /**
     * supprimer un dossier choisi
     * @param id
     * @return
     * @throws Exception
     */
    public Boolean deleteFolder(int id) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/folders/" + id))
                //.header("Content-Type", "application/json") //=> lehet hogy le kell venni
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("DELETE /folders/" + id + " => " + status + " body: " + body);

        //204 => requête réussi, pas besoin de quitter la page
        if(status == 200 || status == 204) {
            return true;
        }

        if(status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }
        System.out.println("Erreur pendant la suppression du dossier. Status=" + status + " body=" + response.body());
        return false;

    }

    /**
     * télécharger un file
     * @param fileId
     * @param target
     * @throws Exception
     */
    public void downloadFileTo(long fileId, File target) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/files/" + fileId + "/download"))
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if(response.statusCode() != 200) {

            //lire le message d'erreur du backend
            String errorMessage = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " +response.statusCode() + " lors du téléchargement: " + errorMessage);
        }

        //écriture le flux dans le fichier
        try (InputStream in = response.body()) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

    }


    //méthodes private

    //DTO =Data Transfer Object => pour structurer les données pour les rendre faciles à échanger
    private static class FolderDto{
        int id;
        String name;
        Integer parentId;
    }


    /**
     * construction d'un NodeItem "racine" virtuel avec tous les dossiers enfants
     * @param json
     * @return
     */
    private NodeItem buildFolderTreeFromJson(String json) {
        List<FolderDto> folders = parseFolders(json);

        // Racine virtuelle (id 0) non affichée parce que TreeView.showRoot = false
        NodeItem root = NodeItem.folder(0, "Racine");
        java.util.Map<Integer, NodeItem> map = new java.util.HashMap<>();

        // Créer tous les noeuds
        for (FolderDto f : folders) {
            NodeItem node = NodeItem.folder(f.id, f.name);
            map.put(f.id, node);
        }

        // Assembler l'arborescence selon parent_id
        for (FolderDto f : folders) {
            NodeItem node = map.get(f.id);

            if (f.parentId == null || f.parentId == 0) {

                // dossier racine
                root.addChild(node);
            } else {
                NodeItem parent = map.get(f.parentId);
                if (parent != null) {
                    parent.addChild(node);
                } else {

                    // parent non trouvé → par sécurité à accrocher à la racine
                    root.addChild(node);
                }
            }
        }
        return root;
    }


    /**
     * Parse un JSON de type:
     *   [ { "id":1, "name":"Docs", "parent_id":null }, ... ]
     * ou un seul objet:
     *   { "id":1, "name":"Docs", "parent_id":null, ... }
     */
    private List<FolderDto> parseFolders(String json) {
        List<FolderDto> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        String trimmed = json.trim();

        String[] parts;

        if (trimmed.startsWith("[")) {    // => tableau: [ {...}, {...} ]

            // enlever les crochets
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            if (trimmed.isBlank()) return result;

            // découper à la grosse: "},{"
            parts = trimmed.split("\\},\\s*\\{"); //=> les objets séparés par } , {
        } else {

            //un seul objet: { ... }
            parts = new String[]{ trimmed };
        }

        for (String part : parts) {
            String objet = part.trim();
            if (!objet.startsWith("{")) objet = "{" + objet;
            if (!objet.endsWith("}")) objet = objet + "}";

            FolderDto dto = new FolderDto();

            // "id": 1
            String idStr = JsonUtils.extractJsonNumberField(objet, "id");

            if (idStr != null) {
                dto.id = Integer.parseInt(idStr);
            }

            // "name": "Documents"
            dto.name = JsonUtils.extractJsonField(objet, "name");

            // "parent_id": null ou un nombre
            String parentStr = JsonUtils.extractJsonNumberField(objet, "parent_id");
            if (parentStr != null) {
                dto.parentId = Integer.parseInt(parentStr);
            } else {
                dto.parentId = null; // parent_id null => dossier racine
            }
            result.add(dto);
        }
        return result;
    }

    /**
     *
     * @param json
     * @return
     */
    private List<FileEntry> parseFiles(String json) {
        List<FileEntry> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;

        String trimmed = json.trim();
        String[] parts;

        if (trimmed.startsWith("[")) {

            // Cas tableau: [ {...}, {...} ]
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            if (trimmed.isBlank()) return result;

            // découpe grossière sur "},{"
            parts = trimmed.split("\\},\\s*\\{");
        } else {

            //un seul objet: { ... }
            parts = new String[]{ trimmed };
        }

        // découper à la grosse: "},{"
        String[] filesParts = trimmed.split("\\},\\s*\\{");

        for (String part : filesParts) {
            String objet = part.trim();
            if (!objet.startsWith("{")) objet = "{" + objet;
            if (!objet.endsWith("}")) objet = objet + "}";


            String idStr = JsonUtils.extractJsonNumberField(objet, "id");
            String name = JsonUtils.extractJsonField(objet, "original_name");
            String sizeStr = JsonUtils.extractJsonNumberField(objet, "size");
            String date = JsonUtils.extractJsonField(objet, "created_at");

            int id = (idStr != null) ? Integer.parseInt(idStr) : 0;
            long size = (sizeStr != null )? Long.parseLong(sizeStr) : 0L;

            if (name == null) {
                // sécurité : si jamais ton API change de champ un jour
                name = JsonUtils.extractJsonField(objet, "name");
            }

            result.add(FileEntry.of(id, name, size, date));

        }
        return result;
    }


    /**
     * Création de lien de partage
     * @param id
     * @param recipient
     * @return
     * @throws Exception
     */
    public String shareFile(int id, String recipient) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(id <= 0) {
            throw new IllegalArgumentException("id invalide");
        }

        //pour l'instant expire dans 7 jours!!!
        String expiresAt =  Instant.now().plus(7, ChronoUnit.DAYS).toString();

        String safeRecipient = (recipient == null) ? "" : recipient.trim();
        String label = safeRecipient.isBlank()
                ? "Partage fichier #" + id
                : "Partage fichier #" + id + " avec " + safeRecipient;

        String jsonBody = "{"
                + "\"kind\":\"file\","
                + "\"target_id\":" + id + ","
                + "\"label\":\"" + escapeJson(label) + "\","
                + "\"max_uses\":2,"
                + "\"expires_at\":\"" + expiresAt + "\""
                + "}";

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/shares"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();


        if(status == 201){

            //extraire url du backend
            String url = JsonUtils.extractJsonField(body, "url");
            url = JsonUtils.unescapeJsonString(url);

            if(url == null || url.isBlank()){

                //renvoyer body pour le debug
                return body;
            }
            return url;
        }

        String error = JsonUtils.extractJsonField(body, "error");
        if(error == null || error.isEmpty()){
            error = body;
        }

        throw new RuntimeException("Erreur de partage: HTTP " + status + "): " + error);

    }

    /**
     * lister les partages
     * @return
     * @throws Exception
     */
    public List<ShareItem> listShares() throws Exception {

        System.out.println("ApiClient - listShares() démarrage...");
        System.out.println("ApiClient - URL: " + baseUrl + "/shares");
        System.out.println("ApiClient - Token: " + (authToken != null ? "présent" : "absent"));

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/shares"))
                .header("Accept", "application/json")
                //.header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        System.out.println("ApiClient - Code de statut HTTP: " + status);
        System.out.println("ApiClient - Corps de la réponse: " + response.body());

        if(status != 200){
            throw new RuntimeException("Erreur de partage: HTTP " + status + "): " + response.body());
        }

        // Parser JSON en List<ShareItem>
        List<ShareItem> shares = JsonUtils.parseShareItem(response.body());
        System.out.println("ApiClient - Nombre de partages parsés: " + shares.size());

        return shares;

    }

    public void revokeShare(int id) throws Exception {

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/shares/" + id + "/revoke"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if(status != 200){
            throw new RuntimeException("Erreur de partage: HTTP " + status + "): " + response.statusCode());
        }

    }






}
