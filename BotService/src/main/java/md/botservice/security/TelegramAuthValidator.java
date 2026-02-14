//package md.botservice.security;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Component
//public class TelegramAuthValidator {
//
//    @Value("${telegram.bot.token}")
//    private String botToken;
//
//    public boolean validate(String initData) {
//        try {
//            Map<String, String> params = parseInitData(initData);
//            String hash = params.remove("hash");
//
//            if (hash == null) return false;
//
//            String dataCheckString = params.entrySet().stream()
//                    .sorted(Map.Entry.comparingByKey())
//                    .map(e -> e.getKey() + "=" + e.getValue())
//                    .collect(Collectors.joining("\n"));
//
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));
//
//            Mac hmac = Mac.getInstance("HmacSHA256");
//            hmac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
//            byte[] calculatedHash = hmac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
//
//            String calculatedHashHex = bytesToHex(calculatedHash);
//            return calculatedHashHex.equals(hash);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private Map<String, String> parseInitData(String initData) {
//        return Arrays.stream(initData.split("&"))
//                .map(s -> s.split("=", 2))
//                .filter(arr -> arr.length == 2)
//                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
//    }
//
//    private String bytesToHex(byte[] bytes) {
//        StringBuilder result = new StringBuilder();
//        for (byte b : bytes) {
//            result.append(String.format("%02x", b));
//        }
//        return result.toString();
//    }
//}
