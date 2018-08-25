package nespresso.memory;

import nespresso.processing.PpuHandler;

public class MMC1Mapper extends Memory {

	public MMC1Mapper(String memString) {
		String[] bytes = memString.split(" ");
		//TODO map cartridge
		ppuHandler = new PpuHandler();
	}
	
	
}
