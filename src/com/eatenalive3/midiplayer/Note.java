package com.eatenalive3.midiplayer;

import java.util.Locale;

public class Note {
	public double volume;
	public int note;
	public int trackNumber;
	public boolean noteOn;
	public int instrument;

	public Note(double v, int n, int tn, boolean non, int instr) {
		volume = v;
		note = n;
		trackNumber = tn;
		noteOn = non;
		instrument = instr;
	}

	public Note(int n) {
		volume = 1;
		note = n;
		trackNumber = 0;
		noteOn = true;
		instrument=0;
	}

	@Override
	public String toString() {
		int mod = note % 12;
		if (mod < 0)
			mod = 12 + mod;
		switch (mod) {
		case 0:
			return "C";
		case 1:
			return "C#";
		case 2:
			return "D";
		case 3:
			return "D#";
		case 4:
			return "E";
		case 5:
			return "F";
		case 6:
			return "F#";
		case 7:
			return "G";
		case 8:
			return "G#";
		case 9:
			return "A";
		case 10:
			return "A#";
		}
		return "B"; // always will be 11.
	}

	// example JSON stringify for 2 notes, 1 with delay: {"n":"c3","v":0.528},{"d":14,"n":"e3","v":0.528}
	public String toJSON(int delay) {
		String n = toString();
		String lastPart = String.format(Locale.US, "\"n\":\"" + n.toLowerCase().replace("#", "s") + (note / 12 - 1) + "\",\"v\":%.3f"
				+ (noteOn ? "" : ",\"s\":1") + "}", volume);
		if (delay == 0)
			return "{" + lastPart;
		return "{" + delay + "," + lastPart;
	}
}
