package nespresso.memory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

import nespresso.processing.PictureProcessingUnit;
import nespresso.processing.PpuHandler;

public class MMC1Mapper extends Memory {
	int numOfPrgBanks = 0;
	int numOfChrBanks = 0;
	int eightKbPrgBanks = 0;
	int sixteenKb = 16384;
	int fourKb = 4096;
	int loadRegister = 0x10;
	int control = 0;
	int chrBank0 = 0;
	int chrBank1 = 0;
	int prgBankSelection = 0;
	int prgMode = 0;
	int chrMode = 0;
	int[][] prgBanks;
	int[][] chrBanks;
	PictureProcessingUnit ppu;

	public MMC1Mapper(FileInputStream stream, PictureProcessingUnit ppu) {
		int aByte;
		int count = 0;
		int prgBankLimit = 0;
		int chrLimit = 0;
		this.ppu = ppu;
		try {
			while ((aByte = stream.read()) != -1) {
				if (count == 4) {
					numOfPrgBanks = aByte;
					prgBanks = new int[numOfPrgBanks][sixteenKb];
					prgBankLimit = 15 + (numOfPrgBanks * sixteenKb);
				} else if (count == 5) {
					numOfChrBanks = aByte;
					chrBanks = new int[numOfChrBanks][fourKb];
					chrLimit = prgBankLimit + numOfChrBanks * fourKb;
				} else if (count == 8) {
					eightKbPrgBanks = aByte;
				} else if (count > 15 && count < prgBankLimit) {
					int rawIndex = count - 16;
					int bank = rawIndex / sixteenKb;
					int offset = rawIndex % sixteenKb;
					prgBanks[bank][offset] = aByte;
				} else if (count >= prgBankLimit && count < chrLimit) {
					int rawIndex = count - prgBankLimit;
					int bank = rawIndex / fourKb;
					int offset = rawIndex % fourKb;
					chrBanks[bank][offset] = aByte;
				} else if (count >= chrLimit) {
					ppu.getInternalMemory()[count - chrLimit] = aByte;
				}
				count += 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.arraycopy(chrBanks[0], 0, ppu.getInternalMemory(), 0, chrBanks[0].length);
		if(numOfChrBanks > 1) {
			System.arraycopy(chrBanks[1], 0, ppu.getInternalMemory(), 0x1000, chrBanks[1].length);
		}else {
			System.arraycopy(chrBanks[0], 0, ppu.getInternalMemory(), 0x1000, chrBanks.length);
		}
	}

	@Override
	public int getByte(int address) {
		if (address < 0x8000) {
			return super.getByte(address);
		}else {
			super.getByte(address);
			if((control & 0xC) == 0xC) {
				return readMode3(address);
			}else if((control & 0xC) == 0x8) {
				return readMode2(address);
			}else {
				return readMode1(address);
			}
		}
	}
	
	public int readMode1(int address) {
		int bank = prgBankSelection & 0xE;
		if(address < 0xC000) {
			return prgBanks[bank][address - 0x8000];
		}else {
			return prgBanks[bank][address - 0xC000];
		}
	}
	
	public int readMode2(int address) {
		if(address < 0xC000 ) {
			return prgBanks[0][address - 0x8000];
		}else {
			int currentBank  = prgBankSelection & 0xF;
			return prgBanks[currentBank][address - 0xC000];
		}
	}
	
	public int readMode3(int address) {
		if(address >= 0xC000) {
			return prgBanks[numOfPrgBanks-1][address - 0xC000];
		}else {
			int currentBank = prgBankSelection & 0xF;
			return prgBanks[currentBank][address - 0x8000];
		}
	}

	@Override
	public void setByte(int address, int value) {
		if (address < 0x6000) {
			super.setByte(address, value);
		} else if (address >= 0x8000 && address <= 0xFFFF) {
			super.setByte(address, value);
			doLoadRegister(address, value);
		}
	}

	private void setPrgBank(int value) {
		prgBankSelection = value;
	}

	private void setChrBank1(int value) {
		chrBank1 = value;
		int bank = chrBank1 & 0xF;
		System.arraycopy(chrBanks[bank], 0, ppu.getInternalMemory(), 0x1000, chrBanks[bank].length);
	}

	private void setChrBank0(int value) {
		chrBank0 = value;
		int bank = chrBank0 & 0xF;
		System.arraycopy(chrBanks[bank], 0, ppu.getInternalMemory(), 0, chrBanks[bank].length);
	}
	
	private void setControl(int value) {
		control = value;
	}

	private void doLoadRegister(int address, int value) {
		if ((value >> 7) == 1) {
			loadRegister = 0x10;
			setControl(control | 0xC);
		} else {
			boolean full = (loadRegister & 1) == 1;
			loadRegister >>= 1;
			loadRegister |= (value & 1) << 4;
			if (full) {
				int leftNibble = address >> 12;
				if (leftNibble == 0x8 || leftNibble == 0x9) {
					setControl(loadRegister);
				} else if (leftNibble == 0xA || leftNibble == 0xB) {
					setChrBank0(loadRegister);
				} else if (leftNibble == 0xC || leftNibble == 0xD) {
					setChrBank1(loadRegister);
				} else if (leftNibble == 0xE || leftNibble == 0xE) {
					setPrgBank(loadRegister);
				}
				loadRegister = 0x10;
			}
		}
	}
}