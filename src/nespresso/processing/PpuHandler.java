package nespresso.processing;

public class PpuHandler {

	public int read(int address) {
		int num = address & 0xF;
		switch (num) {
		case 2:
			PictureProcessingUnit.getInstance().resetAddressLatch();
			int value = PictureProcessingUnit.getInstance().getStatus();
			//TODO clear vblank?
			return value;
		case 4:
			return PictureProcessingUnit.getInstance().getOamdata();
		case 7:
			//TODO correct implementation
			return PictureProcessingUnit.getInstance().getData();
		}
		throw new UnsupportedOperationException("address not supported for read");
	}

	public void write(int address, int data) {
		int num = address & 0xFF;
		switch (num) {
		case 0:
			PictureProcessingUnit.getInstance().setCtrl(data);
			PictureProcessingUnit.getInstance().updateTempOnCtrlWrite(data);
			//TODO NMI
			break;
		case 0x1:
			PictureProcessingUnit.getInstance().setMask(data);
			//TODO
			break;
		case 0x3:
			PictureProcessingUnit.getInstance().setOamaddr(data);
			//TODO correct implementation
			break;
		case 0x4:
			PictureProcessingUnit.getInstance().setOamdata(data);
			//TODO
			break;
		case 0x5:
			PictureProcessingUnit.getInstance().setScroll(data);
			//TODO
			break;
		case 0x6:
			PictureProcessingUnit.getInstance().writeAddress(data);
			break;
		case 0x7:
			PictureProcessingUnit.getInstance().writeToVram(data);
			break;
		case 0x14:
			PictureProcessingUnit.getInstance().writeSprites(data);
			//TODO this costs a high number of cycles, so implement that
			break;
		}
		
	}
}
