package nespresso.exceptions;

public class UnknownOpcodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String MESSAGE = "Processor does not know the the opcode sent to the processor";
	
	public UnknownOpcodeException() {
		super(MESSAGE);
	}
}
