package com.example.herokuservice.misc;

import com.example.herokuservice.HerokuserviceApplication;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;

public class StreamUtils {

    private static ObjectMapper jsonMapper;

    private static void initMapper() {
        if (jsonMapper == null) {
            if (HerokuserviceApplication.jsonMapper != null) {
                jsonMapper = HerokuserviceApplication.jsonMapper;
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
}
