package nespresso.controllers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import lombok.extern.slf4j.Slf4j;
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
	
	public void initializeNes() {
		ppu = PictureProcessingUnit.getInstance();
		try {
			memory = new NromMapper(new FileInputStream(nesTest), ppu);
		} catch (FileNotFoundException e) {
			log.error("Cannot find rom");
			e.printStackTrace();
		}
		processor = new Processor(memory);
		processor.setPpu(ppu);
		ppu.setProcessor(processor);
		ppu.setMemory(memory);
	}	
	
	public static void main(String[] args) {
		NesController controller = new NesController();
		controller.initializeNes();
		controller.runNes();
	}

	public void runNes() {
		processor.start();
	}
}
