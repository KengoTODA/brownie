package jp.skypencil.brownie;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.TypeQualifierValidator;
import javax.annotation.meta.When;

/**
 * A type qualifier for ID of file.
 * 
 * {@link FileId} should be version 1 UUID, to support distributing servers onto multiple hosts.
 */
@Documented
@TypeQualifier(applicableTo = UUID.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface FileId {
    When when() default When.ALWAYS;

    static class Checker implements TypeQualifierValidator<FileId> {
        @Override
        public When forConstantValue(FileId annotation, Object value) {
            if (value instanceof UUID) {
                UUID uuid = (UUID) value;
                if (uuid.version() == 1) {
                    return When.MAYBE;
                }
            }
            return When.NEVER;
        }
    }
}
