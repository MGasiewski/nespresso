package nespresso.memory;

import lombok.extern.slf4j.Slf4j;
import nespresso.controllers.NesController;

@Slf4j
public class Memory {
	private int[] cpuMemory = new int[65536];

	public Memory(String memString) {
    	String[] bytes = memString.split(" ");
    	for(int i=0; i<bytes.length; i++) {
    		cpuMemory[i] = Integer.parseInt(bytes[i], 16);
    	}
    }

	public int getByte(int address) {
		return cpuMemory[address];
	}

	public void setByte(int address, int value) {
		cpuMemory[address] = value;
	}
}
