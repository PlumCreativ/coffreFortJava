package com.coffrefort.client;

import com.coffrefort.client.config.AppProperties;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;
import com.coffrefort.client.util.JsonUtils;
import com.coffrefort.client.util.JwtUtils;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ApiClient {

    //propriétés
    private final HttpClient httpClient;
    private final String baseUrl;
    private String authToken;
    private final HttpClient http = HttpClient.newHttpClient();


    //méthodes
    // Constructeur par défaut avec URL localhost
    public ApiClient() {
        this("http://localhost:8080");
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

//        // Stocker le token en mémoire => megnezni hogy a helyettesites jo-e!!!
//        this.authToken = token;
//
//        // Décoder et stocker l'email depuis le token
//        String emailFromToken = JwtUtils.extractEmail(token);
//
//        // Stockage sécurisé en mémoire
//        AppProperties.set("auth.token", token);
//        if (emailFromToken != null && !emailFromToken.isEmpty()) {
//            AppProperties.set("auth.email", emailFromToken);
//            System.out.println("Email extrait du token: " + emailFromToken);
//        }
        setAuthToken(token);

        return token;
    }

    /**
     * Inscription d'un nouvel utilisateur puis connexion automatique
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @param quotaTotal
     * @param isAdmin
     * @return Le token JWT si succès
     * @throws Exception En cas d'erreur
     */
    public String register(String email, String password, int quotaTotal, Boolean isAdmin) throws Exception{
        // auth/register
        String registerUrl = baseUrl + "/auth/register";

        String registerJson = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"quota_total\":\"%d\",\"is_admin\":\"%b\"}",
                email, password, quotaTotal, isAdmin
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

        //décoder l'email dans le token => megnezni hogy a helyettesites jo-e!!!
//        String emailFromToken = JwtUtils.extractEmail(token);
//
//        //Stocker dans les propriétés globales
//        AppProperties.set("auth.token", token);
//        if(emailFromToken != null){
//            AppProperties.set("auth.email", emailFromToken);
//        }

        setAuthToken(token);


        return token;
    }

    /**
     * Définir manuellement le token (pour restauration depuis persistance)
     */
    public void setAuthToken(String token) {
        this.authToken = token;
        if (token != null) {
            AppProperties.set("auth.token", token);
            String email = JwtUtils.extractEmail(token);
            if (email != null) {
                AppProperties.set("auth.email", email);
            }
        }
    }


    //uploader un ou des fichier(s)
    public boolean uploadFile(File file) throws Exception {
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
        byte[] body = buildMultipartBody(file, boundary);

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

    private byte[] buildMultipartBody(File file, String boundary) throws Exception {
        String CRLF = "\r\n";

        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream"; // fallback
        }

        //Partie folder_id
        String folderPart =
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"folder_id\"" + CRLF + CRLF +
                        "2" + CRLF;

        //partie file
        String filePartHeader =
                "--" + boundary + CRLF +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + CRLF +
                "Content-Type: " + mimeType + CRLF + CRLF;

        String ending =
                CRLF + "--" + boundary + "--" + CRLF;

        byte[] fileBytes = Files.readAllBytes(file.toPath());

        byte[] folderBytes = folderPart.getBytes(StandardCharsets.UTF_8);
        byte[] headerBytes = filePartHeader.getBytes(StandardCharsets.UTF_8);
        byte[] endingBytes = ending.getBytes(StandardCharsets.UTF_8);

        byte[] fullBody = new byte[folderBytes.length + headerBytes.length + fileBytes.length + endingBytes.length];

        int pos = 0;
        System.arraycopy(folderBytes, 0, fullBody, pos, folderBytes.length);
        pos += folderBytes.length;
        System.arraycopy(headerBytes, 0, fullBody, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, fullBody, headerBytes.length, fileBytes.length);
        System.arraycopy(endingBytes, 0, fullBody, headerBytes.length + fileBytes.length, endingBytes.length);

        return fullBody;
    }


    //listFolders(), listFiles(folderId)





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



    //Exception personnalisée pour les erreurs d'authentification
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    //Exception pour les erreurs d'inscription
    public static class RegistrationException extends Exception {
        public RegistrationException(String message) {
            super(message);
        }
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
