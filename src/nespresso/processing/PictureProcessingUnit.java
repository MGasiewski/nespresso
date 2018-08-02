package nespresso.processing;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.memory.Memory;

@Slf4j
public class PictureProcessingUnit {

	@Getter
	@Setter
	private Processor processor;
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
	private int currPixel = 0;
	private int currLine = -1;
	private boolean busHighByteLoaded = false;
	private boolean writeMode = false;
	private int busLowByte = 0x0;
	private int busHighByte = 0x0;
	private int busData = 0x0;
	private int writeAddress = 0x0;
	private boolean evenFrame = true;

	// Render scanlines -1 through 240
	// each scanline, render pixel 0 through 255
	// each pixel do ...

	public PictureProcessingUnit() {
		/*
		 * setPpuCtrl(0x0); setPpuMask(0x0); setPpuScroll(0x0); setPpuData(0x0);
		 */
	}

	public void draw() {
		loadCPUData();
		if (currLine == -1) {
			drawPrerenderLine();
		} else if (currLine >= 0 && currLine < 240 && renderingIsOn()) {
			drawVisibleLine();
		} else {
			drawPostrenderLine();
		}
		currPixel += 1;
		if (currPixel == 341) {
			currPixel = 0;
			currLine += 1;
			if (currLine == 261) {
				currLine = -1;
			}
		}

	}

	private void drawPrerenderLine() {
		if (currPixel == 1) {
			clearVblank();
		}
	}

	private void drawVisibleLine() {
		// TODO
	}

	public void drawPostrenderLine() {
		if (currLine == 241 && currPixel == 1) {
			setVblank();
		}

	}

	private void loadCPUData() {
		if (memory.isPpuAddrSet()) {
			if (!busHighByteLoaded) {
				busHighByte = getPpuAddr();
				busHighByteLoaded = true;
				memory.setPpuAddrSet(false);
			} else {
				busLowByte = getPpuAddr();
				busHighByteLoaded = false;
				memory.setPpuAddrSet(false);
			}
		} else if (memory.isPpuDataSet()) {
			busData = getPpuData();
			memory.setPpuDataSet(false);
			writeToVram();
		}
	}

	private void writeToVram() {
		if (!writeMode) {
			writeAddress = busHighByte << 8;
			writeAddress += busLowByte;
			writeMode = true;
		}
		internalMemory[writeAddress] = busData;
		if ((getPpuCtrl() & 0b00000100) == 0) {
			writeAddress += 1;
		} else {
			writeAddress += 32;
		}
	}

	private void setVblank() {
		int newVal = getPpuStatus() | 0b10000000;
		setPpuStatus(newVal);
		setPpuCtrl(getPpuCtrl() | 0b10000000);
		processor.setNmi(true);
	}

	private void clearVblank() {
		int newVal = getPpuStatus() & 0b01111111;
		setPpuStatus(newVal);
		setPpuCtrl(getPpuCtrl() & 0b01111111);
	}

	private boolean renderingIsOn() {
		return (getPpuMask() & 0x18) == 0x18;
	}

	private boolean inVblank() {
		return (getPpuStatus() >> 7) == 1;
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
