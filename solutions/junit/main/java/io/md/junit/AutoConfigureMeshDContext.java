package io.md.junit;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoConfigureMeshDContext {

  String meshDHost() default  "app.meshdynamics.io";

  int meshDPort() default 443;

  HTTPScheme uriScheme() default HTTPScheme.HTTPS;

  String authToken() default "";

  String[] goldenNames() default { "" };

  String customer() default "CubeCorp";

  String app() default "Cube";
}
