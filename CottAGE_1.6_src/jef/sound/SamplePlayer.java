package jef.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * SoundPool.java
 * 
 * 
 * Created 28-sep-2003
 * @author Erik Duijs
 */
public class SamplePlayer {

//	private javax.sound.sampled.Line.Info lineInfo;

	private Vector<AudioFormat> afs = new Vector<AudioFormat>();
	private Vector<Integer> sizes = new Vector<Integer>();
	private Vector<Info> infos = new Vector<Info>();
	private Vector<byte[]> audios = new Vector<byte[]>();
	private Vector<String> urls = new Vector<String>();
	private Vector<Clip> clips = new Vector<Clip>();
	private int num = 0;


	public void loadSound(String s) throws IOException, UnsupportedAudioFileException, LineUnavailableException, FileNotFoundException {
		System.out.println("loadSound(" + s + ")");
		if (getIndex(s) >= 0) return;
		URL url = getClass().getResource(s);
		if (url == null) throw new FileNotFoundException();
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(loadStream(url.openStream()));
		AudioFormat af = audioInputStream.getFormat();
		int size = (int) (af.getFrameSize() * audioInputStream.getFrameLength());
		byte[] audio = new byte[size];
		DataLine.Info info = new DataLine.Info(Clip.class, af, size);
		audioInputStream.read(audio, 0, size);
		
		urls.add(s);
		afs.add(af);
		sizes.add(new Integer(size));
		infos.add(info);
		audios.add(audio);
		
		Clip clip = (Clip) AudioSystem.getLine(info);
		clip.open(af,audio,0,size);

		clips.add(clip);
		
		
		num++;
	}

	private ByteArrayInputStream loadStream(InputStream inputstream)
	throws IOException {
		ByteArrayOutputStream bytearrayoutputstream =
			new ByteArrayOutputStream();
		byte data[] = new byte[1024];
		for (int i = inputstream.read(data);
		i != -1;
		i = inputstream.read(data))
			bytearrayoutputstream.write(data, 0, i);

		inputstream.close();
		bytearrayoutputstream.close();
		data = bytearrayoutputstream.toByteArray();
		return new ByteArrayInputStream(data);
	}
	
	private void playSound(int x) throws UnsupportedAudioFileException, LineUnavailableException {
		if (x > num || x < 0) {
			System.out.println(
					"playSound: sample nr[" + x + "] is not available");
		} else {
			Clip clip = clips.elementAt(x);
			if (!clip.isActive()) clip.setFramePosition(0);
			clip.start();
		}
	}
	
	private int getIndex(String s) {
		for (int i = 0; i < urls.size(); i++) {
			String u = urls.get(i);
			if (u.equals(s)) return i;
		}
		return -1;
	}
	
	
	public void playSound(String s) throws Exception {
		int i = getIndex(s);
		if (i < 0) {
			loadSound(s);
		}
		playSound(i);

	}


}

