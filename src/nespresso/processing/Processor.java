package nespresso.processing;

import java.util.ArrayList;
import java.util.List;
import static java.lang.Integer.toHexString;

import java.io.File;
import java.io.PrintStream;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nespresso.exceptions.IncorrectOpcodeException;
import nespresso.exceptions.UnknownOpcodeException;
import nespresso.memory.Memory;

@Slf4j
public class Processor {
	@Getter @Setter private int totalCycles = 0;
	@Getter @Setter private boolean nmi;
	@Getter private int accumulator = 0;
	@Getter private int xIndex = 0;
	@Getter private int yIndex = 0;
	@Getter private int stackPointer = 0xFF;
	@Getter private int programCounter = 0;
	@Getter private boolean carryFlag = false;
	@Getter private boolean zeroFlag = false;
	@Getter private boolean interruptFlag = false;
	@Getter private boolean decimalModeFlag = true;
	@Getter private boolean softwareInterruptFlag = true;
	@Getter private boolean overflowFlag = false;
	@Getter private boolean signFlag = false;
	private static int ENDIAN_MULT = 256;
	@Getter @Setter private Memory memory;
	@Getter @Setter private int currentCycles = 0;
	@Getter @Setter private PictureProcessingUnit ppu;
	private List<String> operationCache = new ArrayList<>();
	public int cpuCycles = 0;

	public Processor(Memory memory) {
		setMemory(memory);
		//for (int i = 0; i < 0x800; ++i) {
		//	memory.setByte(i, 0xFF);
		//}
		memory.setByte(0x0008, 0xF7);
		memory.setByte(0x0009, 0xEF);
		memory.setByte(0x000A, 0xDF);
		memory.setByte(0x000B, 0xBF);

		for (int i = 0x4000; i <= 0x400F; ++i) {
			memory.setByte(i, 0x00);
		}

		memory.setByte(0x4015, 0);
		memory.setByte(0x4017, 0);

		accumulator = 0;
		xIndex = 0;
		yIndex = 0;
		stackPointer = 0xFD;
		programCounter = indirect(0xFC, 0xFF);
		// programCounter = 0xC000;

	}

	public void outputState() {
		System.out.println("A:" + toHexString(accumulator).toUpperCase() + " X:" + toHexString(xIndex).toUpperCase()
				+ " Y:" + toHexString(yIndex).toUpperCase() + " S:" + toHexString(stackPointer).toUpperCase() + " P:"
				+ getAlphaStatus() + " $" + toHexString(programCounter));
	}

	private String getAlphaStatus() {
		StringBuilder status = new StringBuilder(10);
		status.append(signFlag ? "N" : "n");
		status.append(overflowFlag ? "V" : "v");
		status.append("U");
		status.append(softwareInterruptFlag ? "B" : "b");
		status.append(decimalModeFlag ? "D" : "d");
		status.append(interruptFlag ? "I" : "i");
		status.append(zeroFlag ? "Z" : "z");
		status.append(carryFlag ? "C" : "c");
		return status.toString();
	}

	public void outputCache() {
		System.out.println(operationCache);
	}

	public void cacheOperation(int opcode) {
		if (operationCache.size() > 10) {
			operationCache.remove(0);
		}
		operationCache.add(Integer.toHexString(opcode) + " PC: " + Integer.toHexString(programCounter));
	}

	public void cacheOperation(int opcode, int operand) {
		if (operationCache.size() > 10) {
			operationCache.remove(0);
		}
		operationCache.add(Integer.toHexString(opcode) + " " + Integer.toHexString(operand) + " PC: "
				+ Integer.toHexString(programCounter));
	}

	public void cacheOperation(int opcode, int operand0, int operand1) {
		if (operationCache.size() > 10) {
			operationCache.remove(0);
		}
		operationCache.add(Integer.toHexString(opcode) + " " + Integer.toHexString(operand0) + " "
				+ Integer.toHexString(operand1) + " PC: " + Integer.toHexString(programCounter));
	}

	public void runInstruction(int opcode, int operand0, int operand1)
			throws UnknownOpcodeException, IncorrectOpcodeException {
		switch (opcode) {
		// ASL - Arithmetic Shift Left
		case 0x0A: case 0x06: case 0x16: case 0x0E: case 0x1E:
			arithmeticShiftLeft(opcode, operand0, operand1);
			break;
		// BCC - Branch if carry clear
		case 0x90:
			branch(!carryFlag, operand0);
			break;
		// BCS - Branch if carry set
		case 0xB0:
			branch(carryFlag, operand0);
			break;
		// BEQ - Branch if equal
		case 0xF0:
			branch(zeroFlag, operand0);
			break;
		// BMI - Branch if minus
		case 0x30:
			branch(signFlag, operand0);
			break;
		// BNE - Branch if not equal
		case 0xD0:
			branch(!zeroFlag, operand0);
			break;
		// BPL - Branch if positive
		case 0x10:
			branch(!signFlag, operand0);
			break;
		// BVC - Branch if Overflow Clear
		case 0x50:
			branch(!overflowFlag, operand0);
			break;
		// BVS - Branch if Overflow Set
		case 0x70:
			branch(overflowFlag, operand0);
			break;
		// JSR - Jump to subroutine
		case 0x20:
			jumpToSubroutine(operand0, operand1);
			break;
		// JMP
		case 0x4C:
			programCounter = convertOperandsToAddress(operand0, operand1);
			currentCycles = 3;
			break;
		case 0x6C:
			programCounter = indirect(operand0, operand1);
			currentCycles = 5;
			break;
		// LDA COMMANDS
		case 0xA9: case 0xA5: case 0xB5: case 0xAD: case 0xBD: 
		case 0xB9: case 0xA1: case 0xB1:
			loadIntoAccumulator(opcode, operand0, operand1);
			break;
		// LDX COMMANDS
		case 0xA2: case 0xA6: case 0xB6: case 0xAE: case 0xBE:
			loadIntoX(opcode, operand0, operand1);
			break;
		// LDY COMMANDS
		case 0xA0: case 0xA4: case 0xB4: case 0xAC: case 0xBC:
			loadIntoY(opcode, operand0, operand1);
			break;
		// AND COMMANDS
		case 0x29: case 0x25: case 0x35: case 0x2D: case 0x3D: 
		case 0x39: case 0x21: case 0x31:
			andWithAccumulator(opcode, operand0, operand1);
			break;
		// STA COMMANDS
		case 0x85: case 0x95: case 0x8D: case 0x9D: case 0x99:
		case 0x81: case 0x91:
			storeAccumulator(opcode, operand0, operand1);
			break;
		// STX commands
		case 0x86: case 0x96: case 0x8E:
			storeX(opcode, operand0, operand1);
			break;
		// STY commands
		case 0x84: case 0x94: case 0x8C:
			storeY(opcode, operand0, operand1);
			break;
		// TAX Transfer accumulator to X
		case 0xAA:
			xIndex = accumulator;
			currentCycles = 2;
			zeroFlag = xIndex == 0;
			signFlag = xIndex >> 7 == 1;
			break;
		// TAY
		case 0xA8:
			yIndex = accumulator;
			currentCycles = 2;
			zeroFlag = yIndex == 0;
			signFlag = yIndex >> 7 == 1;
			break;
		// TSX
		case 0xBA:
			xIndex = stackPointer;
			currentCycles = 2;
			zeroFlag = xIndex == 0;
			signFlag = xIndex >> 7 == 1;
			break;
		// TXA
		case 0x8A:
			accumulator = xIndex;
			currentCycles = 2;
			zeroFlag = accumulator == 0;
			signFlag = accumulator >> 7 == 1;
			break;
		// TXS
		case 0x9A:
			stackPointer = xIndex;
			currentCycles = 2;
			break;
		// TYA
		case 0x98:
			accumulator = yIndex;
			currentCycles = 2;
			zeroFlag = accumulator == 0;
			signFlag = accumulator >> 7 == 1;
			break;
		// ADC Add with carry
		case 0x69: case 0x65: case 0x75: case 0x6D: case 0x7D:
		case 0x79: case 0x61: case 0x71:
			addWithCarry(opcode, operand0, operand1);
			break;
		// BIT - Bit test
		case 0x24: case 0x2C:
			bitTest(opcode, operand0, operand1);
			break;
		// BRK - Break. Store the program counter and processor state on the stack and
		// force an interrupt
		case 0x00:
			brk();
			break;
		// CMP - Compare with accumulator
		case 0xC9: case 0xC5: case 0xD5: case 0xCD: case 0xDD:
		case 0xD9: case 0xC1: case 0xD1:
			compareWithAccumulator(opcode, operand0, operand1);
			break;
		// CPX - Compare with X
		case 0xE0: case 0xE4: case 0xEC:
			compareWithX(opcode, operand0, operand1);
			break;
		// CPY - Compare with Y
		case 0xC0: case 0xC4: case 0xCC:
			compareWithY(opcode, operand0, operand1);
			break;
		// DEC - Decrement memory
		case 0xC6: case 0xD6: case 0xCE: case 0xDE:
			decrementMemory(opcode, operand0, operand1);
			break;
		// DEX - decrement X
		case 0xCA:
			xIndex -= 1;
			zeroFlag = xIndex == 0;
			xIndex = makeConversionIfNecessary(xIndex);
			signFlag = xIndex >> 7 == 1;
			break;
		// DEY - decrement Y
		case 0x88:
			yIndex -= 1;
			zeroFlag = yIndex == 0;
			yIndex = makeConversionIfNecessary(yIndex);
			signFlag = yIndex >> 7 == 1;
			break;
		// EOR - Exclusive Or
		case 0x49: case 0x45: case 0x55: case 0x4D: case 0x5D:
		case 0x59: case 0x41: case 0x51:
			xorAccumulator(opcode, operand0, operand1);
			break;
		// CLC - Clear Carry
		case 0x18:
			carryFlag = false;
			currentCycles = 2;
			break;
		// SEC - Set Carry
		case 0x38:
			carryFlag = true;
			currentCycles = 2;
			break;
		// CLI - Clear Interrupt
		case 0x58:
			interruptFlag = false;
			currentCycles = 2;
			break;
		// SEI - Set Interrupt
		case 0x78:
			interruptFlag = true;
			currentCycles = 2;
			break;
		// CLV - Clear Overflow
		case 0xB8:
			overflowFlag = false;
			currentCycles = 2;
			break;
		// CLD - Clear Decimal Mode
		case 0xD8:
			decimalModeFlag = false;
			currentCycles = 2;
			break;
		// SED - Set Decimal Mode
		case 0xF8:
			decimalModeFlag = true;
			currentCycles = 2;
			break;
		// INC - Increment Memory
		case 0xE6: case 0xF6: case 0xEE: case 0xFE:
			incrementMemory(opcode, operand0, operand1);
			break;
		// INX - Increment x index
		case 0xE8:
			xIndex = (xIndex + 1) % 0x100;
			currentCycles = 2;
			zeroFlag = xIndex == 0;
			signFlag = xIndex >> 7 == 1;
			break;
		// INY - Increment y index
		case 0xC8:
			yIndex = (yIndex + 1) % 0x100;
			currentCycles = 2;
			zeroFlag = yIndex == 0;
			signFlag = yIndex >> 7 == 1;
			break;
		// NOP - no operation
		case 0xEA: case 0x1A: case 0x3A: case 0x5A: case 0x7A:
		case 0xDA: case 0xFA: case 0x80: case 0x82: case 0x89:
		case 0xC2: case 0xE2:
			currentCycles = 2; //TODO 0x80, 0x82, 0x89, 0xC2, 0xE2 
							   //likely take more than two cycles
			break;
		// LSR - Logical Shift Right
		case 0x4A: case 0x46: case 0x56: case 0x4E: case 0x5E:
			logicalShiftRight(opcode, operand0, operand1);
			break;
		// SBC - Subtract with carry
		case 0xE9: case 0xE5: case 0xF5: case 0xED: case 0xFD:
		case 0xF9: case 0xE1: case 0xF1: case 0xEB:
			subtractWithCarry(opcode, operand0, operand1);
			break;
		// ORA - Logical or
		case 0x09: case 0x05: case 0x15: case 0x0D: case 0x1D:
		case 0x19: case 0x01: case 0x11:
			logicalOr(opcode, operand0, operand1);
			break;
		// ROL - rotate left
		case 0x2A: case 0x26: case 0x36: case 0x2E: case 0x3E:
			rotateLeft(opcode, operand0, operand1);
			break;
		// ROR - rotate right
		case 0x6A: case 0x66: case 0x76: case 0x6E: case 0x7E:
			rotateRight(opcode, operand0, operand1);
			break;
		// PHA - Push Accumulator
		case 0x48:
			push(accumulator);
			currentCycles = 3;
			break;
		// PHP - Push Processor Status
		case 0x08:
			int tempStatus = getStatus();
			tempStatus |= 0x30;
			push(tempStatus);
			currentCycles = 3;
			break;
		// PLA - Pull Accumulator
		case 0x68:
			accumulator = pop();
			zeroFlag = accumulator == 0;
			signFlag = accumulator >> 7 == 1;
			currentCycles = 4;
			break;
		// PLP - Pull Processor Status
		case 0x28:
			int status = pop();
			setProcessorFlags(status);
			currentCycles = 4;
			break;
		// RTI - Return From Interrupt
		case 0x40:
			returnFromInterrupt();
			currentCycles = 6;
			break;
		// RTS - Return from subroutine
		case 0x60:
			returnFromSubroutine();
			currentCycles = 6;
			break;
		case 0xB: case 0x2B: //ANC - an unofficial opcode
			carryFlag = (accumulator & operand0) >> 7 == 1;
			currentCycles = 2;
			break;
		case 0x4B: //ALR - an unofficial opcode
			andWithAccumulator(0x29, operand0, 0);
			logicalShiftRight(0x4A, 0, 0);
			currentCycles = 2;
			break;
		case 0x6B: //ARR - unofficial opcode
			andWithAccumulator(0x29, operand0, 0);
			rotateRight(0x6A, 0, 0);
			currentCycles = 2;
			break;
		case 0xAB: //LAX - unofficial opcode
			accumulator = xIndex = memory.getByte(operand0);
			zeroFlag = accumulator == 0;
			signFlag = (accumulator >> 7) == 1;
			break;
		case 0xCB: //AXS - unofficial opcode
			xIndex = ((accumulator & xIndex) - memory.getByte(operand0)) & 0xFF;
			zeroFlag = xIndex == 0;
			signFlag = (xIndex >> 7) == 1;
			carryFlag = (xIndex >= 0);
			break;
		default:
			throw new UnknownOpcodeException();
		}
	}

	public void jumpToSubroutine(int operand0, int operand1) {
		int returnPoint = programCounter - 1;
		push((returnPoint & 0xFF00) >> 8);
		push(returnPoint & 0xFF);
		currentCycles = 6;
		programCounter = operand0 + operand1 * ENDIAN_MULT;
	}

	private void push(int operand) {
		memory.setByte(stackPointer + 0x100, operand);
		stackPointer -= 1;
		stackPointer &= 0xFF;
	}

	private int pop() {
		stackPointer += 1;
		stackPointer &= 0xFF;
		return memory.getByte(stackPointer + 0x100);
	}

	private void rotateRight(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int original = 0;
		int result = 0;
		switch (opcode) {
		case 0x6A:
			original = accumulator;
			break;
		case 0x66:
			original = memory.getByte(operand0);
			break;
		case 0x76:
			original = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			break;
		case 0x6E:
			original = memory.getByte(convertOperandsToAddress(operand0, operand1));
			break;
		case 0x7E:
			original = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		result = original >> 1;
		result &= 0xFF;
		result |= carryFlag ? 0b10000000 : 0;
		switch (opcode) {
		case 0x6A:
			accumulator = result;
			currentCycles = 2;
			break;
		case 0x66:
			memory.setByte(operand0, result);
			currentCycles = 5;
			break;
		case 0x76:
			memory.setByte(zeroPageWithOffset(operand0, xIndex), result);
			currentCycles = 6;
			break;
		case 0x6E:
			memory.setByte(convertOperandsToAddress(operand0, operand1), result);
			currentCycles = 6;
			break;
		case 0x7E:
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), result);
			currentCycles = 7;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = result == 0;
		carryFlag = (original & 0b00000001) == 1;
		signFlag = result >> 7 == 1;
	}

	private void rotateLeft(int opcode, int operand0, int operand1) {
		int original = 0;
		int result = 0;
		switch (opcode) {
		case 0x2A:
			original = accumulator;
			break;
		case 0x26:
			original = memory.getByte(operand0);
			break;
		case 0x36:
			original = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			break;
		case 0x2E:
			original = memory.getByte(convertOperandsToAddress(operand0, operand1));
			break;
		case 0x3E:
			original = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			break;
		default:
			break;
		}
		result = original << 1;
		result &= 0xFF;
		result += carryFlag ? 1 : 0;
		switch (opcode) {
		case 0x2A:
			accumulator = result;
			currentCycles = 2;
			break;
		case 0x26:
			memory.setByte(operand0, result);
			currentCycles = 5;
			break;
		case 0x36:
			memory.setByte(zeroPageWithOffset(operand0, xIndex), result);
			currentCycles = 6;
			break;
		case 0x2E:
			memory.setByte(convertOperandsToAddress(operand0, operand1), result);
			currentCycles = 6;
			break;
		case 0x3E:
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), result);
			currentCycles = 7;
			break;
		default:
			break;
		}
		zeroFlag = result == 0;
		carryFlag = original >> 7 == 1;
		signFlag = result >> 7 == 1;
	}

	private void returnFromSubroutine() {
		int lowByte = pop();
		int highByte = pop();
		programCounter = highByte * ENDIAN_MULT + lowByte;
		programCounter += 1;
	}

	private void returnFromInterrupt() {
		int status = pop();
		setProcessorFlags(status);
		int lowByte = pop();
		int highByte = pop();
		programCounter = highByte * ENDIAN_MULT + lowByte;
	}

	private void setProcessorFlags(int status) {
		carryFlag = status % 2 == 1;
		status /= 2;
		zeroFlag = status % 2 == 1;
		status /= 2;
		interruptFlag = status % 2 == 1;
		status /= 2;
		decimalModeFlag = status % 2 == 1;
		status /= 2;
		softwareInterruptFlag = status % 2 == 1;
		status /= 2;
		status /= 2;
		overflowFlag = status % 2 == 1;
		status /= 2;
		signFlag = status % 2 == 1;
	}

	private void logicalOr(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		switch (opcode) {
		case 0x09:
			accumulator |= operand0;
			currentCycles = 2;
			break;
		case 0x05:
			accumulator |= memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0x15:
			accumulator |= memory.getByte(zeroPageWithOffset(operand0, xIndex));
			currentCycles = 4;
			break;
		case 0x0D:
			accumulator |= memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0x1D:
			accumulator |= memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
				currentCycles += 1;
			}
			break;
		case 0x19:
			accumulator |= memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
				currentCycles += 1;
			}
			break;
		case 0x01:
			accumulator |= memory.getByte(indirectX(operand0));
			break;
		case 0x11:
			accumulator |= memory.getByte(indirectY(operand0));
			if (isPageBoundaryCrossed(convertOperandsToAddress(memory.getByte(operand0), memory.getByte(operand0 + 1)),
					yIndex)) {
				currentCycles += 1;
			}
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = accumulator == 0;
		signFlag = accumulator >> 7 == 1;
	}

	private void brk() {
		programCounter++;
		int highByte = programCounter >> 8;
		int lowByte = programCounter % 0x100;
		push(highByte);
		push(lowByte);
		// does Processor status change?
		push(getStatus() | 0x30);
		programCounter = 0;
		programCounter = memory.getByte(0xFFFF) * ENDIAN_MULT;
		programCounter += memory.getByte(0xFFFE);
		softwareInterruptFlag = true;
		currentCycles = 7;
	}

	private void bitTest(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int result = 0;
		int mem = 0;
		switch (opcode) {
		case 0x24:
			mem = memory.getByte(operand0);
			result = accumulator & mem;
			currentCycles = 3;
			break;
		case 0x2C:
			mem = memory.getByte(convertOperandsToAddress(operand0, operand1));
			result = accumulator & mem;
			currentCycles = 4;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = result == 0;
		overflowFlag = (mem & 0x40) == 0x40;
		signFlag = mem >> 7 == 1;
	}

	private int getStatus() {
		int status = 0;
		if (signFlag) {
			status += 0b10000000;
		}
		if (overflowFlag) {
			status += 0b01000000;
		}
		status += 0b00100000;
		if (softwareInterruptFlag) {
			status += 0b00010000;
		}
		if (decimalModeFlag) {
			status += 0b00001000;
		}
		if (interruptFlag) {
			status += 0b00000100;
		}
		if (zeroFlag) {
			status += 0b00000010;
		}
		if (carryFlag) {
			status += 0b00000001;
		}
		return status;
	}

	private void subtractWithCarry(int opcode, int operand0, int operand1) {
		int subtrahend = 0;
		int sbcCycles = 0;
		switch (opcode) {
		case 0xE9:
			subtrahend = operand0;
			sbcCycles = 2;
			break;
		case 0xE5:
			subtrahend = memory.getByte(operand0);
			sbcCycles = 3;
			break;
		case 0xF5:
			subtrahend = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			break;
		case 0xED:
			subtrahend = memory.getByte(convertOperandsToAddress(operand0, operand1));
			sbcCycles = 4;
			break;
		case 0xFD:
			subtrahend = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			sbcCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
				currentCycles += 1;
			}
			break;
		case 0xF9:
			subtrahend = memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			sbcCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
				sbcCycles += 1;
			}
			break;
		case 0xE1:
			subtrahend = memory.getByte(indirectX(operand0));
			sbcCycles = 6;
			break;
		case 0xF1:
			subtrahend = memory.getByte(indirectY(operand0));
			sbcCycles = 5;
			if (isPageBoundaryCrossed(convertOperandsToAddress(memory.getByte(operand0), memory.getByte(operand1 + 1)),
					yIndex)) {
				sbcCycles += 1;
			}
		default:
			break;
		}
		addWithCarry(0x69, (~subtrahend) & 0xFF, 0);
		currentCycles = sbcCycles;
		accumulator &= 0xFF;
	}

	private void logicalShiftRight(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int previous = 0;
		int result = 0;
		switch (opcode) {
		case 0x4A:
			previous = accumulator;
			result = previous >> 1;
			accumulator = result;
			currentCycles = 2;
			break;
		case 0x46:
			previous = memory.getByte(operand0);
			result = previous >> 1;
			memory.setByte(operand0, result);
			currentCycles = 5;
			break;
		case 0x56:
			previous = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			result = previous >> 1;
			memory.setByte(zeroPageWithOffset(operand0, xIndex), result);
			currentCycles = 6;
			break;
		case 0x4E:
			previous = memory.getByte(convertOperandsToAddress(operand0, operand1));
			result = previous >> 1;
			memory.setByte(convertOperandsToAddress(operand0, operand1), result);
			currentCycles = 6;
			break;
		case 0x5E:
			previous = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			result = previous >> 1;
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), result);
			currentCycles = 7;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		if (previous % 2 == 0) {
			carryFlag = false;
		} else {
			carryFlag = true;
		}
		zeroFlag = result == 0;
		signFlag = result >> 7 == 1;
	}

	private void xorAccumulator(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		switch (opcode) {
		case 0x49:
			accumulator = accumulator ^ operand0;
			currentCycles = 2;
			break;
		case 0x45:
			accumulator = accumulator ^ memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0x55:
			accumulator = accumulator ^ memory.getByte(zeroPageWithOffset(operand0, xIndex));
			currentCycles = 4;
			break;
		case 0x4D:
			accumulator = accumulator ^ memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0x5D:
			accumulator = accumulator ^ memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			currentCycles = isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex) ? 5 : 4;
			break;
		case 0x59:
			accumulator = accumulator ^ memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			currentCycles = isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex) ? 5 : 4;
			break;
		case 0x41:
			accumulator = accumulator ^ memory.getByte(indirectX(operand0));
			currentCycles = 6;
			break;
		case 0x51:
			accumulator = accumulator ^ memory.getByte(indirectY(operand0));
			currentCycles = isPageBoundaryCrossed(
					convertOperandsToAddress(memory.getByte(operand0), memory.getByte(operand0 + 1)), yIndex) ? 6 : 5;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = accumulator == 0;
		signFlag = accumulator >> 7 == 1;
	}

	private void decrementMemory(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int result = 0;
		switch (opcode) {
		case 0xC6:
			result = memory.getByte(operand0) - 1;
			result = makeConversionIfNecessary(result);
			currentCycles = 5;
			memory.setByte(operand0, result);
			break;
		case 0xD6:
			result = memory.getByte(zeroPageWithOffset(operand0, xIndex)) - 1;
			result = makeConversionIfNecessary(result);
			currentCycles = 6;
			memory.setByte(zeroPageWithOffset(operand0, xIndex), result);
			break;
		case 0xCE:
			result = memory.getByte(convertOperandsToAddress(operand0, operand1)) - 1;
			result = makeConversionIfNecessary(result);
			currentCycles = 6;
			memory.setByte(convertOperandsToAddress(operand0, operand1), result);
			break;
		case 0xDE:
			result = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex)) - 1;
			result = makeConversionIfNecessary(result);
			currentCycles = 7;
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), result);
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = result == 0;
		signFlag = result >> 7 == 1;
	}

	public int makeConversionIfNecessary(int result) {
		if (result < 0) {
			return 0xFF + result + 1;
		} else {
			return result;
		}
	}

	private void arithmeticShiftLeft(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int beginValue = 0;
		int endValue = 0;
		switch (opcode) {
		case 0x0A:
			beginValue = accumulator;
			endValue = (beginValue << 1) % 0x100;
			accumulator = endValue;
			break;
		case 0x06:
			beginValue = memory.getByte(operand0);
			endValue = (beginValue << 1) % 0x100;
			memory.setByte(operand0, endValue);
			break;
		case 0x16:
			beginValue = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			endValue = (beginValue << 1) % 0x100;
			memory.setByte(zeroPageWithOffset(operand0, xIndex), endValue);
			break;
		case 0x0E:
			beginValue = memory.getByte(convertOperandsToAddress(operand0, operand1));
			endValue = (beginValue << 1) % 0x100;
			memory.setByte(convertOperandsToAddress(operand0, operand1), endValue);
			break;
		case 0x1E:
			beginValue = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			endValue = (beginValue << 1) % 0x100;
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), endValue);
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		if (beginValue >> 7 == 1) {
			carryFlag = true;
		} else {
			carryFlag = false; // Not explicitly documented
		}
		zeroFlag = endValue == 0;
		signFlag = endValue >> 7 == 1;
	}

	private void branch(boolean condition, int displacement) {
		if (displacement >> 7 == 1) {
			displacement -= 1;
			displacement ^= 0xFF;
			displacement *= -1;
		}
		if (condition) {
			currentCycles = 3;
			if ((programCounter & 0xFF) + displacement > 0xFF) {
				currentCycles += 1;
			}
			programCounter += displacement;
		} else {
			currentCycles = 2;
		}
	}

	private void compareWithAccumulator(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int result = 0;
		switch (opcode) {
		case 0xC9:
			result = accumulator - operand0;
			currentCycles = 2;
			break;
		case 0xC5:
			result = accumulator - memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0xD5:
			result = accumulator - memory.getByte(zeroPageWithOffset(operand0, xIndex));
			currentCycles = 4;
			break;
		case 0xCD:
			result = accumulator - memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0xDD:
			result = accumulator - memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			currentCycles = isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex) ? 5 : 4;
			break;
		case 0xD9:
			result = accumulator - memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			currentCycles = isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex) ? 5 : 4;
			break;
		case 0xC1:
			result = accumulator - memory.getByte(indirectX(operand0));
			currentCycles = 5;
			break;
		case 0xD1:
			result = accumulator - memory.getByte(indirectY(operand0));
			currentCycles = isPageBoundaryCrossed(
					convertOperandsToAddress(memory.getByte(operand0), memory.getByte(operand0 + 1)), yIndex) ? 6 : 5;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = result == 0;
		carryFlag = result >= 0;
		if (result < 0) {
			result = 0x100 + result;
		}
		signFlag = result >> 7 == 1;
	}

	private void compareWithX(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int result = 0;
		switch (opcode) {
		case 0xE0:
			result = xIndex - operand0;
			currentCycles = 2;
			break;
		case 0xE4:
			result = xIndex - memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0xEC:
			result = xIndex - memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = result == 0;
		carryFlag = result >= 0;
		if (result < 0) {
			result = 0x100 + result;
		}
		signFlag = result >> 7 == 1;
	}

	private void compareWithY(int opcode, int operand0, int operand1) throws IncorrectOpcodeException {
		int result = 0;
		switch (opcode) {
		case 0xC0:
			result = yIndex - operand0;
			currentCycles = 2;
			break;
		case 0xC4:
			result = yIndex - memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0xCC:
			result = yIndex - memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		default:
			throw new IncorrectOpcodeException();
		}
		zeroFlag = result == 0;
		carryFlag = result >= 0;
		if (result < 0) {
			result = 0x100 + result;
		}
		signFlag = result >> 7 == 1;
	}

	private void incrementMemory(int opcode, int operand0, int operand1) {
		int newVal = -1;
		switch (opcode) {
		case 0xE6:
			newVal = (memory.getByte(operand0) + 1) & 0xFF;
			memory.setByte(operand0, newVal);
			currentCycles = 5;
			break;
		case 0xF6:
			int address = (operand0 + xIndex) & 0xFF;
			newVal = (memory.getByte(address) +1) & 0xFF;
			memory.setByte(address, newVal);
			currentCycles = 6;
			break;
		case 0xEE:
			newVal = (memory.getByte(convertOperandsToAddress(operand0, operand1)) + 1) & 0xFF;
			memory.setByte(convertOperandsToAddress(operand0, operand1), newVal);
			currentCycles = 6;
			break;
		case 0xFE:
			newVal = (memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex)) + 1) & 0xFF;
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), newVal);
			currentCycles = 7;
			break;
		}
		zeroFlag = newVal == 0;
		signFlag = newVal >> 7 == 1;
	}

	private void loadIntoAccumulator(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0xA9: // Load Accumulator immediate
			accumulator = operand0;
			currentCycles = 2;
			break;
		case 0xA5: // " " Zero Page
			accumulator = memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0xB5: // " " Zero Page X
			accumulator = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			currentCycles = 4;
			break;
		case 0xAD: // " " Absolute
			accumulator = memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0xBD: // " " Absolute X
			accumulator = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
				currentCycles += 1;
			}
			break;
		case 0xB9: // " " Absolute Y
			accumulator = memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
				currentCycles += 1;
			}
			break;
		case 0xA1: // " " Indirect X
			accumulator = memory.getByte(indirectX(operand0));
			currentCycles = 6;
			break;
		case 0xB1: // " " Indirect Y
			int address = indirectY(operand0);
			accumulator = memory.getByte(address);
			currentCycles = 5;
			if (isPageBoundaryCrossed(address, yIndex)) {
				currentCycles += 1;
			}
			break;
		}
		zeroFlag = accumulator == 0;
		signFlag = accumulator >> 7 == 1;
	}

	private void loadIntoX(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0xA2: // Load X Index with operand
			xIndex = operand0;
			currentCycles = 2;
			break;
		case 0xA6: // " " with zero page + operand
			xIndex = memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0xB6: // " " with zero page + operand + x index
			xIndex = memory.getByte(zeroPageWithOffset(operand0, yIndex));
			currentCycles = 4;
			break;
		case 0xAE: // " " with absolute
			xIndex = memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0xBE: // " " with absolute + x index
			xIndex = memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
				currentCycles += 1;
			}
			break;
		}
		zeroFlag = xIndex == 0;
		signFlag = xIndex >> 7 == 1;
	}

	private void loadIntoY(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0xA0: // Load Y Index with operand
			yIndex = operand0;
			currentCycles = 2;
			break;
		case 0xA4: // " " with zero page + operand
			yIndex = memory.getByte(operand0);
			currentCycles = 3;
			break;
		case 0xB4: // " " with zero page + operand + x index
			yIndex = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			currentCycles = 4;
			break;
		case 0xAC: // " " with absolute
			yIndex = memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0xBC: // " " with absolute + x index
			yIndex = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
				currentCycles += 1;
			}
			break;
		}
		zeroFlag = yIndex == 0;
		signFlag = yIndex >> 7 == 1;
	}

	private void andWithAccumulator(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0x29: // AND with operand
			accumulator &= operand0;
			currentCycles = 2;
			break;
		case 0x25: // AND zero page
			accumulator &= memory.getByte((int) operand0);
			currentCycles = 3;
			break;
		case 0x35: // AND zero page + x
			accumulator &= memory.getByte(zeroPageWithOffset(operand0, xIndex));
			currentCycles = 4;
			break;
		case 0x2D: // AND absolute
			accumulator &= memory.getByte(convertOperandsToAddress(operand0, operand1));
			currentCycles = 4;
			break;
		case 0x3D: // AND absolute + X
			accumulator &= memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
				currentCycles += 1;
			}
			break;
		case 0x39: // AND absolute + Y
			accumulator &= memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
				currentCycles += 1;
			}
			break;
		case 0x21: // Indirect X
			accumulator &= memory.getByte(indirectX(operand0));
			currentCycles = 6;
			break;
		case 0x31: // Indirect Y
			accumulator &= memory.getByte(indirectY(operand0));
			currentCycles = 5;
			if (isPageBoundaryCrossed(memory.getByte(operand0), yIndex)) {
				currentCycles += 1;
			}
			break;
		}
		zeroFlag = accumulator == 0;
		signFlag = accumulator >> 7 == 1;
	}

	private void storeAccumulator(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0x85: // Store accumulator zero page
			memory.setByte(operand0, accumulator);
			currentCycles = 3;
			break;
		case 0x95: // Zero Page X Offset
			memory.setByte(zeroPageWithOffset(operand0, xIndex), accumulator);
			currentCycles = 4;
			break;
		case 0x8D:
			memory.setByte(convertOperandsToAddress(operand0, operand1), accumulator);
			currentCycles = 4;
			break;
		case 0x9D:
			memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), accumulator);
			currentCycles = 5;
			break;
		case 0x99:
			memory.setByte(convertOperandsToAddress(operand0, operand1, yIndex), accumulator);
			currentCycles = 5;
			break;
		case 0x81:
			memory.setByte(indirectX(operand0), accumulator);
			currentCycles = 6;
			break;
		case 0x91:
			memory.setByte(indirectY(operand0), accumulator);
			currentCycles = 6;
			break;
		}
	}

	private void addWithCarry(int opcode, int operand0, int operand1) {
		int addend1 = accumulator;
		int addend2 = 0;
		switch (opcode) {
		case 0x69:
			addend2 = operand0;
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 2;
			break;
		case 0x65:
			addend2 = memory.getByte(operand0);
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 3;
			break;
		case 0x75:
			addend2 = memory.getByte(zeroPageWithOffset(operand0, xIndex));
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 4;
			break;
		case 0x6D:
			addend2 = memory.getByte(convertOperandsToAddress(operand0, operand1));
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 4;
			break;
		case 0x7D:
			addend2 = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
				currentCycles += 1;
			}
			break;
		case 0x79:
			addend2 = memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 4;
			if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
				currentCycles += 1;
			}
			break;
		case 0x61:
			addend2 = memory.getByte(indirectX(operand0));
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 6;
			break;
		case 0x71:
			addend2 = memory.getByte(indirectY(operand0));
			accumulator = addWithCarry(addend1, addend2);
			currentCycles = 5;
			if (isPageBoundaryCrossed(convertOperandsToAddress(0x00, memory.getByte(operand0)), yIndex)) {
				currentCycles += 1;
			}
			break;
		}
		zeroFlag = accumulator == 0;
		signFlag = accumulator >> 7 == 1;
		overflowFlag = evaluateOverflowAdc(addend1, addend2, accumulator);
	}

	private void storeX(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0x86:
			memory.setByte(operand0, xIndex);
			currentCycles = 3;
			break;
		case 0x96:
			memory.setByte(zeroPageWithOffset(operand0, yIndex), xIndex);
			currentCycles = 4;
			break;
		case 0x8E:
			memory.setByte(convertOperandsToAddress(operand0, operand1), xIndex);
			currentCycles = 4;
			break;
		}
	}

	private void storeY(int opcode, int operand0, int operand1) {
		switch (opcode) {
		case 0x84:
			memory.setByte(operand0, yIndex);
			currentCycles = 3;
			break;
		case 0x94:
			memory.setByte(zeroPageWithOffset(operand0, xIndex), yIndex);
			currentCycles = 4;
			break;
		case 0x8C:
			memory.setByte(convertOperandsToAddress(operand0, operand1), yIndex);
			currentCycles = 4;
			break;
		}
	}

	private int indirect(int operand0, int operand1) {
		if (operand0 != 0xFF) {
			int lowByte = memory.getByte(convertOperandsToAddress(operand0, operand1));
			int highByte = memory.getByte(convertOperandsToAddress(operand0, operand1, 1)) * ENDIAN_MULT;
			return lowByte + highByte;
		} else {
			int lowByte = memory.getByte(convertOperandsToAddress(operand0, operand1));
			int highByte = memory.getByte(convertOperandsToAddress(0x00, operand1)) * ENDIAN_MULT;
			return lowByte + highByte;
		}
	}

	private int convertOperandsToAddress(int operand0, int operand1) {
		return operand0 + operand1 * ENDIAN_MULT;
	}

	private int convertOperandsToAddress(int operand0, int operand1, int offset) {
		return (operand0 + operand1 * ENDIAN_MULT + offset) & 0xFFFF;
	}

	private int zeroPageWithOffset(int operand0, int offset) {
		return (operand0 + offset) & 0xFF;
	}

	private int indirectX(int operand0) {
		int addressLowByte = (operand0 + xIndex) & 0xFF;
		int addressHighByte = (operand0 + xIndex + 1) & 0xFF;
		return memory.getByte(addressLowByte) + memory.getByte(addressHighByte) * ENDIAN_MULT;
	}

	private int indirectY(int operand0) {
		int add0 = operand0 & 0xFF;
		int add1 = (operand0 + 1) & 0xFF;
		int address = memory.getByte(add0) + memory.getByte(add1) * ENDIAN_MULT + yIndex;
		return address & 0xFFFF;
	}

	private int addWithCarry(int operand0, int operand1) {
		int result = operand0 + operand1;
		result += carryFlag ? 1 : 0;
		if (result > 0xFF) {
			carryFlag = true;
		} else {
			carryFlag = false;
		}
		return result % 0x100;
	}

	private boolean isPageBoundaryCrossed(int address, int offset) {
		return (address & 0xFF) > ((address + offset) & 0xFF); //TODO this doesn't account for a backward page boundary cross
	}

	private void NMI() {
		push(programCounter >> 8);
		push(programCounter & 0xFF);
		push(getStatus());
		programCounter = memory.getByte(0xFFFA) + memory.getByte(0xFFFB) * ENDIAN_MULT;
		currentCycles = 7;
		interruptFlag = true;
		nmi = false;
	}

	public boolean evaluateOverflowAdc(int addend1, int addend2, int result) {
		return (!(((addend1 ^ addend2) & 0x80) == 0x80)) && ((addend1 ^ result) & 0x80) == 0x80;
	}

	public boolean evaluateOverflowSbc(int addend1, int addend2, int result) {
		return ((((addend1 ^ addend2) & 0x80) == 0x80)) && ((addend1 ^ result) & 0x80) == 0x80;
	}

	private void turnOnFileOutput() {
		try {
			System.setOut(new PrintStream(new File("logfile.txt")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {
		while (true) {
			try {
				if (nmi) {
					NMI();
				} else if (OpcodeLookup.oneByteOpcodes.contains(memory.getByte(programCounter))) {
					int opcode = memory.getByte(programCounter++);
					runInstruction(opcode, 0, 0);
					cacheOperation(opcode);
				} else if (OpcodeLookup.twoByteOpcodes.contains(memory.getByte(programCounter))) {
					int opcode = memory.getByte(programCounter++);
					int operand = memory.getByte(programCounter++);
					runInstruction(opcode, operand, 0);
					cacheOperation(opcode, operand);
				} else if (OpcodeLookup.threeByteOpcodes.contains(memory.getByte(programCounter))) {
					int opcode = memory.getByte(programCounter++);
					int operand0 = memory.getByte(programCounter++);
					int operand1 = memory.getByte(programCounter++);
					runInstruction(opcode, operand0, operand1);
					cacheOperation(opcode, operand0, operand1);
				} else {
					throw new UnknownOpcodeException();
				}
			} catch (IncorrectOpcodeException | UnknownOpcodeException e) {
				e.printStackTrace();
				log.error("Unrecognized Opcode: {}", Integer.toHexString(memory.getByte(programCounter)));
				log.error("Most Recent Operations: " + operationCache);
				throw new RuntimeException();
			}
			if (accumulator > 0xFF) {
				log.error("Critical error, accumulator has an invalid value: {}", Integer.toHexString(accumulator));
				log.error("Most recent operations: {}", operationCache);
				throw new RuntimeException();
			} else if (xIndex > 0xFF) {
				log.error("Critical error, x index has an invalid value: {}", Integer.toHexString(xIndex));
				log.error("Most recent operations: {}", operationCache);
				throw new RuntimeException();
			} else if (yIndex > 0xFF) {
				log.error("Critical error, y index has an incorrect value: {}", Integer.toHexString(yIndex));
				log.error("Most recent operations: {}", operationCache);
				throw new RuntimeException();
			} else if (stackPointer > 0xFF) {
				log.error("Critical error, stack pointer has an incorrect value: {}",
						Integer.toHexString(stackPointer));
				log.error("Most recent operations: {}", operationCache);
				throw new RuntimeException();
			} else if (programCounter > 0xFFFF) {
				log.error("Critical error, program counter has an incorrect value: {}", toHexString(programCounter));
				log.error("Most recent operations: {}", operationCache);
				throw new RuntimeException();
			}
			totalCycles += currentCycles;
			for (int i = 0; i < currentCycles * 3; i++) {
				ppu.draw();
			}
		}
	}
}
