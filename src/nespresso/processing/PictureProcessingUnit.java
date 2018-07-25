package nespresso.processing;

import lombok.Getter;
import lombok.Setter;
import nespresso.memory.Memory;

public class PictureProcessingUnit implements Runnable {
	
	@Getter @Setter Memory memory;
	@Getter @Setter int[] internalMemory = new int[0x3FFF];
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	public int getPpuCtrl() {
		return memory.getByte(0x2000);
	}
	
	public int getPpuMask() {
		return memory.getByte(0x2001);
	}
	
	public int getPpuStatus() {
		return memory.getByte(0x2002);
	}
	
	public int getOamAddr() {
		return memory.getByte(0x2003);
	}
	
	public int getOamData() {
		return memory.getByte(0x2004);
	}
	
	public int getPpuScroll() {
		return memory.getByte(0x2005);
	}
	
	public int getPpuAddr() {
		return memory.getByte(0x2006);
	}
	
	public int getPpuData() {
		return memory.getByte(0x2007);
	}
	
	public int getOamDma() {
		return memory.getByte(0x4014);
	}

}
