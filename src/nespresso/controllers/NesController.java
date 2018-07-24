package nespresso.controllers;

import nespresso.memory.Memory;
import nespresso.processing.Clock;
import nespresso.processing.Processor;

public class NesController {
	private Clock clock;
	private Processor processor;
	private Memory memory;

	public void initializeNes() {
		clock = new Clock();
		processor = new Processor();
		memory = new Memory("a2 08 ca 8e 00 02 e0 03 d0 f8 8e 01 02 00");
		processor.setMemory(memory);
		processor.run();
	}
	
	public static void main(String[] args) {
		NesController controller = new NesController();
		controller.initializeNes();
	}

	public void runNes() {

	}
}
