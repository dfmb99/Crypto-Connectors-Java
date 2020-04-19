package bitmex.Exceptions;

public class ApiConnectionException extends Exception {

    public ApiConnectionException() {
        super("Connection error while attempting an API call to the server.");
    }
}
