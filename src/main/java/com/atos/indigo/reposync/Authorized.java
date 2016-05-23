package com.atos.indigo.reposync;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by jose on 23/05/16.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorized {
}
