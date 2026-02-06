package com.coffrefort.client.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtUtils {


    /**
     * Décode la partie payload d’un JWT ( encodé en Base64URL) et la retourne en JSON sous forme de String
     * @param jwt
     * @return
     */
    public static String decodePayload(String jwt){
        if(jwt == null || jwt.isEmpty()) return null;

        String[] parts = jwt.split("\\.");

        //JWT a 3 parties: header.payload.signature !!!
        if(parts.length != 3) return null;

        String payload = parts[1]; // 2ième partie =>i= 1

        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] decoded = decoder.decode(payload);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * Extrait le champ "email" du payload du JWT décodé
     * @param jwt
     * @return
     */
    public static String extractEmail(String jwt){
        String json = decodePayload(jwt);
        if(json == null) return null;

        System.out.println("JWT Payload décodé: " + json); // Debug
        return JsonUtils.extractJsonField(json, "email");
    }


    /**
     * Extrait le champ numérique "user_id" du payload du JWT décodé
     * @param jwt
     * @return
     */
    public static String extractUserID(String jwt){
        String json = decodePayload(jwt);
        if(json == null) return null;

        System.out.println("JWT Payload décodé: " + json); // Debug
        return JsonUtils.extractJsonNumberField(json, "user_id");
    }

    /**
     * Extrait le champ is_admin du payload du JWT décodé (format: 0, 1, false, true)
     * @param jwt
     * @return
     */
    public static Boolean extractIsAdmin(String jwt){
        String json = decodePayload(jwt);
        if(json == null) return false;

        System.out.println("JWT Payload décodé: " + json); // Debug
        String isAdminStr = JsonUtils.extractJsonFieldAny(json, "is_admin");

        System.out.println("DEBUG - is_admin extrait du JWT: '" + isAdminStr + "'");

        if(isAdminStr != null){
            isAdminStr = isAdminStr.trim();
            return "true".equalsIgnoreCase(isAdminStr) || "1".equals(isAdminStr);
        }

        return false;
    }


    /**
     * Empêche l’instanciation de la classe utilitaire JwtUtils
     */
    private JwtUtils(){};
}














