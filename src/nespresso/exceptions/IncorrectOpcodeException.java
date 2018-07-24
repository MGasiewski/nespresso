package nespresso.exceptions;

public class IncorrectOpcodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE = "Opcode dispatched to wrong method";
	
	public IncorrectOpcodeException() {
		super(MESSAGE);
	}

}
