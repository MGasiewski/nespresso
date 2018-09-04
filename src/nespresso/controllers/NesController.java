package nespresso.controllers;

import java.awt.Canvas;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.swing.JFrame;

import lombok.extern.slf4j.Slf4j;
import nespresso.memory.MMC1Mapper;
import nespresso.memory.Memory;
import nespresso.memory.NromMapper;
import nespresso.processing.PictureProcessingUnit;
import nespresso.processing.Processor;
@Slf4j
public class NesController {
	private Processor processor;
	private Memory memory;
	private PictureProcessingUnit ppu;
	private String donkeyKong = "C:\\Users\\Matt\\IdeaProjects\\Nespresso\\bin\\donkey kong.nes"; 
	private String nesTest = "C:\\Users\\Matt\\IdeaProjects\\Nespresso\\bin\\nestest.nes";
	private String test = "C:\\Users\\Matt\\nes-test-roms\\blargg_nes_cpu_test5\\official.nes";
	private String cpuTest = "C:\\Users\\Matt\\Downloads\\instr_test-v5\\instr_test-v5\\rom_singles\\03-immediate.nes";
	private String ppuTest= "C:\\Users\\Matt\\nes-test-roms\\ppu_vbl_nmi\\rom_singles\\07-nmi_on_timing.nes";
	private String tetris = "C:\\Users\\Matt\\Downloads\\Tetris (USA).nes";
	private String balloonFight = "C:\\Users\\Matt\\Downloads\\Balloon Fight (USA)\\Balloon Fight (USA).nes";
	private final int HEIGHT = 270;
	private final int WIDTH = 266;
	
	public void initializeNes() {
		ppu = PictureProcessingUnit.getInstance();
		try {
			//memory = new MMC1Mapper(new FileInputStream(test), ppu);
			memory = new NromMapper(new FileInputStream(cpuTest), ppu);
		} catch (FileNotFoundException e) {
			log.error("Cannot find rom");
			e.printStackTrace();
		}
		processor = new Processor(memory);
		processor.setPpu(ppu);
		ppu.setProcessor(processor);
		ppu.setMemory(memory);
		ppu.setCanvas(initializeGraphics());
	}	
	
	public static void main(String[] args) {
		NesController controller = new NesController();
		controller.initializeNes();
		controller.runNes();
	}

	public void runNes() {
		processor.start();
	}
	
	public Canvas initializeGraphics() {
		JFrame frame = new JFrame("Nespresso");
		frame.setSize(WIDTH + 5, HEIGHT + 5);
		Canvas canvas = new Canvas();
		canvas.setSize(WIDTH, HEIGHT);
		frame.add(canvas);
		frame.setVisible(true);
		return canvas;
	}
}
