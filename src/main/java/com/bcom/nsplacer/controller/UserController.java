package com.bcom.nsplacer.controller;

import com.bcom.nsplacer.dao.SessionDao;
import com.bcom.nsplacer.dao.UserDao;
import com.bcom.nsplacer.misc.StreamUtils;
import com.bcom.nsplacer.model.Session;
import com.bcom.nsplacer.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SessionDao sessionDao;

    @Autowired
    private UserDao userDao;

    @PostMapping("/signIn")
    public String signIn(HttpServletRequest request, HttpServletResponse response, @RequestBody User user) throws Exception {
        HttpSession httpSession = request.getSession();
        User dbUser = userDao.findByUsername(user.getUsername());
        if ((dbUser != null) && dbUser.getPassword().equals(StreamUtils.hash(user.getPassword()))) {
            Session session = sessionDao.findById(httpSession.getId());
            session.setUser(dbUser);
            return "YES" + "\n"
                    + StreamUtils.encrypt(dbUser.getUsername() + "\n" + dbUser.getPassword(), StreamUtils.samplePassword) + "\n"
                    + "/evaluations.html";
        } else {
            return "NO\nIncorrect username and/or password";
        }
    }

    @PostMapping("/signUp")
    public String signUp(HttpServletRequest request, HttpServletResponse response, @RequestBody User user) throws Exception {
        HttpSession httpSession = request.getSession();
        User dbUser = userDao.findByUsername(user.getUsername());
        if (dbUser == null) {
            String problem = User.validateAll(user);
            if (problem == null) {
                dbUser = new User();
                dbUser.setFirstname(user.getFirstname());
                dbUser.setLastname(user.getLastname());
                dbUser.setUsername(user.getUsername());
                dbUser.setPassword(StreamUtils.hash(user.getPassword()));
                userDao.saveAndFlush(dbUser);
                String result = signIn(request, response, user);
                return result;
            } else {
                return "NO\n" + problem;
            }
        } else {
            return "NO\n" + "This username is already taken! Please choose another one!";
        }
    }

}
