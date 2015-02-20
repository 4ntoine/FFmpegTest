package com.ic720.motorola_project.http.exceptions;

import android.os.Message;

import java.text.MessageFormat;

/**
 * Argument value is invalid
 */
public class InvalidArgumentException extends ValidationException {

    // argument value
    private String value;

    public String getValue() {
        return value;
    }

    private String detailedMessage;

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public InvalidArgumentException(Exception e, String name, String value) {
        this(e.getMessage(), name, value);
    }

    public InvalidArgumentException(String detailedMessage, String name, String value) {
        super(name);

        this.detailedMessage = detailedMessage;
        this.value = value;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format("Invalid argument \"{0}\" value \"{1}\": {2}", getName(), value, detailedMessage);
    }
}
