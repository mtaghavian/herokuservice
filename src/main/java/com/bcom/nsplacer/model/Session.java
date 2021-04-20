package com.bcom.nsplacer.model;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Session {

    private String id;

    private User user;

    private Long lastModified;

    public Session(String id, User user, Long lastModified) {
        this.id = id;
        this.user = user;
        this.lastModified = lastModified;
    }

    public void signOut() {
        setUser(null);
    }

    public boolean isSignedIn() {
        return user != null;
    }
}
