package com.ic720.motorola_project.http.exceptions;

/**
 * Validation exception - input value is incorrect
 */
public abstract class ValidationException extends Exception {

    private String name;

    public String getName() {
        return name;
    }

    public ValidationException(String name) {
        this.name = name;
    }
}
