package com.example.myapplication;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CryptoManager {

    private static final String TAG = "CryptoManager";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH_BIT = 128;

    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
    }

    public static String decrypt(String base64Data, SecretKey key) throws Exception {
        try {
            byte[] input = Base64.decode(base64Data, Base64.NO_WRAP);
            ByteBuffer buffer = ByteBuffer.wrap(input);

            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.e(TAG, "❌ Fehler bei der Entschlüsselung (möglicherweise ungültiger Key, IV oder Daten)", e);
            throw new SecurityException("Entschlüsselung fehlgeschlagen", e);
        }
    }
}
