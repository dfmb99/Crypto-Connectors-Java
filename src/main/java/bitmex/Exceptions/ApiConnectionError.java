package bitmex.Exceptions;

public class ApiConnectionError extends Exception {

    public ApiConnectionError() {
        super("Connection error while attempting an API call to the server");
    }
}
