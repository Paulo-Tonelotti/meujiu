package tonelotti.dev.meujiu.common.exceptions;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String message){
        super(message);
    }
}
