package nespresso.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

public class NtscScreen extends Canvas  {

	private int x = 0;
	private int y = 0;
	private Color c;
	
	
	public void drawPixel(int x, int y, Color c) {
		this.x = x;
		this.y = y;
		this.c = c;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(c);
		g.drawLine(x, y, x, y);
	}
}
