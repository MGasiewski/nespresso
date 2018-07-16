public class Memory {
    private int[] cpuMemory = new int[65536];

    public int getByte(int address){
        return cpuMemory[address];
    }

    public void setByte(int address, int value){

    }
}
