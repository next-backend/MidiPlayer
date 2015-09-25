package com.eatenalive3.midiplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

public class Play {
	public static Synthesizer synthesizer;
	public static int instrument = 0;
	public static MidiChannel piano;

	public static boolean backwards = false;
	public static boolean invert = false;
	public static int multipleNotes = 0;
	public static int roundUpOrDown = 1;
	public static int lagBufferTime = 100;

	public static int[] pix = new int[RenderSong.HEIGHT];

	int[] keySignature = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	boolean[] keyIgnore = { false, false, false, false, false, false, false, false, false, false, false, false };

	public static BotSocket socket;

	private HashMap<Long, Long> tempo = new HashMap<Long, Long>();
	private HashMap<Long, List<Note>> notes = new HashMap<Long, List<Note>>();

	private long trackPosition = 0;
	private long maxTrackLength = 0;
	private double tempoMultiplier = 1;
	private long dynamicTempo;

	public final int resolution;

	public static boolean sustain = false;

	public int transpose = 0;

	public File mid;

	public Play(File mid, String ks) throws InvalidMidiDataException, IOException {
		this.mid = mid;

		Sequence sequence = MidiSystem.getSequence(mid);
		Track[] tracks = sequence.getTracks();

		resolution = sequence.getResolution();

		int[] songKeySig = new int[12];

		for (int trackNumber = 0; trackNumber < tracks.length; trackNumber++) {
			Track track = tracks[trackNumber];
			boolean pedal = false;
			double volumeControl = 1;

			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				MidiMessage message = event.getMessage();
				if (message instanceof MetaMessage) {
					byte[] msg = ((MetaMessage) message).getMessage();
					byte[] data = ((MetaMessage) message).getData();
					if (msg.length > 1 && msg[1] == 0x51) {
						long p1 = (data[0] & 0xFF) << 16;
						long p2 = (data[1] & 0xFF) << 8;
						long p3 = (data[2] & 0xFF);
						tempo.put(event.getTick(), (long) ((p1 + p2 + p3) * 1000 / resolution)); // divide by tempoMultiplier in realtime
					}
				} else if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					if (sm.getChannel() == 9) { // percussion channel -- ignore
						continue;
					}
					int cmd = sm.getCommand();
					if (cmd == 176) {
						if (sm.getData1() == 64) {
							pedal = sm.getData2() <= 64;
						} else if (sm.getData1() == 7) {
							// apparently people don't want this. Or at least, Karm doesn't. Lol
							//volumeControl = sm.getData2() / 127.0;
						}

					} else if (cmd == 0x90 || cmd == 0x80) {
						final long tick = event.getTick();
						if (event.getTick() > maxTrackLength)
							maxTrackLength = event.getTick();

						double vol = volumeControl * sm.getData2() / 127.0;

						if (vol == 0) // a bit of a hack, but eh :P
							cmd = 0x80;

						int key = sm.getData1();

						songKeySig[key % 12]++; // C is 0

						if (cmd != 0x80 || !pedal) {
							if (notes.get(tick) == null) {
								notes.put(tick, new ArrayList<Note>());
							}

							notes.get(tick).add(new Note(vol, key - 12, trackNumber, cmd == 0x90, ((ShortMessage) message).getChannel()));
						}
					}
				}
			}
		}

		// remove duplicate notes immediately in the list
		for (List<Note> noteList : notes.values()) {
			Note[] notez = noteList.toArray(new Note[0]);
			for (int i = notez.length - 1; i > 0; i--) {
				for (int j = i - 1; j >= 0; j--) {
					if (notez[i].note == notez[j].note) {
						if (notez[i].noteOn)
							noteList.remove(notez[j]);
						else
							noteList.remove(notez[i]);
						break;
					}
				}
			}
		}

		Object[] sig = getKeySignature(songKeySig);

		MidiPlayer.log((String) sig[0]);

		// don't mess with anything beyond here.
		char type;
		int value = 0, valueOriginal = 0;

		Note n = (Note) sig[1];
		String cont = n.toString();
		type = Character.toLowerCase(cont.charAt(0));
		int sharpOrFlat = cont.contains("#") ? 1 : 0;
		boolean major = n.trackNumber == 1;

		switch (type) {
		case 'c':
			value = 0;
			break;
		case 'd':
			value = 2;
			break;
		case 'e':
			value = 4;
			break;
		case 'f':
			value = 5;
			break;
		case 'g':
			value = 7;
			break;
		case 'a':
			value = 9;
			break;
		case 'b':
			value = 11;
			break;
		default:
			value = 1;
		}
		value += sharpOrFlat;
		if (!major) {
			int[] base = { 1, 3, 6, 8, 10 };
			for (int i = 0; i < 5; i++) {
				int poss = value + base[i];
				if (poss > 11)
					poss -= 12;
				if (poss < 0)
					poss += 12;
				keySignature[poss] = 1;
			}
		} else {
			int[] base = { 2, 4, 6, 9, 11 };
			for (int i = 0; i < 5; i++) {
				int poss = value + base[i];
				if (poss > 11)
					poss -= 12;
				if (poss < 0)
					poss += 12;
				keySignature[poss] = 1;
			}
		}
		switch (type) {
		case 'c':
			valueOriginal = 0;
			break;
		case 'd':
			valueOriginal = 2;
			break;
		case 'e':
			valueOriginal = 4;
			break;
		case 'f':
			valueOriginal = 5;
			break;
		case 'g':
			valueOriginal = 7;
			break;
		case 'a':
			valueOriginal = 9;
			break;
		case 'b':
			valueOriginal = 11;
			break;
		default:
			valueOriginal = 1;
		}
		valueOriginal += sharpOrFlat;
		if (major) {// TODO
			int[] base = { 1, 3, 6, 8, 10 };
			for (int i = 0; i < 5; i++) {
				int poss = valueOriginal + base[i];
				if (poss > 11)
					poss -= 12;
				if (poss < 0)
					poss += 12;
				keyIgnore[poss] = true;
			}
		} else {
			int[] base = { 2, 4, 6, 9, 11 };
			for (int i = 0; i < 5; i++) {
				int poss = valueOriginal + base[i];
				if (poss > 11)
					poss -= 12;
				if (poss < 0)
					poss += 12;
				keyIgnore[poss] = true;
			}
		}
	}

	public void play(double tmpo, double offset, int transp) {
		offset = Math.max(0, Math.min(offset, 1));
		if (tmpo == 0)
			tmpo = 0.01;

		dynamicTempo = (long) (500000 * 1000 / resolution); // used so that the tempo can change without any issues

		transpose = transp;

		tempoMultiplier = tmpo;

		setPosition(offset);

		long now = System.nanoTime(), previous = now, elapsed = 0;

		long nanoPerFrame = (long) (dynamicTempo / tempoMultiplier);

		while (trackPosition < maxTrackLength + 1) {
			now = System.nanoTime();
			elapsed += now - previous;
			previous = now;
			while (elapsed > nanoPerFrame) {
				MidiPlayer.setSlider(trackPosition * 100.0 / maxTrackLength);
				elapsed -= nanoPerFrame;
				nanoPerFrame = (long) (dynamicTempo / tempoMultiplier);

				long tpos = backwards ? maxTrackLength - trackPosition++ : trackPosition++;
				List<Note> noteList = notes.get(tpos);

				if (tempo.get(tpos) != null) {
					dynamicTempo = tempo.get(tpos);
					nanoPerFrame = (long) (dynamicTempo / tempoMultiplier);
				}

				if (noteList == null)
					continue;

				String send = "";
				for (Note note : noteList) {
					int n = note.note;
					int not = n % 12;
					if (not < 0)
						not = 12 + not;
					if (invert && !keyIgnore[not % 12])
						n += roundUpOrDown * keySignature[not % 12];
					n += transpose;
					Note transposed = new Note(note.volume, n, note.trackNumber, note.noteOn, note.instrument);
					if ((sustain && !backwards) || transposed.noteOn) { // !backwards because sustain messes up otherise
						send += "," + transposed.toJSON(0);
					}
					if (n < 100 && n > 0) { // good to have some bounds checking just in case
						if (transposed.noteOn ^ backwards)
							pix[200 - 2 * n] = 1000;
						else
							pix[200 - 2 * n] = 0;
					}
				}
				if (!send.isEmpty()) {
					String snd = send;
					for (int i = 0; i < multipleNotes; i++) {
						snd += send;
					}
					socket.send(String.format(Locale.US, "[{\"m\":\"n\",\"t\":%d,\"n\":[%s]}]", System.currentTimeMillis() + BotSocket.serverTimeOffset
							+ lagBufferTime, snd.substring(1)));
				}
			}
			Thread.yield();
		}
		if (trackPosition != maxTrackLength + 1000) {
			MidiPlayer.resetPlay();
		}
	}

	public void setTempo(double amt) {
		if (amt == 0)
			amt = 0.01;
		tempoMultiplier = amt;
	}

	public void stop() {
		trackPosition = maxTrackLength + 1000;
		pix = new int[RenderSong.HEIGHT];
	}

	public void setPosition(double pos) {
		trackPosition = (long) (maxTrackLength * Math.max(0, Math.min(1, pos)));

		for (long i = trackPosition; i >= 0; i--) {
			long tpos = backwards ? maxTrackLength - trackPosition : trackPosition;
			if (tempo.get(tpos) != null) {
				dynamicTempo = tempo.get(tpos);
				break;
			}
		}
		pix = new int[RenderSong.HEIGHT];
	}

	public static Object[] getKeySignature(int[] notes) {
		int total = 0;

		boolean[] song = new boolean[12];
		for (int q = 0; q < 5; q++) {
			int min = 0;
			for (int i = 0; i < notes.length; i++) {
				total += notes[i];
				if (notes[i] < notes[min] && !song[i]) {
					min = i;
				}
			}
			song[min] = true;
		}

		if (total == 0) {
			return new Object[] { "Could not determine song length.", new Note(0), 0 };
		}
		for (int i = 0; i < notes.length; i++) {
			song[i] = !song[i];
		}

		for (int i = 0; i < 12; i++) {
			if (song[i] && song[(i + 2) % 12] && song[(i + 4) % 12] && song[(i + 5) % 12] && song[(i + 7) % 12] && song[(i + 9) % 12] && song[(i + 11) % 12]) {
				double proportion = ((notes[(i + 12 - 4) % 12]) * 1.0 / total);
				Note maj = new Note(i);
				Note min = new Note((i + 12 - 3) % 12);
				if (proportion < 0.5 / 100.0) {
					return new Object[] {
							"I think the key signature is " + maj + " major. (minorness: " + String.format(Locale.US, "%.1f%%)", proportion * 10000 / 0.5),
							maj, (Integer) i };
				}

				return new Object[] {
						"I think the key signature is " + min + " minor. (minorness: " + String.format(Locale.US, "%.1f%%)", proportion * 10000 / 0.5), min,
						(Integer) i };
			}
		}

		// oh noes, that didn't work. Let's try it a different way.

		int[] sums = new int[12];
		for (int i = 0; i < 12; i++) {
			for (int q = 0; q < 12; q++) {
				if (keySig[0][i][q]) {
					sums[i] += notes[i];
				}
			}
		}
		int max = 0;
		for (int i = 0; i < 12; i++) {
			System.out.println(sums[i]);
			if (sums[i] > sums[max]) {
				max = i;
			}
		}

		double proportion = ((notes[(max + 12 - 3) % 12]) * 1.0 / total);
		Note maj = new Note((max) % 12);
		Note min = new Note((max + 12 - 3) % 12);
		double confidence = sums[max] * 100.0 / total;
		if (proportion < 0.5 / 100.0) {
			return new Object[] {
					"[LOW " + String.format(Locale.US, "%.1f%%", confidence) + " CONFIDENCE] I think the key signature is " + maj + " major. (minorness: "
							+ String.format(Locale.US, "%.1f%%)", proportion * 10000 / 0.5), maj, (Integer) max };
		}

		return new Object[] {
				"[LOW " + String.format(Locale.US, "%.1f%%", confidence) + " CONFIDENCE] I think the key signature is " + min + " minor. (minorness: "
						+ String.format(Locale.US, "%.1f%%)", proportion * 10000 / 0.5), min, (Integer) max };

	}

	public static boolean[][][] keySig = new boolean[2][12][12];

	static {
		// generate Major and Minor key signatures
		final int[] minorBase = { 1, 3, 6, 8, 10 };
		final int[] majorBase = { 2, 4, 6, 9, 11 };
		for (int i = 0; i < 12; i++) {
			for (int j = 0; j < 5; j++) {
				int poss = i + majorBase[j];
				if (poss > 11)
					poss -= 12;
				if (poss < 0)
					poss += 12;
				keySig[0][i][poss] = true;
			}
			for (int j = 0; j < 5; j++) {
				int poss = i + minorBase[j];
				if (poss > 11)
					poss -= 12;
				if (poss < 0)
					poss += 12;
				keySig[1][i][poss] = true;
			}
		}
	}

	public void playLocally(double tmpo, double offset, int transp) {
		offset = Math.max(0, Math.min(offset, 1));
		if (tmpo == 0)
			tmpo = 0.01;

		dynamicTempo = (long) (500000 * 1000 / resolution); // used so that the tempo can change without any issues

		transpose = transp;

		tempoMultiplier = tmpo;

		setPosition(offset);

		long now = System.nanoTime(), previous = now, elapsed = 0;

		long nanoPerFrame = (long) (dynamicTempo / tempoMultiplier);

		while (trackPosition < maxTrackLength + 1) {
			now = System.nanoTime();
			elapsed += now - previous;
			previous = now;
			while (elapsed > nanoPerFrame) {
				MidiPlayer.setSlider(trackPosition * 100.0 / maxTrackLength);
				elapsed -= nanoPerFrame;
				nanoPerFrame = (long) (dynamicTempo / tempoMultiplier);

				long tpos = backwards ? maxTrackLength - trackPosition++ : trackPosition++;
				List<Note> noteList = notes.get(tpos);

				if (tempo.get(tpos) != null) {
					dynamicTempo = tempo.get(tpos);
					nanoPerFrame = (long) (dynamicTempo / tempoMultiplier);
				}

				if (noteList == null)
					continue;

				String send = "";
				for (Note note : noteList) {
					int n = note.note;
					int not = n % 12;
					if (not < 0)
						not = 12 + not;
					if (invert && !keyIgnore[not % 12])
						n += roundUpOrDown * keySignature[not % 12];
					n += transpose + 12; // add an octave because it's just not right
					Note transposed = new Note(note.volume, n, note.trackNumber, note.noteOn, note.instrument);
					if ((sustain && !backwards) || transposed.noteOn) { // !backwards because sustain messes up otherise
						send += "," + transposed.toJSON(0);
					}
					if (n < 100 && n > 0) { // good to have some bounds checking just in case
						if (transposed.noteOn ^ backwards)
							pix[200 - 2 * n] = 1000;
						else
							pix[200 - 2 * n] = 0;
					}
					piano.programChange(synthesizer.getAvailableInstruments()[note.instrument].getPatch().getProgram());
					if (transposed.noteOn)
						piano.noteOn(transposed.note, (int) (transposed.volume * 127));
					else
						piano.noteOff(transposed.note);
				}
			}
			Thread.yield();
		}
		if (trackPosition != maxTrackLength + 1000) {
			MidiPlayer.resetPlay();
		}
	}
}
