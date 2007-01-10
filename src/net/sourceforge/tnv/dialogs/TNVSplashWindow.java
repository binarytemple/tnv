
package net.sourceforge.tnv.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * @author jgood
 * created on: Dec 14, 2006
 */
public class TNVSplashWindow extends JDialog {

	private static final Color BG_COLOR = new Color(240,240,240);
	
	private TNVSplashWindow(String text, URL imgUrl, Frame f, int waitTime) {
		super(f);
		
		this.setAlwaysOnTop(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setUndecorated(true);
		
		JLabel titleLabel = new JLabel( "t n v" );
		titleLabel.setFont(new Font("SanSerif", Font.BOLD, 36));
		titleLabel.setAlignmentX( 0.5f );
		titleLabel.setBackground(BG_COLOR);
		
		JLabel imageLabel = new JLabel( new ImageIcon(imgUrl) );
		imageLabel.setAlignmentX( 0.5f );
		imageLabel.setBackground(BG_COLOR);
		
		JTextArea messageArea = new JTextArea();
		messageArea.setEditable(false);
		messageArea.setLineWrap(true);
		messageArea.setWrapStyleWord(true);
		messageArea.setMargin( new Insets(15,20,5,20) );
		messageArea.append(text);
		messageArea.setBackground(BG_COLOR);
		
		JPanel framePanel = new JPanel();
		framePanel.setBackground(BG_COLOR);
		framePanel.setLayout( new BoxLayout( framePanel, BoxLayout.Y_AXIS ) );
		framePanel.add(Box.createVerticalStrut(10));
		framePanel.add(titleLabel);
		framePanel.add(imageLabel);
		framePanel.add(messageArea);
		
		this.getContentPane().add( framePanel, BorderLayout.CENTER );
		
		this.pack();
		
		this.setSize( new Dimension( 400, 350 ) );
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ( (dim.getWidth() / 2) - (this.getWidth() / 2) );
		int y = (int) ( (dim.getHeight() / 2) - (this.getHeight() / 2) );
		this.setLocation(x, y);

		final Runnable closerRunner = new Runnable() {
			public void run() {
				setVisible(false);
				dispose();
			}
		};
		
		setVisible(true);
		
		final int pause = waitTime;
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(pause);
					SwingUtilities.invokeLater(closerRunner);
				}
				catch(Exception e) { }
			}
		}).start();
	}
	
	/**
	 * Factory constructor
	 * @param p
	 * @return dialog
	 * @throws HeadlessException
	 */
	public static TNVSplashWindow createTNVSplashWindow(String text, URL imgUrl, Frame f, int waitTime) 
			throws HeadlessException {
		return new TNVSplashWindow(text, imgUrl, f, waitTime);
	}

}
