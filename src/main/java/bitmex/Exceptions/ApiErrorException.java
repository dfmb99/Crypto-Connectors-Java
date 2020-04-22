package bitmex.Exceptions;

public class ApiErrorException extends Exception{

    public ApiErrorException(String err) { super(err);}
}