package com.coffrefort.client.util;


import com.coffrefort.client.model.ShareItem;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    // Méthode utilitaire pour extraire un champ texte d'un petit JSON
    public static String extractJsonField(String json, String fieldName) {
        if (json == null) return null;

        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;

        int colon = json.indexOf(":", idx + pattern.length());
        if (colon == -1) return null;

        int firstQuote = json.indexOf("\"", colon);
        if (firstQuote == -1) return null;

        int secondQuote = json.indexOf("\"", firstQuote + 1);
        if (secondQuote == -1) return null;

        return json.substring(firstQuote + 1, secondQuote);
    }


    // méthode pour extraire un champ numérique (ex: user_id: 6)
    public static String extractJsonNumberField(String json, String fieldName) {
        if (json == null) return null;

        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;

        int colon = json.indexOf(":", idx + pattern.length());
        if (colon == -1) return null;

        int i = colon + 1;

        // sauter les espaces
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }

        if (i >= json.length()) return null;

        int start = i;

        // lire les chiffres (+ signe -)
        while (i < json.length()) {
            char c = json.charAt(i);
            if (!Character.isDigit(c) && c != '-') {
                break;
            }
            i++;
        }

        if (i == start) return null;

        return json.substring(start, i);
    }

    /**
     * Extrait un tableau JSON associé à un champ
     * @param json Le JSON source
     * @param fieldName Le nom du champ contenant le tableau
     * @return Le contenu du tableau (incluant les crochets [])
     */
    public static String extractJsonArrayField(String json, String fieldName) {
        if (json == null) return null;

        String pattern = "\"" + fieldName + "\"";
        int index = json.indexOf(pattern);
        if (index == -1) return null;

        int colon = json.indexOf(":", index + pattern.length());
        if (colon == -1) return null;

        //chercher le crochet ouvrant
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }

        if (i >= json.length() || json.charAt(i) != '['){
            return null;
        }

        int start = i;
        int bracketCount = 1;
        i++;

        //parcourir jusqu'à trouver le crochet fermant correspondant
        while ( i < json.length() && bracketCount > 0 ){
            char c = json.charAt(i);
            if (c == '['){
                bracketCount++;
            }else if (c == ']'){
                bracketCount--;
            }
            i++;
        }

        if(bracketCount == 0){
            return json.substring(start, i);
        }
        return null;
    }


    public static String unescapeJsonString(String s) {
        if (s == null) return null;
        return s.replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private JsonUtils() {
        // constructeur privé pour empêcher l'instanciation
    }


    public static List<ShareItem> parseShareItem(String json) {
        System.out.println("JsonUtils - parseShareItem() - JSON reçu: " + json);

        List<ShareItem> result = new ArrayList<>();

        if (json == null || json.isBlank()) {
            System.out.println("JsonUtils - JSON vide ou null");
            return result;
        }

        String arrayContent = json.trim();

        // Vérifier si le JSON contient un objet avec un champ "shares"
        if (arrayContent.startsWith("{")) {
            System.out.println("JsonUtils - JSON est un objet, extraction du champ 'shares'");

            String sharesArray = extractJsonArrayField(json, "shares");


            if (sharesArray == null) {
                System.err.println("JsonUtils - ERREUR: Impossible d'extraire le champ 'shares'");
                return result;
            }

            arrayContent = sharesArray;
            System.out.println("JsonUtils - Tableau 'shares' extrait: " + arrayContent);
        }

        // Retirer les crochets [] si présents
        String trimmed = arrayContent.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        trimmed = trimmed.trim();

        if (trimmed.isEmpty()) {
            System.out.println("JsonUtils - Tableau vide");
            return result;
        }

        System.out.println("JsonUtils - Contenu après nettoyage (premiers 200 chars): " +
                (trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed));

        // Séparation de chaque objet JSON
        List<String> objects = splitJsonObjects(trimmed);
        System.out.println("JsonUtils - Nombre d'objets détectés: " + objects.size());

        for (int idx = 0; idx < objects.size(); idx++) {
            String o = objects.get(idx).trim();

            if (!o.startsWith("{")) {
                o = "{" + o;
            }
            if (!o.endsWith("}")) {
                o = o + "}";
            }

            System.out.println("JsonUtils - Parsing objet " + idx + ": " + o);

            ShareItem item = new ShareItem();

            // id
            String id = extractJsonNumberField(o, "id");
            if (id != null) {
                item.setId(Integer.parseInt(id));
                System.out.println("  - id: " + id);
            }

            //resource => nom du fichier
            String fileName = unescapeJsonString(extractJsonField(o, "file_name"));
            item.setResource(fileName != null ? fileName : "Fichier inconnu");
            System.out.println("  - file_name: " + fileName);

            // resource (utiliser label comme resource pour l'affichage)
            String label = unescapeJsonString(extractJsonField(o, "label"));
            item.setLabel(label);
            System.out.println("  - label: " + label);

            // expires_at
            String expiresAt = unescapeJsonString(extractJsonField(o, "expires_at"));
            item.setExpiresAt(expiresAt != null ? expiresAt : "-");
            System.out.println("  - expires_at: " + expiresAt);

            // remaining_uses
            String remaining = extractJsonNumberField(o, "remaining_uses");
            if (remaining != null && !remaining.isEmpty() && !"null".equalsIgnoreCase(remaining)) {
                item.setRemainingUses(Integer.parseInt(remaining));
                System.out.println("  - remaining_uses: " + remaining);
            } else {
                item.setRemainingUses(null);
                System.out.println("  - remaining_uses: null (illimité)");
            }

            // url
            String url = unescapeJsonString(extractJsonField(o, "url"));
            item.setUrl(url);
            System.out.println("  - url: " + url);

            // is_revoked
            String revoked = extractJsonNumberField(o, "is_revoked");
            boolean isRevoked = "1".equals(revoked);
            item.setRevoked(isRevoked);
            System.out.println("  - is_revoked: " + isRevoked);

            result.add(item);
        }

        System.out.println("JsonUtils - Total d'items parsés: " + result.size());
        return result;
    }

    /**
     * Sépare une chaîne contenant plusieurs objets JSON
     * Plus robuste que split() car gère les virgules dans les valeurs
     */
    private static List<String> splitJsonObjects(String content) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '{') {
                if (braceCount == 0) {
                    start = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(content.substring(start, i + 1));
                }
            }
        }

        return objects;
    }
}
