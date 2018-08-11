package nespresso.processing;

import java.util.stream.IntStream;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.memory.Memory;
import nespresso.ui.Screen;

@Slf4j
public class PictureProcessingUnit {
	
	private static PictureProcessingUnit ppu;
	@Getter
	@Setter
	private Screen screen;
	@Getter
	@Setter
	private Processor processor;
	@Getter
	@Setter
	private Memory memory;
	@Getter
	@Setter
	int[] internalMemory = new int[0x4000];
	int[] primaryOam = new int[0x100];
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
	private int data = 0;
	@Getter
	@Setter
	private int scroll = 0;
	private int currPixel = 0;
	private int currLine = 0;
	private boolean lowByte = false;
	private int busLowByte = 0x0;
	private int busHighByte = 0x0;
	private boolean evenFrame = true;
	private int vramAddr = 0, vramTempAddr = 0, atByte = 0, ntByte = 0, fineXScroll = 0, tileLatch0 = 0, tileLatch1 = 0,
			tileShiftRegister0 = 0, tileShiftRegister1 = 0, paletteShiftRegister0 = 0, paletteShiftRegister1 = 0;
	private boolean firstSecondToggle = false;

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
			drawOnPrerenderLine();
		} else if (currLine >= 0 && currLine < 240 && renderingIsOn()) {
			drawOnVisibleLine();
		} else {
			drawOnPostrenderLine();
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

	private void drawOnPrerenderLine() {
		if (currPixel == 1) {
			clearVblank();
		} else if (currPixel >= 321 && currPixel <= 340) {
			fetch();
			incrementX();
			incrementY();
		}
	}

	private void drawOnVisibleLine() {
		//TODO dummy reads, pixel limitations
		fetch();
		incrementX();
		incrementY();
		outputPixel();
	}

	private void outputPixel() {
		int low = tileLatch0 & 0x1;
		tileLatch0 >>= 1;
		int high = tileLatch1 & 0x1;
		tileLatch1 >>= 1;
		int value = high + low;
		if(value == 0) {
			System.out.print(" ");
		}else {
			System.out.print(value);
		}
		if(currPixel == 256) {
			System.out.println("\\");
		}
		if(currLine == 240) {
			System.out.println("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");
		}
	}

	public void drawOnPostrenderLine() {
		if (currLine == 241 && currPixel == 1) {
			setVblank();
		}
	}

	private void fetch() {
		if (currPixel % 8 == 1) {
			fetchNtByte();
		} else if (currPixel % 8 == 3) {
			fetchAtByte();
		} else if (currPixel % 8 == 5) {
			fetchLowBgTileByte();
		} else if (currPixel % 8 == 7) {
			fetchHighBgTileByte();
		}
	}

	private int getPatternTableHalf() {
		return (getCtrl() & 0x20) << 8;
	}

	private void fetchHighBgTileByte() {
		int addr = getPatternTableHalf() + ntByte * 16;
		tileLatch0 = internalMemory[addr];
	}

	private void fetchLowBgTileByte() {
		int addr = getPatternTableHalf() + ntByte * 16;
		tileLatch1 = internalMemory[addr + 8];
	}

	private void fetchNtByte() {
		int ntAddr = 0x2000 | (vramAddr & 0x0FFF);
		ntByte = internalMemory[ntAddr];
	}

	private void fetchAtByte() {
		int attrAddr = 0x23C0 | (vramAddr & 0x0C00) | ((vramAddr >> 4) & 0x38) | ((vramAddr >> 2) & 0x07);
		atByte = internalMemory[attrAddr];
	}

	public void writeToVram(int data) {
		int address = vramAddr & 0x3FFF;
		if (memory.inRom(address)) {
			return;
		}
		internalMemory[address] = data;
		if ((getCtrl() & 0x4) == 0) {
			vramAddr += 1;
		} else {
			vramAddr += 32;
		}
		vramAddr &= 0x3FFF;
	}

	public void writeAddress(int addressByte) {
		if (lowByte) {
			busLowByte = addressByte;
			vramAddr = busHighByte << 8;
			vramAddr += busLowByte;
			vramAddr &= 0x3FFF;
			lowByte = false;
		} else {
			busHighByte = addressByte;
			lowByte = true;
		}
	}

	private void incrementX() {
		if ((vramAddr & 0x001F) == 31) {
			vramAddr &= 0xFFE0;
			vramAddr ^= 0x0400;
		} else {
			vramAddr += 1;
		}
	}

	private void incrementY() {
		if ((vramAddr & 0x7000) != 0x7000)
			vramAddr += 0x1000;
		else {
			vramAddr &= 0x8FFF;
			int y = (vramAddr & 0x03E0) >> 5;
			if (y == 29) {
				y = 0;
				vramAddr ^= 0x0800;
			} else if (y == 31) {
				y = 0;
			} else {
				y += 1;
			}
			vramAddr = (vramAddr & 0xFC1F) | (y << 5);
		}
	}

	private boolean nmiOccurred() {
		return (getStatus() & 0x80) == 0x80;
	}

	private boolean nmiOutput() {
		return (getCtrl() & 0x80) == 0x80;
	}

	private void setVblank() {
		int newVal = getStatus() | 0x80;
		setStatus(newVal);
		setCtrl(getCtrl() | 0x80);
	}

	private void clearVblank() {
		if (nmiOutput()) {
			processor.setNmi(true);
		}
		int newVal = getStatus() & 0b01111111;
		setStatus(newVal);
	}

	private boolean renderingIsOn() {
		return (getMask() & 0x8) == 0x8 || (getMask() & 0x10) == 0x10;
	}

	private boolean inVblank() {
		return (getStatus() >> 7) == 1;
	}

	public void setByte(int location, int value) {
		internalMemory[location] = value;
	}

	public void resetAddressLatch() {
		vramAddr = 0x00;
	}

	public void writeSprites(int data) {
		IntStream.rangeClosed(0, 0xFF).forEach(i -> primaryOam[i] = memory.getByte((data << 8) + i));
	}

	public int getBaseNameTableAddr() {
		return (0x3 & getCtrl());
	}
}