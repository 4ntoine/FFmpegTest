package com.ic720.motorola_project.http.exceptions;

import java.text.MessageFormat;

/**
 * Required argument is missing
 */
public class NoArgumentException extends ValidationException {

    public NoArgumentException(String name) {
        super(name);
    }

    @Override
    public String getMessage() {
        return MessageFormat.format("Required argument \"{0}\" is missing", getName());
    }
}
