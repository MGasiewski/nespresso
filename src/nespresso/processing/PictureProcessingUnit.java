package nespresso.processing;

import java.awt.Canvas;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;
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
	private Canvas canvas;
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
	private Queue<Integer> lowTileBytes = new LinkedList<>();
	private Queue<Integer> highTileBytes = new LinkedList<>();
	private int currentLowTileByte = 0;
	private int currentHighTileByte = 0;
	private int currPixel = 0;
	private int currLine = 0;
	private boolean lowByte = false;
	private boolean evenFrame = true;
	private int vramAddr = 0, vramTempAddr = 0, nextAtByte = 0, currAtByte = 0, currAttributeIndex = 0,
			nextAttributeIndex = 0, ntByte = 0, fineXScroll = 0, paletteShiftRegister0 = 0, paletteShiftRegister1 = 0;
	private boolean firstSecondToggle = false, nmiPrevious = false;
	private BufferedImage image = new BufferedImage(256, 240, BufferedImage.TYPE_INT_RGB);

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
				canvas.getGraphics().drawImage(image, 0, 0, canvas);
			}
		}
	}

	public void updateTempOnCtrlWrite(int data) {
		vramTempAddr = (vramTempAddr & 0xF3FF) | ((data & 0x03) << 10);
	}

	private void drawOnPrerenderLine() {
		if (currPixel == 1) {
			clearVblank();
		}
		if (renderingIsOn()) {
			if (currPixel % 8 == 0 && ((currPixel > 0 && currPixel < 256) || currPixel > 321)) {
				incrementX();
			} else if (currPixel == 256) {
				incrementY();
			} else if (currPixel == 257) {
				horiV();
			} else if (currPixel > 279 && currPixel < 305) {
				vertV();
			}
			if (currPixel >= 321 && currPixel <= 336 && renderingIsOn()) {
				fetch();
			}
		}
	}

	private void drawOnVisibleLine() {
		if (currPixel < 249 || (currPixel > 320 && currPixel < 337)) {
			fetch();
		}
		if (currPixel % 8 == 0 && ((currPixel > 0 && currPixel < 256) || currPixel > 321)) {
			incrementX();
		} else if (currPixel == 256) {
			incrementY();
		} else if (currPixel == 257) {
			horiV();
		}
		if (currPixel < 257 && currPixel != 0) {
			addPixelToBuffer();
		}
	}

	private void addPixelToBuffer() {
		int low = currentLowTileByte >> 7;
		currentLowTileByte <<= 1;
		currentLowTileByte &= 0xFF;
		int high = currentHighTileByte >> 7;
		currentHighTileByte <<= 1;
		currentHighTileByte &= 0xFF;
		int value = (high << 1) | low;
		int color = getColor(currAtByte, currAttributeIndex, value);
		image.setRGB(currPixel - 1, currLine, color);
	}

	private int getColor(int atByte, int atIndex, int value) {
		int paletteNumber = 0;
		switch (atIndex) {
		case 0:
			paletteNumber = atByte & 0x3;
			break;
		case 1:
			paletteNumber = (atByte & 0xC) >> 2;
			break;
		case 2:
			paletteNumber = (atByte & 0x30) >> 4;
			break;
		case 3:
			paletteNumber = (atByte & 0xC0) >> 6;
			break;
		}
		int paletteAddress = 0x3F00 | (paletteNumber << 2) | value;
		return ColorLookup.get(internalMemory[paletteAddress]);
	}

	public void drawOnPostrenderLine() {
		if (currLine == 241 && currPixel == 1) {
			setVblank();
		}
	}

	private void fetch() {
		if (currPixel % 8 == 1) {
			fetchNtByte();
			if (!highTileBytes.isEmpty()) {
				currentHighTileByte = highTileBytes.remove();
			}
			if (!lowTileBytes.isEmpty()) {
				currentLowTileByte = lowTileBytes.remove();
			}
		} else if (currPixel % 8 == 3) {
			fetchAtByte();
		} else if (currPixel % 8 == 5) {
			fetchLowBgTileByte();
		} else if (currPixel % 8 == 7) {
			fetchHighBgTileByte();
		}
	}
	
	public void nmiChange() {
		boolean nmi = nmiOutput() && nmiOccurred();
		if(nmi && !nmiPrevious){
			processor.setNmi(true);
		}
		nmiPrevious = nmi;
	}

	private int getPatternTableHalf() {
		return (getCtrl() & 0x10) == 0x10 ? 0x1000 : 0x0;
	}

	private void fetchHighBgTileByte() {
		int fineY = (vramAddr >> 12) & 0x7;
		int addr = getPatternTableHalf() + ntByte * 16 + fineY;
		highTileBytes.add(internalMemory[addr]);
	}

	private void fetchLowBgTileByte() {
		int fineY = (vramAddr >> 12) & 0x7;
		int addr = getPatternTableHalf() + ntByte * 16 + fineY;
		lowTileBytes.add(internalMemory[addr + 8]);
	}

	private void fetchNtByte() {
		int ntAddr = 0x2000 | (vramAddr & 0x0FFF);
		ntByte = internalMemory[ntAddr];
	}

	private void fetchAtByte() {
		int attrAddr = 0x23C0 | (vramAddr & 0x0C00) | ((vramAddr >> 4) & 0x38) | ((vramAddr >> 2) & 0x07);
		currAtByte = nextAtByte;
		currAttributeIndex = nextAttributeIndex;
		nextAttributeIndex = vramAddr % 2 == 0 ? 0 : 1;
		int coarseY = vramAddr & 0b1111100000;
		coarseY >>= 5;
		nextAttributeIndex += coarseY % 2 == 0 ? 0 : 2;
		nextAtByte = internalMemory[attrAddr];
	}

	public void writeToVram(int data) {
		int address = vramAddr & 0x3FFF;
		if (memory.inRom(address)) {
			return;
		}
		internalMemory[address] = data;
		if (address >= 0x2000 && address < 0x2400) {
			internalMemory[address + 0x400] = data;
		} else if (address >= 0x2800 && address < 0x3000) {
			internalMemory[address + 0x400] = data;
		}
		if (!renderingIsOn() || (currLine > 240 && currLine <= 340)) {
			if ((getCtrl() & 0x4) == 0) {
				vramAddr += 1;
			} else {
				vramAddr += 32;
			}
		}
		vramAddr &= 0x3FFF;
	}

	public void writeAddress(int addressByte) {
		if (lowByte) {
			vramTempAddr &= 0xfff00;
			vramTempAddr |= addressByte;
			vramAddr = vramTempAddr;
			lowByte = false;
		} else {
			vramTempAddr &= 0xc0ff;
			vramTempAddr |= ((addressByte & 0x3f) << 8);
			vramTempAddr &= 0x3fff;
			lowByte = true;
		}
	}

	private void horiV() {
		vramAddr = (vramAddr & 0xFBE0) | 0x0; //(vramTempAddr & 0x041F); TODO another hack to get this working
	}

	private void vertV() {
		vramAddr = 0x2000; // TODO hack to get PPU going on donkey kong(vramAddr & 0x841F) | (vramTempAddr
							// & 0x7BE0);
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
		if ((vramAddr & 0x7000) != 0x7000) {
			vramAddr += 0x1000;
		} else {
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

	protected boolean nmiOccurred() {
		return (getStatus() & 0x80) == 0x80;
	}

	protected boolean nmiOutput() {
		return (getCtrl() & 0x80) == 0x80;
	}

	private void setVblank() {
		int newVal = getStatus() | 0x80;
		setStatus(newVal);
		nmiChange();
	}

	public void clearVblank() {
		int newVal = getStatus() & 0b01111111;
		setStatus(newVal);
		nmiChange();
	}

	private boolean renderingIsOn() {
		return (getMask() & 0x8) == 0x8 || (getMask() & 0x10) == 0x10;
	}

	public void setByte(int location, int value) {
		internalMemory[location] = value;
	}

	public void resetAddressLatch() {
		// vramAddr = 0x00; //TODO this isn't correct
	}

	public void writeSprites(int data) {
		IntStream.rangeClosed(0, 0xFF).forEach(i -> primaryOam[i] = memory.getByte((data << 8) + i));
	}

	public int getBaseNameTableAddr() {
		return (0x3 & getCtrl());
	}
	
	public void writeScroll(int data) {
		if(lowByte) {
			vramTempAddr = (vramTempAddr & 0xFFE0) | (data >> 3);
			fineXScroll = data & 7;
			lowByte = !lowByte;
		}else {
			vramTempAddr = (vramTempAddr & 0x8FFF | ((data & 7) << 12));
			vramTempAddr = (vramTempAddr & 0xFC1F) | ((data & 0xF8) << 2);
			lowByte = !lowByte;
		}
	}

	public void outputNametable() {
		IntStream.rangeClosed(0x2000, 0x2400).forEach(i -> {
			System.out.print(Integer.toHexString(internalMemory[i]) + " ");
			if ((i & 0x1F) == 1F) {
				System.out.println();
			}
			if ((i & 0x3FF) == 0x3FF) {
				System.out.println();
			}
		});
	}
}