package nespresso.processing;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.memory.Memory;

@Slf4j
public class PictureProcessingUnit implements Runnable {

	@Getter
	@Setter
	private Clock clock;
	@Getter
	@Setter
	private Memory memory;
	@Getter
	@Setter
	int[] internalMemory = new int[0x3FFF];
	private int tileShiftRegisterOne;
	private int tileShiftRegisterTwo;
	private int paletteShiftRegisterOne;
	private int paletteShiftRegisterTwo;
	private boolean evenFrame = true;

	// Render scanlines -1 through 240
	// each scanline, render pixel 0 through 255
	// each pixel do ...

	public PictureProcessingUnit() {
		/*
		 * setPpuCtrl(0x0); setPpuMask(0x0); setPpuScroll(0x0); setPpuData(0x0);
		 */
	}

	@Override
	public void run() {
		while (true) {
			drawScreen();
			log.info("Screen Drawn {}", System.currentTimeMillis());
			evenFrame = !evenFrame;
		}
	}

	public void drawScreen() {
		for (int i = -1; i < 261; i++) {
			drawLine(i);
		}
	}

	public void drawLine(int lineNum) {
		if (lineNum == -1) {
			drawPrerenderLine();
		} else if (lineNum >= 0 && lineNum < 240) {
			drawVisibleLine(lineNum);
		} else {
			drawPostrenderLine(lineNum);
		}
	}

	private void drawPrerenderLine() {
		for (int i = 0; i <= 340; i++) {
			clock.getPpuCycle();
			if (i == 1) {
				clearVblank();
			}
		}
	}

	private void drawVisibleLine(int lineNum) {
		for (int i = 0; i <= 340; i++) {
			clock.getPpuCycle();
			// TODO
		}
	}

	public void drawPostrenderLine(int lineNum) {
		for (int i = 0; i <= 340; i++) {
			clock.getPpuCycle();
			if (lineNum == 241 && i == 1) {
				setVblank();
			}
			loadCPUData();
			if(getPpuData() > 0) {
				log.info("PPUDATA greater than 0: {}", getPpuData());
			}
		}
	}
	
	public void loadCPUData() {
	}

	public void setVblank() {
		int newVal = getPpuStatus() | 0b10000000;
		setPpuStatus(newVal);
	}

	public void clearVblank() {
		int newVal = getPpuStatus() & 0b01111111;
		setPpuStatus(newVal);
	}

	public void setByte(int location, int value) {
		internalMemory[location] = value;
	}

	public void setPpuCtrl(int value) {
		memory.setByte(0x2000, value);
	}

	public int getPpuCtrl() {
		return memory.getByte(0x2000);
	}

	public void setPpuMask(int value) {
		memory.setByte(0x2001, value);
	}

	public int getPpuMask() {
		return memory.getByte(0x2001);
	}

	public void setPpuStatus(int value) {
		memory.setByte(0x2002, value);
	}

	public int getPpuStatus() {
		return memory.getByte(0x2002);
	}

	public void setOamAddr(int value) {
		memory.setByte(0x2003, value);
	}

	public int getOamAddr() {
		return memory.getByte(0x2003);
	}

	public void setOamData(int value) {
		memory.setByte(0x2004, value);
	}

	public int getOamData() {
		return memory.getByte(0x2004);
	}

	public void setPpuScroll(int value) {
		memory.setByte(0x2005, value);
	}

	public int getPpuScroll() {
		return memory.getByte(0x2005);
	}

	public void setPpuAddr(int value) {
		memory.setByte(0x2006, value);
	}

	public int getPpuAddr() {
		return memory.getByte(0x2006);
	}

	public void setPpuData(int value) {
		memory.setByte(0x2007, value);
	}

	public int getPpuData() {
		return memory.getByte(0x2007);
	}

	public int getOamDma() {
		return memory.getByte(0x4014);
	}

}
