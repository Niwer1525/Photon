package niwer.photon.util.session;

import niwer.photon.objects.ObjectUserAccount;

public class SessionSnapshot {
    ObjectUserAccount account;
    long createdAt;
    String csrf = null;
    String refreshToken = null;

    public SessionSnapshot(ObjectUserAccount account, long createdAt) {
        this(account, createdAt, null, null);
    }

    public SessionSnapshot(ObjectUserAccount account, long createdAt, String csrf) {
        this(account, createdAt, csrf, null);
    }

    public SessionSnapshot(ObjectUserAccount account, long createdAt, String csrf, String refreshToken) {
        this.account = account;
        this.createdAt = createdAt;
        this.csrf = csrf;
        this.refreshToken = refreshToken;
    }

    public ObjectUserAccount account() { return this.account; }

    public long createdAt() { return this.createdAt; }

    public String csrf() { return this.csrf; }

    public String refreshToken() { return this.refreshToken; }
}