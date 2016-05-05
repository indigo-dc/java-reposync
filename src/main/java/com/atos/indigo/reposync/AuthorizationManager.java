package com.atos.indigo.reposync;

import java.util.*;

/**
 * Created by jose on 20/04/16.
 */
public class AuthorizationManager {

    private static final long MAX_DURATION = 24*60*60*1000;

    private static final Map<String, Date> tokenList = new HashMap<>();

    public static String generateToken(String username, String password) {
        return UUID.randomUUID().toString();
    }

    public static Date add(String token) {
        Date expires = new Date(new Date().getTime() + MAX_DURATION);
        synchronized (tokenList) {
            tokenList.put(token, expires);
        }
        return expires;
    }

    public static boolean validate(String token) {
        synchronized (tokenList) {
            Date expires = tokenList.get(token);
            if (expires != null) {
                if (expires.after(new Date())) {
                    return true;
                } else {
                    tokenList.remove(token);
                }
            }
        }
        return false;
    }

    public static void trim() {
        List<String> toRemove = new ArrayList<>();
        Date now = new Date();
        synchronized (tokenList) {
            for (Map.Entry<String, Date> entry : tokenList.entrySet()) {
                if (entry.getValue().before(now)) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String token : toRemove) {
                tokenList.remove(token);
            }
        }
    }

}
