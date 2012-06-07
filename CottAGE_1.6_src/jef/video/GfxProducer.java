package jef.video;

// import classes
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;

public abstract class GfxProducer extends javax.swing.JApplet implements
		Runnable, ImageProducer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9154718342186756092L;

	// data
	int _width;
	int _height;
	int _realheight;
	Image _image;
	Thread _thread;
	ImageConsumer _consumer;
	DirectColorModel _model;
	Graphics _graphics;
	boolean zoomed = false;

	public Thread getThread() {
		return this._thread;
	}

	@Override
	public int getWidth() {
		return this._width;
	}

	@Override
	public int getHeight() {
		return this._height;
	}

	public boolean isZoomed() {
		return this.zoomed;
	}

	public synchronized void update(BitMap bitmap) {
		update(bitmap.getPixels());
	}

	public synchronized void update(int[] pixels) {
		// check consumer
		if (_consumer != null) {
			// copy integer pixel data to image consumer
			_consumer.setPixels(0, 0, _width, _height, _model, pixels, 0,
					_width);

			// notify image consumer that the frame is done
			_consumer.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
		}

		// draw image to graphics context
		if (zoomed) {
			_graphics.drawImage(_image, 0, 0, _width * 2, _height * 2, 0, 0,
					_width, _height, null);
		} else {
			_graphics.drawImage(_image, 0, 0, _width, _height, null);
		}
		postPaint(_graphics);
	}

	@Override
	public void start() {

		// check thread
		if (_thread == null) {
			// create thread
			_thread = new Thread(this);

			// start thread
			_thread.start();
		}
	}

	@Override
	public void run() {

		zoomed = false;

		try {
			zoomed = getParameter("ZOOM").equals("Yes");
		} catch (Exception e) {
		}

		try {
			String parStr = getParameter("REALHEIGHT");
			if (parStr != null) {
				_realheight = Integer.parseInt(parStr);
			}
		} catch (Exception e) {
		}

		// get component size
		Dimension size = this.getSize();

		if (zoomed) {
			// setup data
			_width = size.width / 2;
			_height = _realheight / 2;
		} else {
			// setup data
			_width = size.width;
			_height = size.height;
		}

		// setup color model
		_model = new DirectColorModel(32, 0x00FF0000, 0x000FF00, 0x000000FF, 0);

		// create image using default toolkit
		_image = Toolkit.getDefaultToolkit().createImage(this);

		// get component graphics object
		_graphics = getGraphics();

		// call user main
		main(_width, _height);
	}

	@Override
	public void stop() {
		// null thread
		_thread = null;
	}

	public void postPaint(Graphics g) {
	}

	public void update() {
		if (_graphics != null) {
			_graphics.setColor(Color.black);
			if (zoomed) {
				_graphics.fillRect(0, 0, _width * 2, _height * 2);
			} else {
				_graphics.fillRect(0, 0, _width, _height);
			}
			postPaint(_graphics);
		}
	}

	@Override
	public synchronized void addConsumer(ImageConsumer ic) {
		// register image consumer
		_consumer = ic;

		// set image dimensions
		_consumer.setDimensions(_width, _height);

		// System.out.println("ic " + _width * _height);

		// set image consumer hints for speed
		_consumer.setHints(ImageConsumer.TOPDOWNLEFTRIGHT
				| ImageConsumer.COMPLETESCANLINES | ImageConsumer.SINGLEPASS
				| ImageConsumer.SINGLEFRAME);

		// set image color model
		_consumer.setColorModel(_model);
	}

	@Override
	public synchronized boolean isConsumer(ImageConsumer ic) {
		// check if consumer is registered
		return true;
	}

	@Override
	public synchronized void removeConsumer(ImageConsumer ic) {
		// remove image consumer
	}

	@Override
	public void startProduction(ImageConsumer ic) {
		// add consumer
		addConsumer(ic);
	}

	@Override
	public void requestTopDownLeftRightResend(ImageConsumer ic) {
		// ignore resend request
	}

	public void main(int width, int height) {
	}
}
