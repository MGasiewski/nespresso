public class Processor {
    private int accumulator = 0;
    private int xIndex = 0;
    private int yIndex = 0;
    private int stackPointer = 0;
    private int programCounter = 0;
    private boolean carryFlag = false;
    private boolean zeroFlag = false;
    private boolean interruptFlag = false;
    private boolean decimalModeFlag = false;
    private boolean softwareInterruptFlag = false;
    private boolean overflowFlag = false;
    private boolean signFlag = false;
    private static int ENDIAN_MULT = 256;
    private Memory memory = new Memory();
    private int currentCycles = 0;

    public void runInstruction(int opcode, int operand0, int operand1) {
        switch (opcode) {
            //ASL - Arithmetic Shift Left
            case 0x0A:
            case 0x06:
            case 0x16:
            case 0x0E:
            case 0x1E:
                arithmeticShiftLeft(opcode, operand0, operand1);
                break;
            //BCC - Branch if carry clear
            case 0x90:
                branch(!carryFlag, operand0);
                break;
            //BCS - Branch if carry set
            case 0xB0:
                branch(carryFlag, operand0);
                break;
            //BEQ - Branch if equal
            case 0xF0:
                branch(zeroFlag, operand0);
                break;
            //BMI - Branch if minus
            case 0x30:
                branch(signFlag, operand0);
                break;
            //BNE - Branch if not equal
            case 0xD0:
                branch(!zeroFlag, operand0);
                break;
            //BPL - Branch if positive
            case 0x10:
                branch(!signFlag, operand0);
                break;
            //BVC - Branch if Overflow Clear
            case 0x50:
                branch(!overflowFlag, operand0);
                break;
            //BVS - Branch if Overflow Set
            case 0x70:
                branch(overflowFlag, operand0);
                break;
            //JSR - Jump to subroutine
            case 0x20:
                //TODO implement (unclear on how atm)
                break;
            //JMP
            case 0x4C:
                programCounter = convertOperandsToAddress(operand0, operand1);
                currentCycles = 3;
                break;
            case 0x6C:
                programCounter = indirect(operand0, operand1);
                currentCycles = 5;
                break;
            //LDA COMMANDS
            case 0xA9:
            case 0xA5:
            case 0xB5:
            case 0xAD:
            case 0xBD:
            case 0xB9:
            case 0xA1:
            case 0xB1:
                loadIntoAccumulator(opcode, operand0, operand1);
                break;
            //LDX COMMANDS
            case 0xA2:
            case 0xA6:
            case 0xB6:
            case 0xAE:
            case 0xBE:
                loadIntoX(opcode, operand0, operand1);
                break;
            //LDY COMMANDS
            case 0xA0:
            case 0xA4:
            case 0xB4:
            case 0xAC:
            case 0xBC:
                loadIntoY(opcode, operand0, operand1);
                break;
            //AND COMMANDS
            case 0x29:
            case 0x25:
            case 0x35:
            case 0x2D:
            case 0x3D:
            case 0x39:
            case 0x21:
            case 0x31:
                andWithAccumulator(opcode, operand0, operand1);
                break;
            //STA COMMANDS
            case 0x85:
            case 0x95:
            case 0x8D:
            case 0x9D:
            case 0x99:
            case 0x81:
            case 0x91:
                storeAccumulator(opcode, operand0, operand1);
                break;
            //STX commands
            case 0x86:
            case 0x96:
            case 0x8E:
                storeX(opcode, operand0, operand1);
                break;
            //STY commands
            case 0x84:
            case 0x94:
            case 0x8C:
                storeY(opcode, operand0, operand1);
                break;
            //TAX Transfer accumulator to X
            case 0xAA:
                xIndex = accumulator;
                currentCycles = 2;
                if (xIndex == 0) {
                    zeroFlag = true;
                }
                if (xIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //TAY
            case 0xA8:
                yIndex = accumulator;
                currentCycles = 2;
                if (yIndex == 0) {
                    zeroFlag = true;
                }
                if (yIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //TSX
            case 0xBA:
                xIndex = stackPointer;
                currentCycles = 2;
                if (xIndex == 0) {
                    zeroFlag = true;
                }
                if (xIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //TXA
            case 0x8A:
                accumulator = xIndex;
                currentCycles = 2;
                if (accumulator == 0) {
                    zeroFlag = true;
                }
                if (accumulator >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //TXS
            case 0x9A:
                stackPointer = xIndex;
                currentCycles = 2;
                break;
            //TYA
            case 0x98:
                accumulator = yIndex;
                currentCycles = 2;
                if (accumulator == 0) {
                    zeroFlag = true;
                }
                if (accumulator >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //ADC Add with carry
            case 0x69:
            case 0x65:
            case 0x75:
            case 0x6D:
            case 0x7D:
            case 0x79:
            case 0x61:
            case 0x71:
                addWithCarry(opcode, operand0, operand1);
                break;
            //BRK - Break
            case 0x00:
                //CMP - Compare with accumulator
            case 0xC9:
            case 0xC5:
            case 0xD5:
            case 0xCD:
            case 0xDD:
            case 0xD9:
            case 0xC1:
            case 0xD1:
                compareWithAccumulator(opcode, operand0, operand1);
                break;
            //CPX - Compare with X
            case 0xE0:
            case 0xE4:
            case 0xEC:
                compareWithX(opcode, operand0, operand1);
                break;
            //CPY - Compare with Y
            case 0xC0:
            case 0xC4:
            case 0xCC:
                compareWithY(opcode, operand0, operand1);
                break;
            //DEC - Decrement memory
            case 0xC6:
            case 0xD6:
            case 0xCE:
            case 0xDE:
                decrementMemory(opcode, operand0, operand1);
                break;
            //DEX - decrement X
            case 0xCA:
                xIndex -= 1;
                if (xIndex == 0) {
                    zeroFlag = true;
                }
                if (xIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //DEY - decrement Y
            case 0x88:
                yIndex -= 1;
                if (yIndex == 0) {
                    zeroFlag = true;
                }
                if (yIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //EOR - Exclusive Or
            case 0x49:
            case 0x45:
            case 0x55:
            case 0x4D:
            case 0x5D:
            case 0x59:
            case 0x41:
            case 0x51:
                xorAccumulator(opcode, operand0, operand1);
                break;
            //CLC - Clear Carry
            case 0x18:
                carryFlag = false;
                currentCycles = 2;
                break;
            //SEC - Set Carry
            case 0x38:
                carryFlag = true;
                currentCycles = 2;
                break;
            //CLI - Clear Interrupt
            case 0x58:
                interruptFlag = false;
                currentCycles = 2;
                break;
            //SEI - Set Interrupt
            case 0x78:
                interruptFlag = true;
                currentCycles = 2;
                break;
            //CLV - Clear Overflow
            case 0xB8:
                overflowFlag = false;
                currentCycles = 2;
                break;
            //CLD - Clear Decimal Mode
            case 0xD8:
                decimalModeFlag = false;
                currentCycles = 2;
                break;
            //SED - Set Decimal Mode
            case 0xF8:
                decimalModeFlag = true;
                currentCycles = 2;
                break;
            //INC - Increment Memory
            case 0xE6:
            case 0xF6:
            case 0xEE:
            case 0xFE:
                incrementMemory(opcode, operand0, operand1);
                break;
            //INX - Increment x index
            case 0xE8:
                xIndex++;
                currentCycles = 2;
                if (xIndex == 0) {
                    zeroFlag = true;
                }
                if (xIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //INY - Increment y index
            case 0xC8:
                yIndex++;
                currentCycles = 2;
                if (yIndex == 0) {
                    zeroFlag = true;
                }
                if (yIndex >> 7 == 1) {
                    signFlag = true;
                }
                break;
            //NOP - no operation
            case 0xEA:
                currentCycles = 2;
                break;
            //LSR - Logical Shift Right
            case 0x4A:
            case 0x46:
            case 0x56:
            case 0x4E:
            case 0x5E:
                logicalShiftRight(opcode, operand0, operand1);
                break;
            default:
                break; //TODO add exception
        }
    }

    private void logicalShiftRight(int opcode, int operand0, int operand1) {
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
                //TODO exception
                break;
        }
        if (previous % 2 == 0) {
            carryFlag = false;
        } else {
            carryFlag = true;
        }
        if (result == 0) {
            zeroFlag = true;
        }
        if (result >> 7 == 1) {
            signFlag = true;
        }
    }

    private void xorAccumulator(int opcode, int operand0, int operand1) {
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
                accumulator = accumulator ^ zeroPageWithOffset(operand0, xIndex);
                currentCycles = 4;
                break;
            case 0x4D:
                accumulator = accumulator ^ zeroPageWithOffset(operand0, yIndex);
                currentCycles = 4;
                break;
            case 0x5D:
                accumulator = accumulator ^ convertOperandsToAddress(operand0, operand1);
                currentCycles = 4;
                //TODO page boundary cycle
                break;
            case 0x59:
                accumulator = accumulator ^ convertOperandsToAddress(operand0, operand1, xIndex);
                //TODO page boundary cycle
                break;
            case 0x41:
                accumulator = accumulator ^ memory.getByte(indirectX(operand0));
                currentCycles = 6;
                break;
            case 0x51:
                accumulator = accumulator ^ memory.getByte(indirectY(operand0));
                currentCycles = 5;
                //TODO page boundary cycle
                break;
            default:
                //TODO throw exception
                break;
        }
    }

    private void decrementMemory(int opcode, int operand0, int operand1) {
        int result = 0;
        switch (opcode) {
            case 0xC6:
                result = memory.getByte(operand0) - 1;
                currentCycles = 5;
                memory.setByte(operand0, result);
                break;
            case 0xD6:
                result = memory.getByte(zeroPageWithOffset(operand0, xIndex)) - 1;
                currentCycles = 6;
                memory.setByte(zeroPageWithOffset(operand0, xIndex), result);
                break;
            case 0xCE:
                result = memory.getByte(convertOperandsToAddress(operand0, operand1)) - 1;
                currentCycles = 6;
                memory.setByte(convertOperandsToAddress(operand0, operand1), result);
                break;
            case 0xDE:
                result = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
                currentCycles = 7;
                memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), result);
                break;
            default:
                //TODO throw exception
                break;
        }
        if (result == 0) {
            zeroFlag = true;
        }
        if (result >> 7 == 1) {
            signFlag = true;
        }
    }

    private void arithmeticShiftLeft(int opcode, int operand0, int operand1) {
        int beginValue = 0;
        int endValue = 0;
        switch (opcode) {
            case 0x0A:
                beginValue = accumulator;
                endValue = (beginValue << 1) % 0xFF;
                accumulator = endValue;
                break;
            case 0x06:
                beginValue = memory.getByte(operand0);
                endValue = (beginValue << 1) % 0xFF;
                memory.setByte(operand0, endValue);
                break;
            case 0x16:
                beginValue = memory.getByte(zeroPageWithOffset(operand0, xIndex));
                endValue = (beginValue << 1) % 0xFF;
                memory.setByte(zeroPageWithOffset(operand0, xIndex), endValue);
                break;
            case 0x0E:
                beginValue = memory.getByte(convertOperandsToAddress(operand0, operand1));
                endValue = (beginValue << 1) % 0xFF;
                memory.setByte(convertOperandsToAddress(operand0, operand1), endValue);
            case 0x1E:
                beginValue = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
                endValue = (beginValue << 1) % 0xFF;
                memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), endValue);
                break;
            default:
                //TODO throw exception
        }
        if (beginValue >> 7 == 1) {
            carryFlag = true;
        }
        if (endValue == 0) {
            zeroFlag = true;
        }
        if (endValue >> 7 == 1) {
            signFlag = true;
        }
    }

    private void branch(boolean condition, int displacement) {
        if (condition) {
            currentCycles = 3;
            if (programCounter % 0xFF + displacement > 0xFF) {
                currentCycles += 1;
            }
            programCounter += displacement;
        } else {
            currentCycles = 2;
        }
    }

    private void compareWithAccumulator(int opcode, int operand0, int operand1) {
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
                result = accumulator - memory.getByte(operand0 + xIndex); //TODO how to handle overflow?
                currentCycles = 4;
                break;
            case 0xCD:
                result = accumulator - memory.getByte(convertOperandsToAddress(operand0, operand1));
                currentCycles = 4;
                break;
            case 0xDD:
                result = accumulator - memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex)); //TODO handle page boundary
                currentCycles = 4;
                break;
            case 0xD9:
                result = accumulator - memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex)); //TODO handle page boundary
                currentCycles = 6;
                break;
            case 0xC1:
                result = accumulator - memory.getByte(indirectX(operand0));
                currentCycles = 5;
                break;
            case 0xD1:
                result = accumulator - memory.getByte(indirectY(operand0));
                break;
            default:
                //TODO exception
                break;
        }
        if (result > 0) {

        }
    } //TODO needs work

    private void compareWithX(int opcode, int operand0, int operand1) {
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
                break; //TODO add exception
        }
        if (result > 0) {

        }
    } //TODO needs work

    private void compareWithY(int opcode, int operand0, int operand1) {
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
                break; //TODO add exception
        }
        if (result > 0) {

        }
    } //TODO needs work

    private void incrementMemory(int opcode, int operand0, int operand1) {
        int newVal = -1;
        switch (opcode) {
            case 0xE6:
                newVal = memory.getByte(operand0) + 1;
                memory.setByte(operand0, newVal);
                currentCycles = 5;
                break;
            case 0xF6:
                newVal = memory.getByte(operand0 + xIndex);
                memory.setByte(operand0, newVal);
                currentCycles = 6;
                break;
            case 0xEE:
                newVal = memory.getByte(convertOperandsToAddress(operand0, operand1)) + 1;
                memory.setByte(convertOperandsToAddress(operand0, operand1), newVal);
                currentCycles = 6;
                break;
            case 0xFE:
                newVal = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex)) + 1;
                memory.setByte(convertOperandsToAddress(operand0, operand1, xIndex), newVal);
                currentCycles = 7;
                break;
        }
        if (newVal == 0) {
            zeroFlag = true;
        }
        if (newVal >> 7 == 1) {
            signFlag = true;
        }
    }

    private void loadIntoAccumulator(int opcode, int operand0, int operand1) {
        switch (opcode) {
            case 0xA9: //Load Accumulator immediate
                accumulator = operand0;
                currentCycles = 2;
                break;
            case 0xA5: //" " Zero Page
                accumulator = memory.getByte(operand0);
                currentCycles = 3;
                break;
            case 0xB5: //" " Zero Page X
                accumulator = memory.getByte(zeroPageWithOffset(operand0, xIndex));
                currentCycles = 4;
                break;
            case 0xAD: //" " Absolute
                accumulator = memory.getByte(convertOperandsToAddress(operand0, operand1));
                currentCycles = 4;
                break;
            case 0xBD: //" " Absolute X
                accumulator = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
                currentCycles = 4;
                if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
                    currentCycles += 1;
                }
                break;
            case 0xB9: //" " Absolute Y
                accumulator = memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex));
                currentCycles = 4;
                if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), yIndex)) {
                    currentCycles += 1;
                }
                break;
            case 0xA1: //" " Indirect X
                accumulator = memory.getByte(indirectX(operand0));
                currentCycles = 6;
                break;
            case 0xB1: //" " Indirect Y
                accumulator = memory.getByte(indirectY(operand0));
                currentCycles = 5;
                if (isPageBoundaryCrossed(memory.getByte(operand0), yIndex)) {
                    currentCycles += 1;
                }
                break;
        }
        if (accumulator == 0) {
            zeroFlag = true;
        }
        if (accumulator >> 7 == 1) {
            signFlag = true;
        }
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
                xIndex = memory.getByte(zeroPageWithOffset(operand0, xIndex));
                currentCycles = 4;
                break;
            case 0xAE: // " " with absolute
                xIndex = memory.getByte(convertOperandsToAddress(operand0, operand1));
                currentCycles = 4;
                break;
            case 0xBE: // " " with absolute + x index
                xIndex = memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex));
                currentCycles = 4;
                if (isPageBoundaryCrossed(convertOperandsToAddress(operand0, operand1), xIndex)) {
                    currentCycles += 1;
                }
                break;
        }
        if (xIndex == 0) {
            zeroFlag = true;
        }
        if (xIndex >> 7 == 1) {
            signFlag = true;
        }
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
        if (yIndex == 0) {
            zeroFlag = true;
        }
        if (yIndex >> 7 == 1) {
            signFlag = true;
        }
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
        if (accumulator == 0) {
            zeroFlag = true;
        }
        if (accumulator >> 7 == 1) {
            signFlag = true;
        }
    }

    private void storeAccumulator(int opcode, int operand0, int operand1) {
        switch (opcode) {
            case 0x85: // Store accumulator zero page
                memory.setByte(operand0, accumulator);
                currentCycles = 3;
                break;
            case 0x95: // Zero Page X Offset
                memory.setByte(operand0 + xIndex, accumulator);
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
        switch (opcode) {
            case 0x69:
                accumulator = addWithCarry(operand0, accumulator);
                currentCycles = 2;
                break;
            case 0x65:
                accumulator = addWithCarry(memory.getByte(operand0), accumulator);
                currentCycles = 3;
                break;
            case 0x75:
                accumulator = addWithCarry(memory.getByte(zeroPageWithOffset(operand0, xIndex)), accumulator);
                currentCycles = 4;
                break;
            case 0x6D:
                accumulator = addWithCarry(memory.getByte(convertOperandsToAddress(operand0, operand1)), accumulator);
                currentCycles = 4;
                break;
            case 0x7D:
                accumulator = addWithCarry(memory.getByte(convertOperandsToAddress(operand0, operand1, xIndex)), accumulator);
                currentCycles = 4;
                //TODO if page crossed, add a cycle
                break;
            case 0x79:
                accumulator = addWithCarry(memory.getByte(convertOperandsToAddress(operand0, operand1, yIndex)), accumulator);
                currentCycles = 4;
                //TODO if page crossed, add a cycle
                break;
            case 0x61:
                accumulator = addWithCarry(memory.getByte(indirectX(operand0)), accumulator);
                currentCycles = 6;
                break;
            case 0x71:
                accumulator = addWithCarry(memory.getByte(indirectY(operand0)), accumulator);
                currentCycles = 5;
                //TODO if page crossed, add a cycle
                break;
        }
        if (accumulator == 0) {
            zeroFlag = true;
        }
        //TODO set overflow flag if sign bit is incorrect?
        //TODO set if overflow in bit 7
    } //TODO needs work still

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
        return memory.getByte(convertOperandsToAddress(operand0, operand1)) + memory.getByte(convertOperandsToAddress(operand0, operand0, 1)) * ENDIAN_MULT;
    }

    private int convertOperandsToAddress(int operand0, int operand1) {
        return operand0 + operand1 * ENDIAN_MULT;
    }

    private int convertOperandsToAddress(int operand0, int operand1, int offset) {
        return operand0 + operand1 * ENDIAN_MULT + offset;
    }

    private int zeroPageWithOffset(int operand0, int offset) {
        return (operand0 + offset) % 0xFF;
    }

    private int indirectX(int operand0) {
        return memory.getByte(operand0 + xIndex) + memory.getByte(operand0 + xIndex + 1) * ENDIAN_MULT;
    }

    private int indirectY(int operand0) {
        return memory.getByte(operand0) + memory.getByte(operand0 + 1) * ENDIAN_MULT + yIndex;
    }

    private int addWithCarry(int operand0, int operand1) {
        return (operand0 + operand1) % 0xFF;
    }

    private boolean isPageBoundaryCrossed(int address, int offset) {
        return address % 0x100 > (address + offset) % 0x100;
    }
}
