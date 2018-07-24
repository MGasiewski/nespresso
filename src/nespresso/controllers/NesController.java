package nespresso.controllers;

import lombok.extern.slf4j.Slf4j;
import nespresso.memory.Memory;
import nespresso.processing.Clock;
import nespresso.processing.Processor;

@Slf4j
public class NesController {
	private Clock clock;
	private Processor processor;
	private Memory memory;

	public void initializeNes() {
		clock = new Clock();
		processor = new Processor();
		memory = new Memory("20 09 00 20 0c 00 20 12 00 a2 00 60 e8 e0 05 d0"
				+ " fb 60 00");
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
