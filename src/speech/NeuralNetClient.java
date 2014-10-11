package speech;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import speech.AudioProcessing.SpectralAnalysisProcess;
import speech.AudioProcessing.SpectrumAdjust;
import speech.FrontendGfx.DrawScrollingSpectrum;
import uk.ac.bath.ai.backprop.BackProp;

public class NeuralNetClient {
	
	NeuralNet neuralNet;
	SpectrumAdjust spectrumAdjust;
	SpectralAnalysisProcess sprectralAnalysis;
	DrawScrollingSpectrum drawScrollingSpectrum;
	private double neuralNetOutputs[];
	double logarithmicAudio[];
	int fftSize;
	int frequencyBins;
	
	//public NeuralNetClient(int fftsize, int frequencyBins, DrawScrollingSpectrum scrollingSpect) {
	public NeuralNetClient(int fftsize, int frequencyBins) {
		
		this.frequencyBins = frequencyBins;
		this.fftSize=fftsize;
		spectrumAdjust = new SpectrumAdjust();
		//this.drawScrollingSpectrum=scrollingSpect;
		neuralNetOutputs = new double[6];
		
		FileInputStream ostr;
		try {
			ostr = new FileInputStream("src/textfiles/network.txt");
			ObjectInputStream in = new ObjectInputStream(ostr);
			neuralNet = (NeuralNet) in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	public void audioChunkReciever(double[] spectrum) {
		
		logarithmicAudio = spectrumAdjust.linearToLog(frequencyBins, fftSize, spectrum);
		logarithmicAudio = spectrumAdjust.smoothSpectrumRunningAverageOf3(frequencyBins, logarithmicAudio);
		logarithmicAudio = spectrumAdjust.changeVolume(2, logarithmicAudio);
		
		//if (drawScrollingSpectrum != null) drawScrollingSpectrum.notifyMoreDataReady(logarithmicAudio);
		
		neuralNetForwardPass(logarithmicAudio);
	}
	
	public void neuralNetForwardPass(double[] audioSpectrum){
		neuralNetOutputs = neuralNet.forwardPass(logarithmicAudio);
	}
	
	public double[] getNeuralNetworkOutputs() {
		return neuralNetOutputs;
	}
	
	public double[] getSmoothedFrequencySpectrum() {
		return logarithmicAudio;
	}
	
	public String getStrongestPhoneme() {
		
		double maxPhoneme = 0;
		String phonemeText = "";
		
		for (int i = 0; i < neuralNetOutputs.length; i ++) {
			if (neuralNetOutputs[i] > maxPhoneme && neuralNetOutputs[i] > 0.3) {
				maxPhoneme = neuralNetOutputs[i];
				if (i == 0) {phonemeText = "EEE";}
				if (i == 1) {phonemeText = "EHH";}
				if (i == 2) {phonemeText = "ERR";}
				if (i == 3) {phonemeText = "AHH";}
				if (i == 4) {phonemeText = "OOH";}
				if (i == 5) {phonemeText = "UHH";}
				}
		}
		return phonemeText;
	}
	
}
