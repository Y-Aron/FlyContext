package org.aron.context.error;

public class BeanInstantiationException extends Exception {

    private String message = "bean instantiation exception";

    public BeanInstantiationException() {}

    public BeanInstantiationException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
