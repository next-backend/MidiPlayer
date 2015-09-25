package com.eatenalive3.midiplayer;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JPanel;

public class RenderSong extends JPanel {
	private static final long serialVersionUID = 1L;
	int switchTimer = 7;

	public static final int WIDTH = 650, HEIGHT = 300;

	BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	final int[] pixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();

	public RenderSong() {
		System.out.println(this.getWidth() + ", " + this.getHeight());
		this.setSize(WIDTH, HEIGHT);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.drawImage(canvas, 0, 0, WIDTH, HEIGHT, null);
	}

	public void update(int[] pix) {
		for (int i = 0; i < pix.length; i++) {
			if (pix[i] > 0)
				pix[i]--;
		}
		if (switchTimer-- > 0)
			return;
		switchTimer = MidiPlayer.currentSong.resolution / 50;

		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH - 1; x++) {
				pixels[x + y * WIDTH] = pixels[x + y * WIDTH + 1];
			}
		}
		for (int y = 0; y < pix.length; y++) {
			if (y < HEIGHT) {
				if (pix[y] != 0)
					pixels[WIDTH - 1 + y * WIDTH] = 0xFFFFFF;
				else
					pixels[WIDTH - 1 + y * WIDTH] = 0;
			}
		}
	}
}
