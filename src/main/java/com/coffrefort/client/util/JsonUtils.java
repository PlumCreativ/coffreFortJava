package com.coffrefort.client.util;


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

    private JsonUtils() {
        // constructeur privé pour empêcher l'instanciation
    }
}
