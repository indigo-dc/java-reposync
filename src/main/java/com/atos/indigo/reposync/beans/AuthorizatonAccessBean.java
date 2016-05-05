package com.atos.indigo.reposync.beans;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by jose on 20/04/16.
 */
public class AuthorizatonAccessBean implements Serializable{

    public class TokenBean {
        Date issued_at;
        Date expires;
        String id;
    }

    public class AccessBean {
        TokenBean token;
    }

    AccessBean access;

    public AuthorizatonAccessBean() {}

    public AuthorizatonAccessBean(Date issued, Date expires, String id) {
        TokenBean token = new TokenBean();
        token.issued_at = issued;
        token.expires = expires;
        token.id = id;

        access = new AccessBean();
        access.token = token;
    }

}
