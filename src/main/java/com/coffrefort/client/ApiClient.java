package com.coffrefort.client;

import com.coffrefort.client.config.AppProperties;
import com.coffrefort.client.model.*;
import com.coffrefort.client.util.JsonUtils;
import com.coffrefort.client.util.JwtUtils;
import com.coffrefort.client.util.UIDialogs;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.JSONPObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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

//import pour la classe statique ProgressBodyPublisher
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;


public class ApiClient {

    //propriétés
    private static ApiClient INSTANCE;
    private final HttpClient httpClient;
    private final String baseUrl;
    private String authToken;
    private final HttpClient http = HttpClient.newHttpClient();


    //méthodes

    /**
     * Initialise l’ApiClient avec l’URL par défaut (localhost)
     */
    public ApiClient() {
        this("http://localhost:9081");
    }

    /**
     * Initialise l’ApiClient avec une URL de backend personnalisée
     * @param baseUrl
     */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.authToken = null;
    }

    /**
     * Authentification utilisateur avec email et mot de passe via /auth/login
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
     * Inscrit un utilisateur via /auth/register puis le connecte via /auth/login et stocke le JWT
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
     * @return l’instance singleton d’ApiClient (créée au premier appel)
     */
    public static ApiClient getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ApiClient();
        }
        return INSTANCE;
    }

    /**
     * Enregistre le token JWT et persist email/userId extraits du token dans AppProperties
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
     * Crée un dossier (racine ou enfant) via POST /folders pour l’utilisateur connecté
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



    /**
     * Upload un fichier dans un dossier (ou racine) via POST /files en multipart/form-data
     * Upload un fichier dans la racine en réutilisant uploadFile(file, null)
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
     * Récupère tous les dossiers via GET /folders et reconstruit l’arborescence en NodeItem
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
     * Récupère la liste des fichiers d’un dossier via GET /files?folder=... et les parse en FileEntry
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
     * Déconnecte l’utilisateur en supprimant le token en mémoire et dans AppProperties
     */
    public void logout() {
        this.authToken = null;
        AppProperties.remove("auth.token");
        AppProperties.remove("auth.email");
        System.out.println("Déconnexion effectuée.");
    }

    /**
     * Indique si un token JWT valide est présent côté client
     */
    public boolean isAuthenticated() {
        return this.authToken != null && !this.authToken.isEmpty();
    }

    /**
     * Retourne le token JWT actuellement stocké en mémoire
     */
    public String getAuthToken() {
        return this.authToken;
    }

    /**
     * Récupère le quota utilisateur via GET /me/quota et retourne un objet Quota (used/total)
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
     * Supprime un fichier via DELETE /files/{id} (toutes ses versions) =>ok
     * retourne true si succès
     * @param fileId
     * @return
     * @throws Exception
     */
    public void deleteFile(int fileId) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0){
            throw new IllegalArgumentException("FileId invalide: " + fileId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/files/" + fileId))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        //204 => requête réussi, pas besoin de quitter la page
        if(status == 200 || status == 204) {
            return;
        }

        if(status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }

        if(status == 404) {
            throw new RuntimeException("Fichier introuvable");
        }

        //autres erreurs
        String error = JsonUtils.extractJsonField(body, "error");
        error = JsonUtils.unescapeJsonString(error);

        if(error == null || error.isEmpty()){
            error = body;
        }

        throw new RuntimeException("Erreur de suppression (HTTP " + status + "): " + error);
    }

    /**
     * Supprime un fichier via DELETE /files/{file_id}/versions/{id} =>ok
     * @param fileId
     * @param versionId
     * @throws Exception
     */
    public void deleteVersion(int fileId, int versionId) throws Exception{
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0){
            throw new IllegalArgumentException("FileId invalide: " + fileId);
        }

        if(versionId <= 0){
            throw new IllegalArgumentException("VersionId invalide: " + versionId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/files/" + fileId + "/versions/" + versionId))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        //204 => requête réussi, pas besoin de quitter la page
        if(status == 200 || status == 204) {
            return;
        }

        if(status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }

        if(status == 404) {
            String error = JsonUtils.extractJsonField(body, "error");
            error = JsonUtils.unescapeJsonString(error);

            if (error == null || error.isEmpty()) {
                error = "Fichier ou version introuvable";
            }

            throw new RuntimeException(error);
        }

        //autres erreurs
        String error = JsonUtils.extractJsonField(body, "error");
        error = JsonUtils.unescapeJsonString(error);

        if(error == null || error.isEmpty()){
            error = body;
        }

        throw new RuntimeException("Erreur de suppression (HTTP " + status + "): " + error);
    }


    /**
     * Supprime un dossier via DELETE /folders/{id} et retourne true si succès =>ok
     * @param folderId
     * @return
     * @throws Exception
     */
    public void deleteFolder(int folderId) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(folderId <= 0){
            throw new IllegalArgumentException("FolderId invalide: " + folderId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/folders/" + folderId))
                //.header("Content-Type", "application/json") //=> lehet hogy le kell venni
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("DELETE /folders/" + folderId + " => status " + status);
        System.out.println("DELETE /folders/" + folderId + " =>  body: " + body);

        //204 => requête réussi, pas besoin de quitter la page
        if(status == 200 || status == 204) {
            return;
        }

        if(status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }

        if(status == 404) {
            throw new RuntimeException("Dossier introuvable");
        }

        //dossier non vide
        if(status == 400){
            String error = JsonUtils.extractJsonField(body, "error");
            error = JsonUtils.unescapeJsonString(error);

            if(error == null || error.isEmpty()){
                error = "Dossier non vide";
            }
            throw new RuntimeException(error);
        }

        //autres erreurs
        String error = JsonUtils.extractJsonField(body, "error");
        error = JsonUtils.unescapeJsonString(error);

        if(error == null || error.isEmpty()){
            error = body;
        }

        throw new RuntimeException("Erreur de suppression (HTTP " + status + "): " + error);
    }

    /**
     * Télécharge un fichier via GET /files/{id}/download et l’écrit dans le fichier cible
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


    public void downloadFileVersionTo(long fileId, int version,  File target, DownloadProgressListener progress) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0) throw new IllegalArgumentException("fileId invalide");
        if(version <= 0) throw new IllegalArgumentException("version invalide");
        if(target == null) throw new IllegalArgumentException("target invalide");

        String url = baseUrl + "/files/" + fileId + "/versions/" + version + "/download";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if(response.statusCode() != 200) {

            //lire le message d'erreur du backend
            String errorMessage = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("HTTP " +response.statusCode() + " lors du téléchargement: " + errorMessage);
        }

        long total = -1;
        String cl = response.headers().firstValue("Content-Length").orElse(null);
        if(cl != null) {
            try{
                total = Long.parseLong(cl); // => convertion p.ex. "84011" → 84011L
            }catch (Exception ignored){
                //ingore => total reste -1
            }
        }

        //copier le flux HTTP vers le fichier cible + progression
        //try-with-resources : ferme automatiquement in et out à la fin, même s'il y a erreur.
        try (InputStream in = response.body();  //=> flux entrant depuis HTTP
            var out = Files.newOutputStream(target.toPath())) {  //=> flux d'écriture vers le fichier local target

            byte[] buffer = new byte[8192];  //=> tableau 8192 octets => 8kb
            long done = 0;  //=>nbre octets déjà téléchargé
            int read;  //=> nbre octets lus par chaque itération

            if(progress != null){
                progress.onProgress(0, total);
            }

            while((read = in.read(buffer)) != -1) {  //= retourne -1 si on atteint la fin du flux (EOF)
                out.write(buffer, 0, read);
                done += read;                       //=> màj le total téléchargé

                if(progress != null){
                    progress.onProgress(done, total);
                }
            }
        }
    }


    /**
     * Crée un lien de partage via POST /shares et retourne l’URL générée par le backend
     * @param id
     * @param data Format: "recipient|maxUses|expiresDays|allowVersions"
     * @return
     * @throws Exception
     */
    public String shareFile(int id, String data) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(id <= 0) {
            throw new IllegalArgumentException("id invalide");
        }

        //découper => destinataire|maxUses|expiresDays
        String[] parts = data.split("\\|");
        if(parts.length != 4){
            throw new IllegalArgumentException("Format de donées invalide");
        }

        String destinataire = parts[0];
        String maxUsesStr = parts[1];
        String expiresDaysStr = parts[2];
        boolean allowVersions = Boolean.parseBoolean(parts[3]);

        // construire le JSON
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"kind\":\"file\",");
        jsonBody.append("\"target_id\":").append(id).append(",");
        jsonBody.append("\"label\":\"").append(escapeJson("Partage avec " + destinataire)).append("\",");
        jsonBody.append("\"allow_fixed_versions\":").append(allowVersions);

        //max uses
        if(!"null".equals(maxUsesStr)){
            try{
                int maxUses = Integer.parseInt(maxUsesStr);
                jsonBody.append("\"max_uses\":").append(maxUses);
            }catch (NumberFormatException e){
                //ignore
            }
        }

        //expires at
        if(!"null".equals(expiresDaysStr)){
            try{
                int expireAt = Integer.parseInt(expiresDaysStr);
                jsonBody.append("\"expires_at\":").append(expireAt).append("\"");
            }catch (NumberFormatException e){
                //ignore
            }
        }
        jsonBody.append("}");

        System.out.println("ApiClient - JSON envoyé: " + jsonBody);

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/shares"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("ApiClient - Réponse HTTP: " + status);
        System.out.println("ApiClient - Corps: " + body);

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
        }else{
            error = JsonUtils.unescapeJsonString(error);
        }

        throw new RuntimeException("Erreur de partage: (HTTP " + status + "): " + error);
    }

    /**
     * Récupère tous les partages via GET /shares et les parse en List<ShareItem>
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
            throw new RuntimeException("Erreur de partage: (HTTP " + status + "): " + response.body());
        }

        // Parser JSON en List<ShareItem>
        List<ShareItem> shares = JsonUtils.parseShareItem(response.body());
        System.out.println("ApiClient - Nombre de partages parsés: " + shares.size());

        return shares;
    }

    /**
     * Révoque un partage via PATCH /shares/{id}/revoke  => ok
     * @param id
     * @throws Exception
     */
    public void revokeShare(int id) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/shares/" + id + "/revoke"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();


        if(status == 200){
            //UIDialogs.showInfo("Succès", null, "Partage #\" + id + \" révoqué avec succès");
            System.out.println("Partage #" + id + " révoqué avec succès");
            return;
        }

        // Erreur d'authentification
        if (status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }

        // Erreur 404 : partage introuvable
        if (status == 404) {
            throw new Exception("Partage #" + id + " introuvable.");
        }

        String errorMessage = parseErrorMessage(response.body(), "Erreur lors de la révocation du partage");
        throw new Exception(errorMessage + " (HTTP " + status + ")");

    }

    /**
     * supprimer un partage  => OK
     * @param id
     * @return
     * @throws Exception
     */
    public void deleteShare(int id) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/shares/" + id))
                //.header("Content-Type", "application/json") //=> lehet hogy le kell venni
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        //204 => requête réussi, pas besoin de quitter la page
        if(status == 200 || status == 204) {
            System.out.println("Suppression du partage a réussi");
            return;
        }

        if(status == 401 || status == 403) {
            throw new AuthenticationException("Non autorisé : token invalide ou expiré.");
        }

        if (status == 404) {
            throw new Exception("Partage introuvable (déjà supprimé ou n'existe pas).");
        }

        //autres erreur
        String errorMessage = parseErrorMessage(response.body(), "Erreur lors de la suppression du partage");
        throw new Exception(errorMessage + " (HTTP " + status + ")");
    }


    //**************************************  Methode PRIVATE   **************************************

    private String parseErrorMessage(String body, String defaultMessage) {
        try {
            String error = JsonUtils.extractJsonField(body, "error");
            if (error!= null && !error.isEmpty()) {
                return JsonUtils.unescapeJsonString(error);
            }
        } catch (Exception e) {
            // Ignore : le body n'est pas du JSON valide
        }
        return defaultMessage;
    }

    //************************************************************************************************


    /**
     * Renomme un dossier via PUT /folders/{id} avec le nouveau nom
     * @param folderId
     * @param newName
     * @throws Exception
     */
    public void renameFolder(int folderId, String newName, String currentName) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant)."); //=> état de l'objet invalide
        }

        if(folderId <= 0){
            throw new IllegalArgumentException("FolderId invalide " + folderId); //=> argument mauvais
        }

        if(newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }

//        if(newName.trim().equals(currentName)){
//            throw new IllegalArgumentException("Le nouveau nom est identique à l'ancien");
//        }

        String jsonBody = "{"
                + "\"name\":\"" + escapeJson(newName.trim()) + "\""
                + "}";

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/folders/" + folderId))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        if(status == 200 || status == 204) return;

        String error = JsonUtils.extractJsonField(body, "error");
        if(error == null || error.isEmpty()){
            error = body;
        }

        throw new RuntimeException("Erreur de renameFolder: (HTTP " + status + "): " + error);
    }

    /**
     * Renomme un fichier via PUT /files/{id} avec le nouveau nom
     * @param fileId
     * @param newName
     * @throws Exception
     */
    public void renameFile(int fileId, String newName) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0){
            throw new IllegalArgumentException("FileId invalide " + fileId);
        }

        if(newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }

        String jsonBody = "{"
                + "\"name\":\"" + escapeJson(newName.trim()) + "\""
                + "}";

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/files/" + fileId))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        if(status == 200 || status == 204) return;

        String error = JsonUtils.extractJsonField(body, "error");
        if(error == null || error.isEmpty()){
            error = body;
        }

        throw new RuntimeException("Erreur de renameFile: (HTTP " + status + "): " + error);
    }

    /**
     * Interface callback pour remonter l’avancement (octets envoyés / total) pendant un upload
     */
    public interface ProgressListener {
        void onProgress(long sentBytes, long totalBytes);
    }

    public interface DownloadProgressListener{
        void onProgress(long done, long total);
    }


    /**
     * Liste les versions d’un fichier via GET /files/{id}/versions et retourne une List<VersionEntry>
     * @param fileId
     * @param page
     * @param limit
     * @return
     * @throws Exception
     */
    public List<VersionEntry> listFileVersions(int fileId, int page, int limit) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0){
            throw new IllegalArgumentException("FileId invalide " + fileId);
        }

        if(page < 0) page = 1;
        if(limit < 0) limit = 20;

        String url = baseUrl + "/files/" + fileId + "/versions?page=" + page + "&limit=" + limit;

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                //.header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("GET " + url + "status = " + status);
        System.out.println("GET versions body = " + body );

        if (status == 401 || status == 403){
            throw new AuthenticationException("Non autorise :  token invalide ou expire");
        }

        if(status != 200){
            String apiError = JsonUtils.extractJsonField(body, "error");

            if(apiError == null || apiError.isEmpty()){
                apiError = body;
                throw new RuntimeException("Erreur listFileVersions (HTTP " + status + "): " + apiError);
            }
        }
        return JsonUtils.parseVersionEntriesFromVersionsList(body);
    }

    /**
     * Upload une nouvelle version d’un fichier via POST /files/{id}/versions en multipart avec suivi de progression
     * @param fileId
     * @param newFile
     * @param progress
     * @throws Exception
     */
    public void uploadNewVersion(int fileId, File newFile, ProgressListener progress) throws Exception {

        if(newFile == null || !newFile.exists()){
            throw new Exception("Fichier invalide");
        }

        //vérifier le token
        String token = this.authToken;
        if(token == null || token.isEmpty()){
            token = AppProperties.get("auth.token");
        }
        if(token == null || token.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0){
            throw new IllegalArgumentException("FileId invalide " + fileId);
        }

        String url = baseUrl + "/files/" + fileId + "/versions";
        String boundary = "----CryptoVaultBoundary" + UUID.randomUUID();
        String contentType = "multipart/form-data; boundary=" + boundary;

        //construction le multipart en bytes
        byte[] bodyBytes = buildMultipartBody(newFile, boundary, null);
        long total = bodyBytes.length;

        HttpRequest.BodyPublisher publisher = new ProgressBodyPublisher(
                HttpRequest.BodyPublishers.ofByteArray(bodyBytes),
                total,
                progress
        );

        // Construction de la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", contentType)
                .header("Authorization", "Bearer " + token)
                .POST(publisher)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("POST " + url + "status = " + status);
        System.out.println("POST versions body = " + body );

        if (status == 401 || status == 403){
            throw new AuthenticationException("Non autorise :  token invalide ou expire");
        }

        if(status != 201){
            String apiError = JsonUtils.extractJsonField(body, "error");

            if(apiError == null || apiError.isEmpty()){
                apiError = body;
            }
            throw new RuntimeException("Erreur uploadNewVersion (HTTP " + status + "): " + apiError);
        }
    }

    public FileEntry getFile(int fileId) throws Exception {
        if(authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException("Utilisateur non authentifié (auth.token manquant).");
        }

        if(fileId <= 0){
            throw new IllegalArgumentException("FileId invalide " + fileId);
        }

        String url = baseUrl + "/files/" + fileId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if(status != 200){
            String apiError =  JsonUtils.extractJsonField(response.body(), "error");

            if(apiError == null || apiError.isEmpty()){
                apiError = response.body();
            }

            throw new RuntimeException("Erreur getFile (HTTP " + status + "): " + apiError);
        }

        return JsonUtils.parseFileEntry(response.body());


    }


    //méthodes private  => HELPERS

    /**
     * DTO interne pour parser les dossiers (id, name, parentId) avant de reconstruire l’arbre
     * DTO =Data Transfer Object => pour structurer les données pour les rendre faciles à échanger
     */
    private static class FolderDto{
        int id;
        String name;
        Integer parentId;
    }


    /**
     * construction d'un NodeItem "racine" virtuel avec tous les dossiers enfants
     * Reconstruit l’arborescence NodeItem (racine virtuelle + enfants) à partir du JSON des dossiers
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
    /**
     * Parse un JSON de dossiers (tableau ou objet) en liste de FolderDto (id/name/parentId)
     * Parse un JSON de type:
     *   [ { "id":1, "name":"Docs", "parent_id":null }, ... ]
     * ou un seul objet:
     *   { "id":1, "name":"Docs", "parent_id":null, ... }
     * @param json
     * @return
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
     * Parse un JSON de fichiers en liste de FileEntry (id, nom, taille, date)
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
            String updatedDate =  JsonUtils.extractJsonField(objet, "updated_at");

            int id = (idStr != null) ? Integer.parseInt(idStr) : 0;
            long size = (sizeStr != null )? Long.parseLong(sizeStr) : 0L;

            if (name == null) {
                // sécurité : si jamais ton API change de champ un jour
                name = JsonUtils.extractJsonField(objet, "name");
            }

            result.add(FileEntry.of(id, name, size, date, updatedDate));
        }
        return result;
    }

    /**
     * Construit le body multipart/form-data (folder_id optionnel + part 'file') pour les uploads
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

        // ---- Partie "folder_id" (si fourni)
        if (folderId != null) {
            String folderPart =
                    "--" + boundary + CRLF +
                            "Content-Disposition: form-data; name=\"folder_id\"" + CRLF + CRLF +
                            folderId + CRLF;

            output.write(folderPart.getBytes(StandardCharsets.UTF_8));
        }

        // ---- Partie "file"
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
     * Échappe les caractères spéciaux pour insérer une valeur proprement dans une chaîne JSON
     * @param value
     * @return
     */
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
     * Exception levée en cas d’erreur d’authentification (token manquant/invalide, accès refusé)
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * Exception levée en cas d’échec d’inscription ou de connexion automatique après inscription
     */
    public static class RegistrationException extends Exception {
        public RegistrationException(String message) {
            super(message);
        }
    }


    /**
     * BodyPublisher wrapper qui comptabilise les octets envoyés et notifie un ProgressListener pendant l’upload
     */
    private static class ProgressBodyPublisher implements HttpRequest.BodyPublisher {
        private final HttpRequest.BodyPublisher delegate;
        private final long totalBytes;
        private final ProgressListener listener;

        ProgressBodyPublisher(HttpRequest.BodyPublisher delegate, long totalBytes, ProgressListener listener) {
            this.delegate = delegate;
            this.totalBytes = totalBytes;
            this.listener = listener;
        }

        @Override
        public long contentLength() {
            return totalBytes >= 0 ? totalBytes : delegate.contentLength();
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(new Flow.Subscriber<>() {
                long sent = 0;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                    if (listener != null) listener.onProgress(0, totalBytes);
                }

                @Override
                public void onNext(ByteBuffer item) {
                    sent += item.remaining();
                    if (listener != null) listener.onProgress(sent, totalBytes);
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                    if (listener != null) listener.onProgress(totalBytes, totalBytes);
                    subscriber.onComplete();
                }
            });
        }
    }








}
