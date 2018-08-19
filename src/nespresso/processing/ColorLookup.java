package nespresso.processing;

import java.util.HashMap;
import java.util.Map;
import java.awt.Color;

public class ColorLookup {
	private static Map<Integer, Color> lookup = new HashMap<>();
	static {
		lookup.put(0x0, convert(84, 84, 84));
		lookup.put(0x1, convert(0, 30, 116));
		lookup.put(0x2, convert(8, 16, 144));
		lookup.put(0x3, convert(48, 0, 136));
		lookup.put(0x4, convert(68, 0, 100));
		lookup.put(0x5, convert(92, 0, 48));
		lookup.put(0x6, convert(84, 4, 0));
		lookup.put(0x7, convert(60, 24, 0));
		lookup.put(0x8, convert(32, 42, 0));
		lookup.put(0x9, convert(8, 58, 0));
		lookup.put(0xA, convert(0, 65, 0));
		lookup.put(0xB, convert(0, 60, 0));
		lookup.put(0xC, convert(0, 50, 60));
		lookup.put(0xD, convert(0, 0, 0));
		lookup.put(0xE, convert(0, 0, 0));
		lookup.put(0xF, convert(0, 0, 0));

		lookup.put(0x10, convert(152, 150, 152));
		lookup.put(0x11, convert(8, 76, 196));
		lookup.put(0x12, convert(48, 50, 236));
		lookup.put(0x13, convert(92, 30, 228));
		lookup.put(0x14, convert(136, 20, 176));
		lookup.put(0x15, convert(160, 20, 100));
		lookup.put(0x16, convert(152, 34, 32));
		lookup.put(0x17, convert(120, 60, 0));
		lookup.put(0x18, convert(84, 90, 0));
		lookup.put(0x19, convert(40, 114, 0));
		lookup.put(0x1A, convert(8, 124, 0));
		lookup.put(0x1B, convert(0, 118, 40));
		lookup.put(0x1C, convert(0, 102, 120));
		lookup.put(0x1D, convert(0, 0, 0));
		lookup.put(0x1E, convert(0, 0, 0));
		lookup.put(0x1F, convert(0, 0, 0));

		lookup.put(0x20, convert(236, 238, 236));
		lookup.put(0x21, convert(76, 154, 236));
		lookup.put(0x22, convert(120, 124, 236));
		lookup.put(0x23, convert(176, 98, 236));
		lookup.put(0x24, convert(228, 84, 236));
		lookup.put(0x25, convert(236, 88, 180));
		lookup.put(0x26, convert(236, 106, 100));
		lookup.put(0x27, convert(212, 136, 32));
		lookup.put(0x28, convert(160, 170, 0));
		lookup.put(0x29, convert(116, 196, 0));
		lookup.put(0x2A, convert(76, 208, 32));
		lookup.put(0x2B, convert(56, 204, 108));
		lookup.put(0x2C, convert(56, 180, 204));
		lookup.put(0x2D, convert(60, 60, 60));
		lookup.put(0x2E, convert(0, 0, 0));
		lookup.put(0x2F, convert(0, 0, 0));
		
		lookup.put(0x30, convert(236, 238, 236));
		lookup.put(0x31, convert(168, 204, 236));
		lookup.put(0x32, convert(188, 188, 236));
		lookup.put(0x33, convert(212, 178, 236));
		lookup.put(0x34, convert(236, 174, 236));
		lookup.put(0x35, convert(236, 174, 212));
		lookup.put(0x36, convert(236, 180, 176));
		lookup.put(0x37, convert(228, 196, 144));
		lookup.put(0x38, convert(204, 210, 120));
		lookup.put(0x39, convert(180, 222, 120));
		lookup.put(0x3A, convert(168, 226, 144));
		lookup.put(0x3B, convert(152, 226, 180));
		lookup.put(0x3C, convert(160, 214, 228));
		lookup.put(0x3D, convert(160, 162, 160));
		lookup.put(0x3E, convert(0, 0, 0));
		lookup.put(0x3F, convert(0, 0, 0));
	}

	private static Color convert(int red, int green, int blue) {
		return new Color(red, green, blue);
	}
	
	public static int get(int hex) {
		return lookup.get(hex).getRGB();
	}
}
