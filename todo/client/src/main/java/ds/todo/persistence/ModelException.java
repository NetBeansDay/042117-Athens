package ds.todo.persistence;

public class ModelException extends Exception {
    
    /** Creates a new instance of ModeloException */
    public ModelException(String msg, Throwable cause) {
        super(msg, cause);        
    }
    
    public ModelException(String msg) {
        this(msg, null);        
    }
    
}
