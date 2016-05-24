package com.atos.indigo.reposync;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.ws.rs.NameBinding;

/**
 * Created by jose on 23/05/16.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorized {
}
