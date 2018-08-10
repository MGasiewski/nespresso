package nespresso.processing;

import java.util.stream.IntStream;

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
	private int writeAddress = 0x0;
	private boolean evenFrame = true;
	private int attrX = 0, attrY = 0;
	private int vramAddr = 0, attrAddr = 0, vramTempAddr = 0, currAt = 0, nextAt = 0, currNt = 0, nextNt = 0,
			fineXScroll = 0, tileLatch0 = 0, tileLatch1 = 0, tileShiftRegister0 = 0, tileShiftRegister1 = 0,
			paletteShiftRegister0 = 0, paletteShiftRegister1 = 0;
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
			drawPrerenderPixel();
		} else if (currLine >= 0 && currLine < 240 && renderingIsOn()) {
			drawVisiblePixel();
		} else {
			drawPostrenderPixel();
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

	private void drawPrerenderPixel() {
		if (currPixel == 1) {
			setVramAddress();
			clearVblank();
		} else if (currPixel >= 321 && currPixel <= 340) {
			fetch();
		}
	}

	private void drawVisiblePixel() {
		fetch();
	}

	public void drawPostrenderPixel() {
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

	private void setVramAddress() {
		int baseNametable = getCtrl() & 0x3;
		switch (baseNametable) {
		case 0:
			vramAddr = 0x2000;
			attrAddr = 0x23C0;
			break;
		case 1:
			vramAddr = 0x2400;
			attrAddr = 0x27C0;
			break;
		case 2:
			vramAddr = 0x2800;
			attrAddr = 0x2BC0;
			break;
		case 3:
			vramAddr = 0x2C00;
			attrAddr = 0x2FC0;
			break;
		}
	}

	private void fetchHighBgTileByte() {
		tileShiftRegister0 = tileLatch0;
		tileLatch0 = internalMemory[currNt + getBaseNameTableAddr()];
	}

	private void fetchLowBgTileByte() {
		tileShiftRegister1 = tileLatch1;
		tileLatch1 = internalMemory[currNt + getBaseNameTableAddr() + 8];
	}

	private void fetchNtByte() {
		currNt = nextNt;
		nextNt = internalMemory[vramAddr++];
		if(vramAddr % 0x20 == 0 && currLine % 8 != 0) {
			vramAddr -= 0x20;
		}
	}

	private void fetchAtByte() {
		currAt = nextAt;
		nextAt = internalMemory[attrAddr];
	}

	public void writeToVram(int data) {
		if(memory.inRom()) {
			return;
		}
		internalMemory[writeAddress] = data;
		if ((getCtrl() & 0x4) == 0) {
			writeAddress += 1;
		} else {
			writeAddress += 32;
		}
		writeAddress &= 0x3FFF;
	}

	public void writeAddress(int addressByte) {
		if (lowByte) {
			busLowByte = addressByte;
			writeAddress = busHighByte << 8;
			writeAddress += busLowByte;
			writeAddress &= 0x3FFF;
			lowByte = false;
		} else {
			busHighByte = addressByte;
			lowByte = true;
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
		return (getMask() & 0x18) == 0x18;
	}

	private boolean inVblank() {
		return (getStatus() >> 7) == 1;
	}

	public void setByte(int location, int value) {
		internalMemory[location] = value;
	}

	public void resetAddressLatch() {
		writeAddress = 0x00;
	}

	public void writeSprites(int data) {
		IntStream.rangeClosed(0, 0xFF).forEach(i -> primaryOam[i] = memory.getByte((data << 8) + i));
	}

	public int getBaseNameTableAddr() {
		return (0x3 & getCtrl());
	}
}