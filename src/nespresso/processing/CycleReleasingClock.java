package nespresso.processing;

public class CycleReleasingClock implements Clock {

	private int bankedPpuCycles = 0;

	@Override
	public void registerCycles(int cpuCycleAmount) {
		
	}

	@Override
	public int getPpuCycle() {
		while (bankedPpuCycles == 0) {
			try {
				Thread.sleep(0, 1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized(this) {
			bankedPpuCycles -= 1;
		}
		return 0;
	}

}
