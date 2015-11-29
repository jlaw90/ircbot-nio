package net.newbiehacker.bot.util;

import java.security.MessageDigest;

/**
 * {@code GenericHelper}
 *
 * @author James Lawrence
 * @version 1
 */
public final class GenericHelper {
    public static String sha(String s) {
        try{
            MessageDigest sha = MessageDigest.getInstance("SHA");
            sha.update(s.toLowerCase().getBytes());
            StringBuilder sb = new StringBuilder();
            byte[] shad = sha.digest();
            for(byte b: shad)
                sb.append(Integer.toHexString(b & 0xff));
            return sb.toString().toLowerCase();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}