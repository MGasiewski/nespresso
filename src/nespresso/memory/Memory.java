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
	protected PpuHandler ppuHandler;

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
	
	public boolean isPpuRegister(int address) {
		if((address >= 0x2000 && address <= 0x2FFF) || address == 0x4014) {
			return true;
		}
		return false;
	}

	public int getByte(int address) {
		if (isPpuRegister(address)) {
			return ppuHandler.read(address);
		}
		return cpuMemory[address];
	}

	public void setByte(int address, int value) {
		if (isPpuRegister(address)) {
			ppuHandler.write(address, value);
		} else {
			cpuMemory[address] = value;
		}
	}

	public boolean inRom(int num) {
		return false;
	}
}
