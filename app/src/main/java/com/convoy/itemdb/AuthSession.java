package com.convoy.itemdb;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Holds the unlocked SQLCipher passphrase only for the current process. */
public final class AuthSession {
    private static final String PREFS = "secure_account";
    private static final String USERNAME = "user";
    private static final String DEFAULT_PASSWORD = "12345678";
    private static String passphrase;

    private AuthSession() {}

    public static boolean verify(Context context, String username, String password) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String salt = prefs.getString("salt", null);
        if (salt == null) {
            byte[] bytes = new byte[16]; new SecureRandom().nextBytes(bytes);
            salt = Base64.getEncoder().encodeToString(bytes);
            prefs.edit().putString("salt", salt).putString("username", USERNAME)
                    .putString("verifier", hash(salt, DEFAULT_PASSWORD)).apply();
        }
        String expectedUser = prefs.getString("username", USERNAME);
        if (!expectedUser.equals(username)) return false;
        String verifier = hash(salt, password);
        if (!verifier.equals(prefs.getString("verifier", ""))) return false;
        passphrase = password;
        return true;
    }

    public static String password() {
        if (passphrase == null) throw new IllegalStateException("The database is locked");
        return passphrase;
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest((salt + "\0" + password).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
