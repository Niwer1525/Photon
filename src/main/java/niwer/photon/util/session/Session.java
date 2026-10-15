package niwer.photon.util.session;

import niwer.photon.objects.ObjectUserAccount;

public class Session {
    String token;
    String refreshToken;
    ObjectUserAccount account;

    public Session(String token, String refreshToken, ObjectUserAccount account) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.account = account;
    }

    public String token() { return this.token; }

    public String refreshToken() { return this.refreshToken; }

    public ObjectUserAccount account() { return this.account; }
}