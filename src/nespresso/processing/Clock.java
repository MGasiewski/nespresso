package nespresso.processing;

public interface Clock{
	public void registerCycles(int amount);
	public int getPpuCycle();
}
