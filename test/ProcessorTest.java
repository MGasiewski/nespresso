import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProcessorTest {
	Processor processor = new Processor();

	private void init() {
		processor.getMemory().setByte(0x000, 0xFF);
		processor.getMemory().setByte(0x0001, 0x10);
		processor.getMemory().setByte(0x0002, 0x20);
		processor.getMemory().setByte(0x0003, 0xC0);
		processor.getMemory().setByte(0x0005, 0b11110000);
		processor.getMemory().setByte(0x0004, 0);
		processor.getMemory().setByte(0x10, 0b10101010);
	}

	@Test
	public void adcTest() {
		// Tests Immediate and Indirect X
		processor.runInstruction(0x69, 0x10, -1); // ADC #$10
		processor.runInstruction(0x69, 0x5, -1); // ADC #$10
		assertTrue(processor.getAccumulator() == 0x15);
		processor.getMemory().setByte(0x10, 0x10);
		processor.getMemory().setByte(0x11, 0x00);
		processor.runInstruction(0x61, 0x10, -1);
		assertTrue(processor.getAccumulator() == 0x25);
		processor.getMemory().setByte(0x6050, 0xFF);
		processor.runInstruction(0x6D, 0x50, 0x60);
		assertTrue(processor.isCarryFlag());
		assertTrue(processor.getAccumulator() == 0x25);
		processor.runInstruction(0x69, 0x10, -1);
		assertTrue(processor.getAccumulator() == 0x25 + 0x10 + 0x1);
	}

	@Test
	public void andTest() {
		init();
		processor.runInstruction(0x69, 0x10, -1);
		processor.runInstruction(0x25, 0x0, -1);
		assertTrue(processor.getAccumulator() == 0x10);
		processor.runInstruction(0x69, 0b10101010, -1);
		processor.runInstruction(0x29, 0b00001111, -1);
		assertTrue(processor.getAccumulator() == 0b00001010);
		processor.runInstruction(0x2D, 0x04, 0x00);
		assertTrue(processor.getAccumulator() == 0);
	}

	@Test
	public void aslTest() {
		// also tests LDX
		// tests absolute x addressing
		init();
		processor.runInstruction(0xA2, 0x3, -1);
		processor.runInstruction(0x1E, 0x02, 0x00);
		assertTrue(processor.getMemory().getByte(0x5) == 0b11100001);
		processor.runInstruction(0xA2, 0x10, -1);
		processor.runInstruction(0x16, 0x0, -1);
		assertTrue(processor.getMemory().getByte(0x10) == 0b01010101);
	}

	@Test
	public void bccTest() {
		int previous0 = processor.getProgramCounter();
		processor.runInstruction(0x90, 0x50, -1);
		assertTrue(processor.getProgramCounter() == previous0 + 0x50);
		assertTrue(processor.getCurrentCycles() == 3);
		int previous1 = processor.getProgramCounter();
		processor.runInstruction(0x90, 0xFF, -1);
		assertTrue(processor.getProgramCounter() == previous1 + 0xFF);
		assertTrue(processor.getCurrentCycles() == 4);
	}

	@Test
	public void bcsSecClcTest() {
		int previous = processor.getProgramCounter();
		processor.runInstruction(0x38, -1, -1);
		processor.runInstruction(0xB0, 0x1F, -1);
		assertTrue(processor.getProgramCounter() == previous + 0x1F);
		processor.runInstruction(0x18, -1, -1);
		int previous0 = processor.getProgramCounter();
		processor.runInstruction(0xB0, 0x10, -1);
		assertTrue(previous0 == processor.getProgramCounter());
	}

	@Test
	public void beqTest() {
		processor.runInstruction(0xA, 0x0, -1);
		int previous = processor.getProgramCounter();
		processor.runInstruction(0xF0, 0x1F, -1);
		assertTrue(processor.getProgramCounter() == previous + 0x1F);
	}

	@Test
	public void bitTest() {
	}

	@Test
	public void bmiTest() {
		processor.runInstruction(0xA9, 0b11111111, -1); // Load operand 0 into accumulator
		processor.runInstruction(0xC9, 0b1, -1); // Compare accumulator with operand0 which should set neg flag
		int previous = processor.getProgramCounter();
		processor.runInstruction(0x30, 0x10, -1);
		assertTrue(processor.getProgramCounter() == previous + 0x10);
	}

	@Test
	public void bneTest() {
		processor.runInstruction(0xA9, 0x05, 0); // set zero flag by executing LDA 0x05
		int previous0 = processor.getProgramCounter();
		processor.runInstruction(0xD0, 0x10, 0); // BNE 0x10
		assertTrue(processor.getProgramCounter() == previous0 + 0x10);
		int previous1 = processor.getProgramCounter();
		processor.runInstruction(0xA9, 0, 0); // Make sure zero flag is clear by executing LDA 0x00
		processor.runInstruction(0xD0, 0x05, 0);
		assertTrue(processor.getProgramCounter() == previous1);
	}

	@Test
	public void bplTest() {
		processor.runInstruction(0xA9, 0xFF, 0); // LDA #$FF - Set negative flag
		int previous0 = processor.getProgramCounter();
		processor.runInstruction(0x10, 0x15, 0); // BPL 0x15
		assertTrue(previous0 == processor.getProgramCounter());
	}

	@Test
	public void bpcTest() {
		int previous0 = processor.getProgramCounter();
		processor.runInstruction(0x50, 0x10, 0); // BVC 0x10
		assertTrue(previous0 + 0x10 == processor.getProgramCounter());
	}

	@Test
	public void bvsTest() {
		// TODO make overflow test
	}

	@Test
	public void cliSeiTest() {
		processor.runInstruction(0x78, 0, 0); // Set Interrupt disable flag
		assertTrue(processor.isInterruptFlag());
		processor.runInstruction(0x58, 0, 0); // Clear Interrupt disable flag
		assertFalse(processor.isInterruptFlag());
	}

	@Test
	public void clvTest() {
		// TODO clv test
	}

	@Test
	public void cmpTest() {
		processor.runInstruction(0xa9, 0x10, 0); // LDA 0x10
		processor.getMemory().setByte(0x20, 0x10);
		processor.getMemory().setByte(0x21, 0x3);
		processor.runInstruction(0xC5, 0x20, 0); // CMP $0x20
		assertTrue(processor.isZeroFlag());
		processor.runInstruction(0xA2, 0x1, 0); // LDX #0x1
		processor.runInstruction(0xD5, 0x20, 0); // CMP $0x20 + xindex
		assertTrue(processor.isCarryFlag());
	}

	@Test
	public void cpxTest() {
		processor.runInstruction(0xA2, 0x10, 0);
		processor.getMemory().setByte(0x2010, 0x13);
		processor.getMemory().setByte(0x2020, 0x5);
		processor.runInstruction(0xEC, 0x10, 0x20);
		assertFalse(processor.isCarryFlag());
		assertFalse(processor.isZeroFlag());
		processor.runInstruction(0xEC, 0x20, 0x20);
		assertTrue(processor.isCarryFlag());
		assertFalse(processor.isZeroFlag());
		processor.runInstruction(0xE0, 0x10, 0);
		assertTrue(processor.isCarryFlag());
		assertTrue(processor.isZeroFlag());
	}

	@Test
	public void cpyTest() {
		processor.runInstruction(0xC0, 0x0, 0);
		assertTrue(processor.isZeroFlag());
		assertTrue(processor.isCarryFlag());
		processor.getMemory().setByte(0x10, 0x10);
		processor.runInstruction(0xC4, 0x10, 0);
		assertFalse(processor.isZeroFlag());
		assertFalse(processor.isCarryFlag());
		assertTrue(processor.isSignFlag());
	}

	@Test
	public void decTest() {
		processor.getMemory().setByte(0x100, 0x0);
		processor.runInstruction(0xCE, 0x00, 0x01); // Decrement memory address 0x100
		assertTrue(processor.isSignFlag());
		assertTrue(processor.getMemory().getByte(0x100) == 0xFF);
	}

	@Test
	public void dexTest() {
		processor.runInstruction(0xA2, 0x1, 0);
		processor.runInstruction(0xCA, 0, 0);
		assertTrue(processor.isZeroFlag());
		processor.runInstruction(0xCA, 0, 0);
		assertTrue(processor.isSignFlag());
		assertFalse(processor.isZeroFlag());
		assertTrue(processor.getXIndex() == 0xFF);
	}

	@Test
	public void deyTest() {
		processor.runInstruction(0xA0, 0x1, 0);
		processor.runInstruction(0x88, 0, 0);
		assertTrue(processor.isZeroFlag());
		processor.runInstruction(0x88, 0, 0);
		assertTrue(processor.isSignFlag());
		assertFalse(processor.isZeroFlag());
		assertTrue(processor.getYIndex() == 0xFF);
	}

	@Test
	public void eorTest() { //Also tests indirect addressing
		processor.runInstruction(0x49, 0b1111, 0); // EOR A=0 #$0b1111
		assertTrue(processor.getAccumulator() == 0b1111);
		processor.getMemory().setByte(0x1, 0b11110000);
		processor.runInstruction(0x45, 0x01, 0); // EOR A=0b1111 mem addr = 0b11110000
		assertTrue(processor.getAccumulator() == 0b11111111);
		processor.runInstruction(0xA2, 0x5, 0); // LDX 0x5
		processor.getMemory().setByte(0x5, 0b111100); // mem 5 = 0b111100
		processor.runInstruction(0x55, 0x00, 0); // EOR A and mem 0 + xIndex (5)
		assertTrue(processor.getAccumulator() == 0b11000011);
		processor.getMemory().setByte(0x2010, 0b11110000);
		processor.runInstruction(0x4D, 0x10, 0x20); // EOR A and mem 0x2010
		assertTrue(processor.getAccumulator() == 0b00110011);
		processor.getMemory().setByte(0x3005, 0b11111111);
		processor.runInstruction(0x5D, 0x00, 0x30); // EOR A and mem 0x300 + 0x5
		assertTrue(processor.getAccumulator() == 0b11001100);
		processor.getMemory().setByte(0x15, 0b11111111);
		processor.getMemory().setByte(0xA, 0x15);
		processor.getMemory().setByte(0xB, 0x00);
		processor.runInstruction(0x41, 0x5, 0); // EOR (0x5)
		assertTrue(processor.getAccumulator() == 0b00110011);
		processor.runInstruction(0xA0, 0x5, 0); // LDY 0x5
		processor.getMemory().setByte(0x00, 0x10);
		processor.getMemory().setByte(0x01, 0x00);
		processor.runInstruction(0x51, 0x00, 0); // EOR y index (0x00)
		assertTrue(processor.getAccumulator() == 0b11001100);
	}
	
	@Test
	public void jmpTest() {
		
	}
}
