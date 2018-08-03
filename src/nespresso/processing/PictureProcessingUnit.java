package nespresso.processing;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.memory.Memory;

@Slf4j
public class PictureProcessingUnit {

	private static PictureProcessingUnit ppu;
	@Getter
	@Setter
	private Processor processor;
	@Getter
	@Setter
	private Memory memory;
	@Getter
	@Setter
	int[] internalMemory = new int[0x4000];
	private int tileShiftRegisterOne;
	private int tileShiftRegisterTwo;
	private int paletteShiftRegisterOne;
	private int paletteShiftRegisterTwo;
	@Getter
	@Setter
	private int ctrl = 0;
	@Getter
	@Setter
	private int mask = 0;
	@Getter
	@Setter
	private int status = 0;
	@Getter
	@Setter
	private int oamdata = 0;
	@Getter
	@Setter
	private int oamaddr = 0;
	@Getter
	@Setter
	private int scroll = 0;
	@Getter
	@Setter
	private int addr = 0;
	@Getter
	@Setter
	private int data = 0;
	private int currPixel = 0;
	private int currLine = -1;
	private boolean lowByte = false;
	private int busLowByte = 0x0;
	private int busHighByte = 0x0;
	private int busData = 0x0;
	private int writeAddress = 0x0;
	private boolean evenFrame = true;

	// Render scanlines -1 through 240
	// each scanline, render pixel 0 through 255
	// each pixel do ...

	public static synchronized PictureProcessingUnit getInstance() {
		if (ppu == null) {
			ppu = new PictureProcessingUnit();
		}
		return ppu;
	}

	private PictureProcessingUnit() {
	}

	public void draw() {
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
				log.info("Palette values: {} {} {} {}", internalMemory[0x3F00], internalMemory[0x3F01], internalMemory[0x3F02], internalMemory[0x3F03]);
				log.info("Nametable bytes: {} {} {} {}", internalMemory[0x2000], internalMemory[0x2001], internalMemory[0x2002], internalMemory[0x2003]);
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

	public void writeToVram(int data) {
		internalMemory[writeAddress] = data;
		if ((getCtrl() & 0b00000100) == 0) {
			writeAddress += 1;
		} else {
			writeAddress += 32;
		}
		writeAddress &= 0x3FFF;
	}

	public void writeAddress(int addressByte) {
		if(lowByte) {
			busLowByte = addressByte;
			writeAddress = busHighByte << 8;
			writeAddress += busLowByte;
			writeAddress &= 0x3FFF;
			lowByte = false;
		}else {
			busHighByte = addressByte;		
			lowByte = true;
		}
	}
	
	private void setVblank() {
		int newVal = getStatus() | 0b10000000;
		setStatus(newVal);
		setCtrl(getCtrl() | 0b10000000);
		processor.setNmi(true);
	}

	private void clearVblank() {
		int newVal = getStatus() & 0b01111111;
		setStatus(newVal);
		setCtrl(getCtrl() & 0b01111111);
	}

	private boolean renderingIsOn() {
		return (getMask() & 0x18) == 0x18;
	}

	private boolean inVblank() {
		return (getStatus() >> 7) == 1;
	}

	public void setByte(int location, int value) {
		internalMemory[location] = value;
	}
}
