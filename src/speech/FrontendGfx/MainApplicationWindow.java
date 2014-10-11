package speech.FrontendGfx;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import speech.DataAcquisition.ReadImage;

// addComponent function from http://www.java-forums.org/

public class MainApplicationWindow {

	DrawVocalTract drawCurrentVocalTract;
	DrawVocalTract drawTargetVocalTract;
	
	DrawLips drawCurrentLipShape;
	DrawLips drawTargetLipShape;
	
	DrawScrollingPhonemeGraph drawScrollingPhonemeGraph;
	
	public DrawScrollingSpectrum drawScrollingSpectrum;
	DrawScrollingSpectrumHistogram drawScrollingSpectrumHistogram;

	private JFrame masterFrame;
	
	public double vocalTractContour[][][];
	public double innerLipContour[][][];
	public double outerLipContour[][][];

	public MainApplicationWindow(int phonemes, int onscreenBins) {
		drawCurrentVocalTract = new DrawVocalTract(phonemes);
		drawTargetVocalTract = new DrawVocalTract(phonemes);
		drawCurrentLipShape = new DrawLips(phonemes);
		drawTargetLipShape = new DrawLips(phonemes);
		drawScrollingPhonemeGraph = new DrawScrollingPhonemeGraph(phonemes);
		drawScrollingSpectrum = new DrawScrollingSpectrum(480);
		drawScrollingSpectrumHistogram = new DrawScrollingSpectrumHistogram(onscreenBins);
		
		ReadImage readImage = new ReadImage();
		try {
			vocalTractContour = readImage.readTract(); 				// Read in data from images of lip
			innerLipContour = readImage.readInnerLipContour(); 			//		and vocal tract shapes
			outerLipContour = readImage.readOuterLipContour();			// 		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void makeMaster() {

		JFrame frame = new JFrame("JR Speech Analysis Toolbox");
		frame.setSize(1320, 800);
		frame.setLayout(null);

		class MyPanel extends JPanel {

			/**
			 * 
			 */
			private Image offScreenImage;
			private Dimension screenSize;
			private Graphics offScreenGraphics;

			private static final long serialVersionUID = 1L;

			MyPanel() {
				setDoubleBuffered(true);
			}

			@Override
			public void paint(Graphics g) {
				if (offScreenGraphics == null || !getSize().equals(screenSize)) {
					if (getSize().width == 0)
						return;
					int width = getWidth();
					int height = getHeight();
					screenSize = new Dimension(getSize());
					if (true) {
						offScreenImage = createImage(getSize().width,
								getSize().height);
						offScreenGraphics = offScreenImage.getGraphics();
						System.out.println("FRAME I just made some gfx");
					} else {
						// This makes no difference.
						GraphicsEnvironment env = GraphicsEnvironment
								.getLocalGraphicsEnvironment();
						GraphicsDevice device = env.getDefaultScreenDevice();
						GraphicsConfiguration config = device
								.getDefaultConfiguration();
						offScreenImage = config.createCompatibleImage(width,
								height, Transparency.TRANSLUCENT);
						System.out.println("FRAME I just made some gfx");
					}
					offScreenGraphics = offScreenImage.getGraphics();
					
				}
				//long t1 = System.nanoTime();
				
				super.paint(offScreenGraphics);
				
				//long t2 = System.nanoTime();
				//System.out.println("Render " + (t2 - t1) / 1e6);
				
				//long t3 = System.nanoTime();
				
				g.drawImage(offScreenImage, 0, 0, this);
				
				//long t4 = System.nanoTime();
				//System.out.println(" Blit " + (t4 - t3) / 1e6);

			}

		}

		JPanel content = new MyPanel();

		// Container content = frame.getContentPane();
		frame.setContentPane(content);
		content.setLayout(null);
		frame.addKeyListener(new KeyHandler());

		addComponent(content, drawCurrentLipShape, 680, 0, 320, 400);
		addComponent(content, drawCurrentVocalTract, 680, 400, 320, 400);
		addComponent(content, drawTargetLipShape, 1000, 0, 320, 400);
		addComponent(content, drawTargetVocalTract, 1000, 400, 320, 400);
		addComponent(content, drawScrollingPhonemeGraph, 0, 0, 360, 400);
		addComponent(content, drawScrollingSpectrum, 0, 400, 480, 400);
		addComponent(content, drawScrollingSpectrumHistogram, 480, 400, 200, 400);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		masterFrame = frame;

	}

	public void updateGfx(String text, double[] neuralNetOutputs, double[] audioSpectrum) {

		drawCurrentVocalTract.vectorMean(vocalTractContour, neuralNetOutputs, text);
		drawCurrentLipShape.vectorMean(innerLipContour, outerLipContour, neuralNetOutputs);
		
		drawScrollingPhonemeGraph.updateGraph(neuralNetOutputs, KeyHandler.scrollSpeed,
				KeyHandler.scrollTransparent, KeyHandler.scrollPause, text);
		drawScrollingSpectrumHistogram.update(audioSpectrum);
		masterFrame.repaint();

		drawTargetVocalTract.vectorMean(vocalTractContour, KeyHandler.targetNeuralOutputs, KeyHandler.text);
		drawTargetLipShape.vectorMean(innerLipContour, outerLipContour, KeyHandler.targetNeuralOutputs);
		
		drawScrollingSpectrum.notifyMoreDataReady(audioSpectrum);
		
	}

	private void addComponent(Container container, Component c, int x, int y,
			int width, int height) {
		c.setBounds(x, y, width, height);
		container.add(c);
	}

}

class KeyHandler extends KeyAdapter {

	static boolean scrollPause = true;
	static boolean scrollTransparent = false;
	static int scrollSpeed = 15;
	static double targetNeuralOutputs[] = new double[6];
	static String text = "";

	public void keyReleased(KeyEvent e) {

		int kCode = e.getKeyCode();

		if (kCode == KeyEvent.VK_SPACE) {
			if (scrollPause)
				scrollPause = false;
			else
				scrollPause = true;
		}

		if (kCode == KeyEvent.VK_EQUALS) {
			scrollSpeed += 5;
		}

		if (kCode == KeyEvent.VK_MINUS) {
			scrollSpeed -= 5;
		}

		if (kCode == KeyEvent.VK_T) {
			if (scrollTransparent)
				scrollTransparent = false;
			else
				scrollTransparent = true;
		}
		
		if (kCode == KeyEvent.VK_A) {
			targetNeuralOutputs = new double[6];
			targetNeuralOutputs[0] = 1.0;
			text = "EEE";
		}
		
		if (kCode == KeyEvent.VK_S) {
			targetNeuralOutputs = new double[6];
			targetNeuralOutputs[1] = 1.0;
			text = "EHH";
		}
		
		if (kCode == KeyEvent.VK_D) {
			targetNeuralOutputs = new double[6];
			targetNeuralOutputs[2] = 1.0;
			text = "ERR";
		}
		
		if (kCode == KeyEvent.VK_F) {
			targetNeuralOutputs = new double[6];
			targetNeuralOutputs[3] = 1.0;
			text = "AHH";
		}
		
		if (kCode == KeyEvent.VK_G) {
			targetNeuralOutputs = new double[6];
			targetNeuralOutputs[4] = 1.0;
			text = "OOH";
		}
		
		if (kCode == KeyEvent.VK_H) {
			targetNeuralOutputs = new double[6];
			targetNeuralOutputs[5] = 1.0;
			text = "UHH";
		}
		
	}
}
