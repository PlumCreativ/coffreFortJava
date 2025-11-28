package com.coffrefort.client.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtUtils {

    //décoder en json le payload qui est encodé en Base64URL
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

    //récupérer la valeur du champ "email" dans le payload JSON
    public static String extractEmail(String jwt){
        String json = decodePayload(jwt);
        if(json == null) return null;

        System.out.println("JWT Payload décodé: " + json); // Debug
        return JsonUtils.extractJsonField(json, "email");
    }

    //récupérer la valeur du champ "user_id" dans le payload JSON
    public static String extractUserID(String jwt){
        String json = decodePayload(jwt);
        if(json == null) return null;

        System.out.println("JWT Payload décodé: " + json); // Debug
        return JsonUtils.extractJsonNumberField(json, "user_id");
    }



    private JwtUtils(){};
}














