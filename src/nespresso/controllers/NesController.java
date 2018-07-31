package nespresso.controllers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;
import nespresso.memory.Memory;
import nespresso.memory.NromMapper;
import nespresso.processing.Clock;
import nespresso.processing.HandOffClock;
import nespresso.processing.PictureProcessingUnit;
import nespresso.processing.Processor;

@Slf4j
public class NesController {
	private Clock clock;	
	private Processor processor;
	private Memory memory;
	private PictureProcessingUnit ppu;
	private ExecutorService executor = Executors.newFixedThreadPool(2);
	
	public void initializeNes() {
		clock = new HandOffClock();
		ppu = new PictureProcessingUnit();
		try {
			memory = new NromMapper(new FileInputStream("C:\\Users\\Matt\\IdeaProjects\\Nespresso\\bin\\donkey kong.nes"), ppu);
		} catch (FileNotFoundException e) {
			log.error("Cannot find rom");
			e.printStackTrace();
		}
		processor = new Processor(memory);
		ppu.setMemory(memory);
		ppu.setClock(clock);
		processor.setClock(clock);
	}
	
	public static void main(String[] args) {
		NesController controller = new NesController();
		controller.initializeNes();
		controller.runNes();
	}

	public void runNes() {
		executor.submit(processor);
		executor.submit(ppu);
	}
}
