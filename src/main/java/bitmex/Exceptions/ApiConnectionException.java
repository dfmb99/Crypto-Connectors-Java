package bitmex.Exceptions;

public class ApiConnectionException extends Exception{

    public ApiConnectionException() { super("Connection error while trying an api call.");}
}