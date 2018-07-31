package nespresso.memory;

import lombok.extern.slf4j.Slf4j;
import nespresso.controllers.NesController;

@Slf4j
public class Memory {
	private int[] cpuMemory = new int[65536];
	private Object lock = new Object();

	public Memory(String memString) {
		String[] bytes = memString.split(" ");
		for (int i = 0; i < bytes.length; i++) {
			cpuMemory[i] = Integer.parseInt(bytes[i], 16);
		}
	}

	public Memory() {
	}

	public int getByte(int address) {
		synchronized (lock) {
			return cpuMemory[address];
		}
	}

	public void setByte(int address, int value) {
		synchronized (lock) {
			cpuMemory[address] = value;
			if(address==0x2007) {
				log.info("write to 2007");
			}
		}
	}
}
