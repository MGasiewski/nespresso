package nespresso.processing;

import java.util.concurrent.atomic.AtomicInteger;

public class HandOffClock implements Clock {

	private Object lock = new Object();
	private AtomicInteger ppuCycles = new AtomicInteger(0);
	private AtomicInteger dummy = new AtomicInteger(0);

	@Override
	public void registerCycles(int cpuCycleAmount) {
		ppuCycles.addAndGet(cpuCycleAmount);
		while(ppuCycles.get()>0) {
			dummy.incrementAndGet();
			dummy.incrementAndGet();
			dummy.incrementAndGet();
		}
	}

	@Override
	public int getPpuCycle() {
		while(ppuCycles.get()==0) {
			dummy.decrementAndGet();
		}
		return ppuCycles.decrementAndGet();
	}

}
