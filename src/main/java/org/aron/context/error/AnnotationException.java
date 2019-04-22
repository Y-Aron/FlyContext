package org.aron.context.error;

public class AnnotationException extends Exception {

    private String message = "Annotations exception error";

    public AnnotationException() { }

    public AnnotationException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
