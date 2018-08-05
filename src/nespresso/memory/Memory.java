package nespresso.memory;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.controllers.NesController;
import nespresso.processing.PpuHandler;

@Slf4j
public class Memory {
	private int[] cpuMemory = new int[65536];

	private List<Integer> ppuRegisters = List.of(0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007);
	private PpuHandler ppuHandler;

	public Memory(String memString) {
		String[] bytes = memString.split(" ");
		for (int i = 0; i < bytes.length; i++) {
			cpuMemory[i] = Integer.parseInt(bytes[i], 16);
		}
		ppuHandler = new PpuHandler();
	}

	public Memory() {
		ppuHandler = new PpuHandler();
	}

	public int getByte(int address) {
		if (ppuRegisters.contains(address)) {
			return ppuHandler.read(address);
		}
		return cpuMemory[address];
	}

	public void setByte(int address, int value) {
		if (ppuRegisters.contains(address)) {
			ppuHandler.write(address, value);
		} else {
			cpuMemory[address] = value;
		}
	}
}
