package org.dragonskulle.audio;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This will hold all the information needed for Clips
 * @author low101043
 *
 */
public class ClipClass {
	
	private Clip clip;
	private BooleanControl mute;
	private FloatControl volume;
	private int currentVol;
	private boolean looping;
	
	public static final Logger LOGGER = Logger.getLogger("audio");
	
	
	public ClipClass(Mixer mixer, boolean loopContinuously) throws LineUnavailableException {
		
		DataLine.Info dataLine = new DataLine.Info(Clip.class, null);
		clip = (Clip) mixer.getLine(dataLine);
		System.out.println("Making");
		try {
			AudioInputStream startingStream = AudioSystem.getAudioInputStream(new File("Silent.wav").getAbsoluteFile());
			clip.open(startingStream);
			
		} catch (UnsupportedAudioFileException e) {
			// TODO Log error (And Cry)  It shouldn't get here cos silent.wav WILL EXIST
			System.out.println("ERROR");
		} catch (IOException e) {
			// TODO Log Error (And Cry)  It shouldn't get here cos silent.wav WILL EXIST
			System.out.println("ERROR1");
		} 
		
		Control[] controls = clip.getControls();
        
        for (Control y : controls) {
        	//System.out.println(y.toString());
        	LOGGER.log(Level.INFO, y.toString());
        }
		
		mute = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
		mute.setValue(false);
		
		volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		
		currentVol = 0;
		
		setVolume(50);
		
		if (loopContinuously) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		looping = loopContinuously;
		
		
	}
	
	public void setMute(boolean muteValue) {
		mute.setValue(muteValue);
	}
	
	public boolean getMute() {
		return mute.getValue();
	}
	
	public void setVolume(int newVolume) {
		
		if (newVolume < 0) {
			newVolume = 0;
		}
		else if (newVolume > 100) {
			newVolume = 100;
		}
		
		//System.out.println("We got here");
		
		currentVol = newVolume;
		
		float amountOfVolForClip = Math.abs(volume.getMaximum()) + Math.abs(volume.getMinimum());
		
		float newVol = (((float) newVolume / 100) * amountOfVolForClip) + volume.getMinimum();
		
	
		
		volume.setValue(newVol);
	}
	
	public int getVolume() {
		return currentVol;
	}
	
	public boolean getLooping() {
		return looping;
	}
	
	public Clip play(AudioInputStream audio) {
		clip.close();
		try {
			clip.open(audio);
		} catch (LineUnavailableException e) {
			//TODO Log
		} catch (IOException e) {
			//TODO Log
		}
		clip.setMicrosecondPosition(0);
		clip.start();
		
		if (clip.isActive() && looping) {
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}
		
		return clip;
		
		
	}
	

}
