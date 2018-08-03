package nespresso.processing;

import lombok.Getter;
import lombok.Setter;

public class PpuHandler {

	public int read(int address) {
		int num = address & 0xF;
		switch (num) {
		case 2:
			return PictureProcessingUnit.getInstance().getStatus();
		case 4:
			return PictureProcessingUnit.getInstance().getOamdata();
		case 7:
			//TODO correct implementation
			return PictureProcessingUnit.getInstance().getData();
		}
		throw new UnsupportedOperationException("address not supported for read");
	}

	public void write(int address, int data) {
		int num = address & 0xF;
		switch (num) {
		case 0:
			PictureProcessingUnit.getInstance().setCtrl(data);
			//TODO
			break;
		case 1:
			PictureProcessingUnit.getInstance().setMask(data);
			//TODO
			break;
		case 3:
			PictureProcessingUnit.getInstance().setOamaddr(data);
			//TODO correct implementation
			break;
		case 4:
			PictureProcessingUnit.getInstance().setOamdata(data);
			//TODO
			break;
		case 5:
			PictureProcessingUnit.getInstance().setScroll(data);
			//TODO
			break;
		case 6:
			PictureProcessingUnit.getInstance().writeAddress(data);
			break;
		case 7:
			PictureProcessingUnit.getInstance().writeToVram(data);
			break;
		}
	}
}
