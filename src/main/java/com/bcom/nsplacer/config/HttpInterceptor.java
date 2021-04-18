package com.bcom.nsplacer.config;

import com.bcom.nsplacer.misc.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class HttpInterceptor implements HandlerInterceptor {

    @Autowired
    private ServletContext servletContext;

    private Map<String, byte[]> cachedFiles = new HashMap<>();
    private Map<String, Long> cachedFilesDate = new HashMap<>();

    public HttpInterceptor() {
    }

    public MediaType getMediaType(String fileName) {
        try {
            String mimeType = servletContext.getMimeType(fileName);
            MediaType mediaType = MediaType.parseMediaType(mimeType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.equals("/")) {
            response.sendRedirect("/index.html");
            return false;
        }
        HttpSession httpSession = request.getSession();
        if (!uri.startsWith("/api/")) {
            File file = new File("res/" + uri.substring(1));
            if (file.exists()) {
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT,OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "*");
                response.setHeader("Access-Control-Allow-Credentials", "" + true);
                response.setHeader("Access-Control-Max-Age", "" + 180);
                response.setContentType("" + getMediaType(uri.substring(1)));
                if (!(cachedFiles.containsKey(file.getAbsolutePath()) && cachedFilesDate.get(file.getAbsolutePath()) == file.lastModified())) {
                    cachedFiles.put(file.getAbsolutePath(), StreamUtils.readBytes(file));
                    cachedFilesDate.put(file.getAbsolutePath(), file.lastModified());
                }
                response.getOutputStream().write(cachedFiles.get(file.getAbsolutePath()));
            }
            response.setStatus(HttpServletResponse.SC_OK);
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
    }

    public void login(String username, String password) {
        // implement login
    }

    public void loginWithCookies(Cookie cookie[]) {
        if (cookie == null) {
            return;
        }
        Properties cookies = new Properties();
        for (Cookie c : cookie) {
            cookies.put(c.getName(), c.getValue());
        }
        if (cookies != null) {
            if (cookies.containsKey("username") && cookies.containsKey("password")) {
                String username = "" + cookies.get("username");
                String password = "" + cookies.get("password");
                login(username, password);
            }
        }
    }

    private void loginWithBasicAuth(HttpServletRequest request) throws UnsupportedEncodingException {
        String auth = request.getHeader("Authorization");
        if (auth != null) {
            String[] split = auth.split(" ");
            if ("basic".equals(split[0].toLowerCase())) {
                String base64 = split[1];
                String cred = new String(Base64.getDecoder().decode(base64), "UTF-8");
                String username = cred.substring(0, cred.indexOf(":"));
                String password = cred.substring(cred.indexOf(":") + 1, cred.length());
                login(username, password);
            }
        }
    }
}
