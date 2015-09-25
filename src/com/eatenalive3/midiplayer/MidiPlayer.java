package com.eatenalive3.midiplayer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import javax.sound.midi.MidiSystem;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSliderUI;

public class MidiPlayer {
	private static boolean mouseDown = false;

	public static RenderSong rs = new RenderSong();

	private JFrame frame;
	private JTextArea room;
	private File currentMid;

	public static JSlider trackPosition;

	private static JButton play;
	private JSlider tempo;
	private JSlider transpose;

	private JCheckBox backwards;
	private JCheckBox sustain;
	private JCheckBox invertRound;
	private JCheckBox invert;
	private JCheckBox multipleNotes;

	public static JTextArea console;

	public static BotSocket currentSocket = null;

	private String currentRoom = null;

	public static Play currentSong = null;

	public static void main(String args[]) {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		try {
			Play.synthesizer = MidiSystem.getSynthesizer();
			Play.synthesizer.open();

			Play.synthesizer.getChannels()[0].programChange(Play.synthesizer.getAvailableInstruments()[0].getPatch().getProgram());
			
			Play.piano = Play.synthesizer.getChannels()[0];
		} catch (Exception e) {}

		MidiPlayer gui = new MidiPlayer();

		gui.setUpGUI();

		console.setText("MidiPlayer BETA v1.1 started. Coded by Boss :P");
		log("Remember to connect to a room first!");

	}

	public void setUpGUI() {
		frame = new JFrame("MultiplayerPiano Midi Player BETA");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(670, 700);
		frame.setLocationRelativeTo(null);

		JPanel pane = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		frame.add(pane);

		new FileDrop(pane, new FileDrop.Listener() {
			public void filesDropped(File[] files) {
				for (File file : files) {
					try {
						MidiSystem.getSequence(file);
						currentMid = file;
						resetPlay();
						log("Successfully loaded " + file.getName());
						return;
					} catch (Exception e) {
					}
				}
				log("Could not load any midis from the dropped files.");
			}
		});

		JButton choose = new JButton("Choose File (or drag&drop)");
		choose.addActionListener(new Choose());

		final JButton connect = new JButton("Connect");
		connect.addActionListener(new Connect());

		final String defaultText = "Room name or URL";
		room = new JTextArea(defaultText, 1, 30);

		room.setMinimumSize(new Dimension(1, 25));
		room.setPreferredSize(new Dimension(1, 30));
		room.setFocusable(false);

		room.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					connect.getActionListeners()[0].actionPerformed(null);
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		room.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				if (room.getText().equals(defaultText)) {
					room.setText("");
				}
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

		});

		room.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "none"); // blocks enter from having an effect but still works with keyListener

		JPanel roomPanel = new JPanel(new GridBagLayout());

		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;

		roomPanel.add(new JLabel("Room Name: "), c);

		JPanel roomPan = new JPanel();
		roomPan.add(room);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		roomPanel.add(roomPan, c);

		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 1 + 1;
		c.weightx = .1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pane.add(roomPanel, c);

		c.gridy = 0;
		JPanel topSlider = new JPanel(new BorderLayout());
		transpose = new JSlider() {
			{
				MouseListener[] listeners = getMouseListeners();
				for (MouseListener l : listeners)
					removeMouseListener(l); // remove UI-installed TrackListener
				final BasicSliderUI ui = (BasicSliderUI) getUI();
				BasicSliderUI.TrackListener tl = ui.new TrackListener() {
					// this is where we jump to absolute value of click
					@Override
					public void mouseClicked(MouseEvent e) {
						Point p = e.getPoint();
						int value = ui.valueForXPosition(p.x);

						setValue(value);
					}

					// disable check that will invoke scrollDueToClickInTrack
					@Override
					public boolean shouldScroll(int dir) {
						return false;
					}
				};
				addMouseListener(tl);
			}
		};
		transpose.setMinimum(-12);
		transpose.setMaximum(12);
		transpose.setMajorTickSpacing(12);
		transpose.setMinorTickSpacing(1);
		transpose.setPaintLabels(true);
		transpose.setPaintTicks(true);
		transpose.setValue(0);
		transpose.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (currentSong != null)
					currentSong.transpose = transpose.getValue();
				Play.pix = new int[RenderSong.HEIGHT];
			}

		});

		topSlider.add(transpose, BorderLayout.SOUTH);
		topSlider.setBorder(new TitledBorder(new EtchedBorder(), "Transpose"));
		c.gridwidth = 3;
		pane.add(topSlider, c);

		c.gridy = 2;
		c.fill = GridBagConstraints.CENTER;
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = .9;
		pane.add(connect, c);

		// pane.add(javax.swing.Box.createGlue(), c);

		JPanel selectButtons = new JPanel();

		play = new JButton("Play");
		play.addActionListener(new Playing());

		selectButtons.add(choose);

		backwards = new JCheckBox("Backwards");
		backwards.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Play.backwards = backwards.isSelected();
				Play.pix = new int[RenderSong.HEIGHT];
			}
		});
		selectButtons.add(backwards);
		invert = new JCheckBox("Invert");
		invert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Play.invert = invert.isSelected();
				Play.pix = new int[RenderSong.HEIGHT];
			}
		});
		selectButtons.add(invert);
		sustain = new JCheckBox("Sustain");
		sustain.setSelected(true);
		sustain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Play.sustain = !sustain.isSelected();
			}
		});
		selectButtons.add(sustain);
		invertRound = new JCheckBox("Invert: Round Down");
		invertRound.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Play.roundUpOrDown = invertRound.isSelected() ? -1 : 1;
				Play.pix = new int[RenderSong.HEIGHT];
			}
		});
		selectButtons.add(invertRound);
		final JCheckBox lessLag = new JCheckBox("Less lag");
		lessLag.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Play.lagBufferTime = lessLag.isSelected() ? 1000 : 100;
			}
		});
		selectButtons.add(lessLag);

		c.gridx = 0;
		c.gridy = 1 + 2;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 3;
		pane.add(selectButtons, c);

		c.gridy = 1 + 4;
		c.gridwidth = 3;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		JPanel tempoBar = new JPanel();
		tempo = new JSlider() {
			{
				MouseListener[] listeners = getMouseListeners();
				for (MouseListener l : listeners)
					removeMouseListener(l); // remove UI-installed TrackListener
				final BasicSliderUI ui = (BasicSliderUI) getUI();
				BasicSliderUI.TrackListener tl = ui.new TrackListener() {
					// this is where we jump to absolute value of click
					@Override
					public void mouseClicked(MouseEvent e) {
						Point p = e.getPoint();
						int value = ui.valueForXPosition(p.x);

						setValue(value);
					}

					// disable check that will invoke scrollDueToClickInTrack
					@Override
					public boolean shouldScroll(int dir) {
						return false;
					}
				};
				addMouseListener(tl);
			}
		};
		tempo.setMinimum(0);
		tempo.setMaximum(300);
		tempo.setValue(100);
		tempo.setMajorTickSpacing(100);
		tempo.setMinorTickSpacing(5);
		tempo.setPaintTicks(true);
		tempo.setSnapToTicks(true);
		tempo.setPaintLabels(true);

		tempo.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (currentSong != null)
					currentSong.setTempo(tempo.getValue() / 100.0);
			}

		});
		tempoBar.setLayout(new BorderLayout());
		tempoBar.add(tempo);
		tempoBar.setBorder(new TitledBorder(new EtchedBorder(), "Tempo"));
		pane.add(tempoBar, c);

		c.gridy++;
		c.gridwidth = 3;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		JPanel multiPanel = new JPanel();
		final JSlider multi = new JSlider() {
			{
				MouseListener[] listeners = getMouseListeners();
				for (MouseListener l : listeners)
					removeMouseListener(l); // remove UI-installed TrackListener
				final BasicSliderUI ui = (BasicSliderUI) getUI();
				BasicSliderUI.TrackListener tl = ui.new TrackListener() {
					// this is where we jump to absolute value of click
					@Override
					public void mouseClicked(MouseEvent e) {
						Point p = e.getPoint();
						int value = ui.valueForXPosition(p.x);

						setValue(value);
					}

					// disable check that will invoke scrollDueToClickInTrack
					@Override
					public boolean shouldScroll(int dir) {
						return false;
					}
				};
				addMouseListener(tl);
			}
		};
		multi.setMinimum(1);
		multi.setMaximum(5);
		multi.setValue(0);
		multi.setMajorTickSpacing(1);
		multi.setPaintTicks(true);
		multi.setSnapToTicks(true);
		multi.setPaintLabels(true);

		multi.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				Play.multipleNotes = multi.getValue() - 1;
			}

		});
		multiPanel.setLayout(new BorderLayout());
		multiPanel.add(multi);
		multiPanel.setBorder(new TitledBorder(new EtchedBorder(), "Multi-note"));
		pane.add(multiPanel, c);

		JPanel trackBar = new JPanel();
		trackBar.setLayout(new BorderLayout());

		c.gridy = 1 + 3;
		trackPosition = new JSlider() {
			{
				MouseListener[] listeners = getMouseListeners();
				for (MouseListener l : listeners)
					removeMouseListener(l); // remove UI-installed TrackListener
				final BasicSliderUI ui = (BasicSliderUI) getUI();
				BasicSliderUI.TrackListener tl = ui.new TrackListener() {
					// this is where we jump to absolute value of click
					@Override
					public void mouseClicked(MouseEvent e) {
						Point p = e.getPoint();
						int value = ui.valueForXPosition(p.x);

						setValue(value);
					}

					// disable check that will invoke scrollDueToClickInTrack
					@Override
					public boolean shouldScroll(int dir) {
						return false;
					}
				};
				addMouseListener(tl);
			}
		};
		trackPosition.setMaximum(100);
		trackPosition.setMinimum(0);
		trackPosition.setValue(0);
		trackPosition.setMajorTickSpacing(50);
		trackPosition.setMinorTickSpacing(10);
		trackPosition.setPaintTicks(true);

		trackPosition.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				mouseDown = true;
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				mouseDown = false;
				if (currentSong != null)
					currentSong.setPosition(trackPosition.getValue() / 100.0);
				Play.pix = new int[RenderSong.HEIGHT];
			}

		});

		trackBar.add(trackPosition, BorderLayout.NORTH);
		trackBar.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridBagLayout());
		GridBagConstraints cc = new GridBagConstraints();

		JButton stop = new JButton("Stop");
		stop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (currentSong != null) {
					currentSong.stop();
					trackPosition.setValue(0);
					play.setText("Play");
				}
			}

		});
		buttons.add(stop, cc);
		buttons.add(play, cc);
		trackBar.add(buttons);

		pane.add(trackBar, c);

		JPanel bottom = new JPanel();
		bottom.setBorder(new TitledBorder(new EtchedBorder(), "Display"));

		console = new JTextArea(1, 0);
		console.setEditable(false);
		JScrollPane scroll = new JScrollPane(console);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setAutoscrolls(true);

		bottom.setLayout(new BorderLayout());
		bottom.add(rs);
		c.weighty = 1;

		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridwidth = 3;
		c.gridy = 7;
		// c.anchor = GridBagConstraints.SOUTH;
		pane.add(bottom, c);

		c.weighty = 0.3;
		JPanel consoleBottom = new JPanel();
		consoleBottom.setBorder(new TitledBorder(new EtchedBorder(), "Console"));

		consoleBottom.setLayout(new BorderLayout());
		consoleBottom.add(scroll);
		c.gridy++;
		pane.add(consoleBottom, c);

		frame.setVisible(true);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				room.setFocusable(true);
			}
		});
	}

	public void getMidiFile() {
		JFileChooser chooser;
		if (currentMid == null)
			chooser = new JFileChooser();
		else
			chooser = new JFileChooser(currentMid);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("MIDI files", "mid", "midi");
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			log("Loaded " + chooser.getSelectedFile().getName());
			currentMid = chooser.getSelectedFile();
			return;
		}
		log("Did not load any file. (approve option not pressed)");
	}

	class Choose implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			getMidiFile();
		}

	}

	class Playing implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (currentMid != null && currentRoom != null) {
				if (play.getText().equals("Play")) {
					play.setText("Pause");
					try {
						new Thread(new Runnable() {
							public void run() {
								try {
									if (currentSong != null)
										currentSong.stop();
									if (currentSong == null || !currentSong.mid.equals(currentMid))
										currentSong = new Play(currentMid, null);
									currentSong.play(tempo.getValue() / 100.0, trackPosition.getValue() / 100.0, transpose.getValue());

									// com.eatenalive3.midiplayer.Play.playSong(currentMid, 1, null, 0);
								} catch (Exception e) {
									log("Did not play mid. You are not connected to a room.");
									e.printStackTrace();
								} finally {
									play.setText("Play");
								}
							}
						}).start();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					play.setText("Play");
					if (currentSong == null) {
						log("No song loaded.");
					} else {
						currentSong.stop();
					}
				}

			} else {
				if (currentMid == null) {
					log("Did not play mid:");
					log("No midi file loaded!");
				} else {
					log("Not connected to a room, so playing locally.");
					if (play.getText().equals("Play")) {
						play.setText("Pause");
						try {
							new Thread(new Runnable() {
								public void run() {
									try {
										if (currentSong != null)
											currentSong.stop();
										if (currentSong == null || !currentSong.mid.equals(currentMid))
											currentSong = new Play(currentMid, null);
										currentSong.playLocally(tempo.getValue() / 100.0, trackPosition.getValue() / 100.0, transpose.getValue());
									} catch (Exception e) {
										log("Did not play mid. You are not connected to a room.");
										e.printStackTrace();
									} finally {
										play.setText("Play");
									}
								}
							}).start();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						play.setText("Play");
						if (currentSong == null) {
							log("No song loaded.");
						} else {
							currentSong.stop();
						}
					}
				}
			}
		}

	}

	class Connect implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String text = room.getText();
			String mpp = "www.multiplayerpiano.com/";
			if (text.contains(mpp)) {
				text = text.substring(text.indexOf(mpp) + mpp.length());
			}
			try {
				text = java.net.URLDecoder.decode(text, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
				log(e1.toString());
			}
			if (!text.equals(currentRoom)) {
				if (currentSocket != null) {
					currentSocket.send("[{\"m\":\"ch\",\"_id\":\"" + text + "\"}]");
				} else {
					try {
						currentSocket = new BotSocket(text);
					} catch (URISyntaxException e) {
						e.printStackTrace();
						log(e.toString());
					}
				}
				currentRoom = text;
				log("Connected to " + text);
			} else {
				log("You are already connected to " + text);
			}

		}

	}

	public static void log(String s) {
		console.setText(console.getText() + "\n" + s);
	}

	public static void setSlider(double pos) {
		MidiPlayer.rs.update(Play.pix);
		MidiPlayer.rs.repaint();
		if (!mouseDown && pos < 100) {
			trackPosition.setValue((int) pos);
		}
	}

	public static void resetPlay() {
		Play.pix = new int[RenderSong.HEIGHT];
		if (currentSong != null)
			currentSong.stop();
		play.setText("Play");
		trackPosition.setValue(0);
	}
}