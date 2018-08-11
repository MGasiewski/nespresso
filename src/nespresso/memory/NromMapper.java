package nespresso.memory;

import java.io.FileInputStream;
import java.io.IOException;

import nespresso.processing.PictureProcessingUnit;

public class NromMapper extends Memory {
	
	private int sixteenKbPrgUnits;
	private int eightKbPrgUnits;
	private int eightKbChrUnits;
	private PictureProcessingUnit ppu;
	
	private NromMapper(String memString) {
		super(memString);
		// TODO Auto-generated constructor stub
	}

	public NromMapper(FileInputStream stream, PictureProcessingUnit ppu) {
		super();
		this.ppu = ppu;
		int i;
		int count = 0;
		try (stream) {
			while ((i = stream.read()) != -1) {
				if(count == 4) {
					sixteenKbPrgUnits = i;
				}else if(count == 5) {
					eightKbChrUnits = i;
				}else if(count == 6) {
					
				}else if(count == 7) {
					
				}else if(count == 8) {
					eightKbPrgUnits = i;
				}else if(count == 9) {
					
				}else if(count > 15 && count < 16384 * sixteenKbPrgUnits + 15) {
					if(sixteenKbPrgUnits == 1) {
						int address0 = count - 16 + 0x8000;
						int address1 = count - 16 + 0xC000;
						setByte(address0, i);
						setByte(address1, i);
					}else{
						int address = count - 16 + 0x8000;
						setByte(address, i);
					}					
				}else if(16384 * sixteenKbPrgUnits + 15 < count) {
					int ppuIndex = count - (16384 * sixteenKbPrgUnits + 16 );
					ppu.setByte(ppuIndex, i);
				}
				count += 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean inRom(int num) {
		return num < 8192 * eightKbChrUnits;
	}

}
