package com.mg.cinephile.auth;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("Username already taken: " + username);
    }
}
