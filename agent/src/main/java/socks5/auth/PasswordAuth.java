package socks5.auth;

public interface PasswordAuth {
    boolean auth(String user, String password);
}
