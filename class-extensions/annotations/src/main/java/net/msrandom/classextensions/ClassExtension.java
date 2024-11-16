package net.msrandom.classextensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker allowing a type to extend the base class {@link ClassExtension#value}.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClassExtension {
    /**
     * Chooses the class that needs to be extended, meaning members annotated with {@link ExtensionInject} will be copied to it.
     *
     * @return The class to be extended.
     */
    Class<?> value();
}
