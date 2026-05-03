package dev.kekao.study;

public class UserCardNotFoundException extends RuntimeException {

    public UserCardNotFoundException(Long userCardId) {
        super("User card not found: " + userCardId);
    }
}
