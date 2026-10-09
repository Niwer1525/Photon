package niwer.photon.util.session;

public record Pending2FA(String uuid, SessionScope scope, long createdAt, String checkoutToken) {}