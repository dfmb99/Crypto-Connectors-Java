package bitmex.Exceptions;

public class UnhandledErrorException extends Exception{

    public UnhandledErrorException() { super("Unhandled error after an api call to the server.");}
}
