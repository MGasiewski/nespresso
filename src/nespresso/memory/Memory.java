package nespresso.memory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.controllers.NesController;

@Slf4j
public class Memory {
	private int[] cpuMemory = new int[65536];

	@Getter @Setter private boolean ppuAddrSet = false;
	@Getter @Setter private boolean ppuDataSet = false;
	
	public Memory(String memString) {
		String[] bytes = memString.split(" ");
		for (int i = 0; i < bytes.length; i++) {
			cpuMemory[i] = Integer.parseInt(bytes[i], 16);
		}
	}

	public Memory() {
	}

	public int getByte(int address) {
		return cpuMemory[address];
	}

	public void setByte(int address, int value) {
		cpuMemory[address] = value;
		if(address == 0x2006) {
			ppuAddrSet = true;
		}else if(address == 0x2007) {
			ppuDataSet = true;
		}
	}
}
