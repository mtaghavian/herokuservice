package com.bcom.nsplacer.config;

import com.bcom.nsplacer.NsPlacerApplication;
import com.bcom.nsplacer.dao.SessionDao;
import com.bcom.nsplacer.dao.UserDao;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.Session;
import com.bcom.nsplacer.model.User;
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
import java.util.*;

@Component
public class HttpInterceptor implements HandlerInterceptor {

    public static final String resourcePath = "./res";
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private SessionDao sessionDao;
    @Autowired
    private UserDao userDao;
    private Map<String, byte[]> cachedFiles = new HashMap<>();
    private Map<String, Long> cachedFilesDate = new HashMap<>();
    private Set<String> publicPaths = new HashSet<>();

    public HttpInterceptor() {
        File res = new File(resourcePath);
        res.mkdir();
        for (String filename : res.list()) {
            if (!filename.endsWith(".html")) {
                publicPaths.add("/" + filename);
            }
        }
        publicPaths.add("/signIn.html");
        publicPaths.add("/index.html");
        publicPaths.add("/cmd.html");
        publicPaths.add("/api/user/signIn");
        publicPaths.add("/api/message/*");
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
        if ("/".equals(uri)) {
            response.sendRedirect("/index.html");
            return false;
        }

        HttpSession httpSession = request.getSession();
        Session session = sessionDao.findById(httpSession.getId());
        if (session == null) {
            session = new Session(httpSession.getId(), null, System.currentTimeMillis());
            authenticateByBA(request, session);
            authenticateByCookies(request, session);
            sessionDao.save(session);
        }
        session.setLastModified(System.currentTimeMillis());
        if ("/api/user/signOut".equals(uri)) {
            session.signOut();
            response.sendRedirect("/signIn.html");
            return false;
        }
        if (session.isSignedIn()) {
            if ("/signIn.html".equals(uri)) {
                response.sendRedirect("/index.html");
                return false;
            }
        } else {
            if (!isPublicPath(uri)) {
                response.sendRedirect("/signIn.html");
                return false;
            }
        }

        if (!uri.startsWith("/api/")) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,DELETE,PUT,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "" + true);
            response.setHeader("Access-Control-Max-Age", "" + 180);
            response.setHeader("Cache-Control", "max-age=3600;");
            if (!uri.contains("/../")) {
                File file = new File("./res" + uri);
                if (file.exists()) {
                    response.setContentType("" + getMediaType(uri.substring(1)));
                    if (!(cachedFiles.containsKey(file.getAbsolutePath()) && cachedFilesDate.get(file.getAbsolutePath()) == file.lastModified())) {
                        cachedFiles.put(file.getAbsolutePath(), StreamUtils.readBytes(file));
                        cachedFilesDate.put(file.getAbsolutePath(), file.lastModified());
                    }
                    response.getOutputStream().write(cachedFiles.get(file.getAbsolutePath()));
                    response.setStatus(HttpServletResponse.SC_OK);
                    return false;
                }
            }
            response.setContentType("text/html");
            response.getOutputStream().write("<html><body><code> Page not found <br/> <a href=\"signIn.html\">Return to the main page</a></code></body></html>".getBytes());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        } else {
            if (isPublicPath(uri)) {
                return true;
            }
        }

        if (NsPlacerApplication.adminUsername.equals(session.getUser().getUsername()) && "/api/shutdown".equals(uri)) {
            System.exit(0);
        }

        return true;
    }

    private boolean isPublicPath(String uri) {
        for (String p : publicPaths) {
            if (p.contains("*")) {
                p = p.substring(0, p.indexOf("*"));
                if (uri.startsWith(p)) {
                    return true;
                }
            } else {
                if (p.equals(uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
    }

    public void authenticateByCookies(HttpServletRequest request, Session session) {
        try {
            Cookie cookie[] = request.getCookies();
            if (cookie == null) {
                return;
            }
            Properties cookies = new Properties();
            for (Cookie c : cookie) {
                cookies.put(c.getName(), c.getValue());
            }
            if (cookies.containsKey("auth")) {
                String auth = "" + cookies.get("auth");
                String[] split = StreamUtils.decrypt(auth, StreamUtils.samplePassword).split("\n");
                String username = split[0];
                String password = split[1];
                User dbUser = userDao.findByUsername(username);
                if ((dbUser != null) && dbUser.getPassword().equals(password)) {
                    session.setUser(dbUser);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void authenticateByBA(HttpServletRequest request, Session session) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                String[] split = auth.split(" ");
                if ("basic".equals(split[0].toLowerCase())) {
                    String base64 = split[1];
                    String cred = new String(Base64.getDecoder().decode(base64), "UTF-8");
                    String username = cred.substring(0, cred.indexOf(":"));
                    String password = cred.substring(cred.indexOf(":") + 1);
                    User dbUser = userDao.findByUsername(username);
                    if ((dbUser != null) && dbUser.getPassword().equals(StreamUtils.hash(password))) {
                        session.setUser(dbUser);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
