package com.bcom.nsplacer.misc;

import com.bcom.nsplacer.NsPlacerApplication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;

public class StreamUtils {

    public static final String samplePassword = "Masoud Taghavian";
    private static ObjectMapper jsonMapper;

    private static void initMapper() {
        if (jsonMapper == null) {
            if (NsPlacerApplication.jsonMapper != null) {
                jsonMapper = NsPlacerApplication.jsonMapper;
            } else {
                jsonMapper = new ObjectMapper();
            }
        }
    }

    public static Object fromJson(String json, Class<? extends Object> type) throws JsonProcessingException {
        initMapper();
        return jsonMapper.readValue(json, type);
    }

    public static String toJson(Object obj, boolean pretty) throws JsonProcessingException {
        initMapper();
        if (pretty) {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } else {
            return jsonMapper.writeValueAsString(obj);
        }
    }

    public static void writeString(String str, File file) throws IOException {
        writeBytes(str.getBytes("UTF-8"), file);
    }

    public static void writeBytes(byte b[], File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        os.write(b);
        os.flush();
        os.close();
    }

    public static String readString(File file) throws IOException {
        return new String(readBytes(file), "UTF-8");
    }

    public static String readString(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(is, os, false, false);
        return new String(os.toByteArray(), "UTF-8");
    }

    public static byte[] readBytes(File file) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copy(new FileInputStream(file), os, true, true);
        byte b[] = os.toByteArray();
        return b;
    }

    public static void copy(InputStream is, OutputStream os, boolean closeInput, boolean closeOutput) throws IOException {
        byte b[] = new byte[10000];
        while (true) {
            int r = is.read(b);
            if (r < 0) {
                break;
            }
            os.write(b, 0, r);
        }
        if (closeInput) {
            is.close();
        }
        if (closeOutput) {
            os.flush();
            os.close();
        }
    }

    public static String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(s.getBytes("UTF-8"));
            return toHex(hash, "");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String encrypt(String text, String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(digest.digest(key.getBytes("UTF-8")), "AES"),
                new IvParameterSpec(new byte[16]));
        byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String text, String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(digest.digest(key.getBytes("UTF-8")), "AES"),
                new IvParameterSpec(new byte[16]));
        byte[] original = cipher.doFinal(Base64.getDecoder().decode(text));
        return new String(original, "UTF-8");
    }

    public static String hash(byte b[]) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(b);
            return toHex(hash, "");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String toHex(byte b[], String delimeter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String h = String.format("%h", b[i] & 0xff);
            h = (h.length() == 1) ? "0" + h : h;
            sb.append((i == 0) ? h : (delimeter + h));
        }
        return sb.toString();
    }

}
