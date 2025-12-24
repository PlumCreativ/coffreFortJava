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

    public static String unescapeJsonString(String s) {
        if (s == null) return null;
        return s.replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private JsonUtils() {
        // constructeur privé pour empêcher l'instanciation
    }
}
