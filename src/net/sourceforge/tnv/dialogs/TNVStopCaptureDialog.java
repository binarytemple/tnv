
package net.sourceforge.tnv.dialogs;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.sourceforge.jpcap.capture.PacketCapture;
import net.sourceforge.tnv.TNV;
import net.sourceforge.tnv.util.SwingWorker;

/**
 * @author jgood
 * created on: Dec 11, 2007
 */
public class TNVStopCaptureDialog extends JDialog {

	private long startTime;
	
	private JLabel timeLabel;
	private JLabel receivedLabel;
	private JLabel droppedLabel;
	
	private boolean done = false;
	
	private PacketCapture pcap;
	private TNV parent;
 
	/**
	 * Constructor
	 * @param title
	 * @throws java.awt.HeadlessException
	 */
	private TNVStopCaptureDialog(TNV p, PacketCapture pcap) throws HeadlessException {
		super(p, "Capturing Packets");
		this.parent = p;
		this.pcap = pcap;
		this.startTime = new Date().getTime();
		
		this.setModal(true);
		this.setResizable(false);
		
		JPanel framePanel = new JPanel();
		framePanel.setLayout( new BoxLayout( framePanel, BoxLayout.Y_AXIS ) );
		framePanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );		
		this.getContentPane().add( framePanel, BorderLayout.CENTER );

		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		
		JButton stopButton = new JButton("Stop capture");
		stopButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				parent.stopCapture();
				TNVStopCaptureDialog.this.dispose();
				return;
			}
		} );

		timeLabel = new JLabel("Elapsed Time: 0 seconds");
		receivedLabel = new JLabel("Packets captured: 0");
		droppedLabel = new JLabel("Packets dropped: 0");
		
		framePanel.add(timeLabel);
		framePanel.add(receivedLabel);
		framePanel.add(droppedLabel);
		
		framePanel.add(progressBar);
		framePanel.add(Box.createVerticalStrut(10));
		framePanel.add(stopButton);
		
		// Thread for updating the packet count
		SwingWorker worker = new SwingWorker() {
			public Object construct() {
				return update();
			}
			public void finished() {
				done=true;
			}
		};
		worker.start();

		this.pack();
		this.getRootPane().setDefaultButton( stopButton );
		this.setLocationRelativeTo(this.parent);
		this.setVisible( true );

	}
	
	private Object update() {
		while (!done) {
			timeLabel.setText("Elapsed Time: " + ( ( new Date().getTime() - this.startTime ) / 1000 )
					+ " seconds");
			receivedLabel.setText("Packets captured: " + this.pcap.getStatistics().getReceivedCount());
			droppedLabel.setText("Packets dropped: " + this.pcap.getStatistics().getDroppedCount());
			try { Thread.sleep(1000); } catch (InterruptedException ex) { }
		}
		return "Completed";
    }
	
	/**
	 * Factory constructor
	 * @param p
	 * @return dialog
	 * @throws HeadlessException
	 */
	public static TNVStopCaptureDialog createTNVStopCaptureDialog(TNV p, PacketCapture pcap) throws HeadlessException {
		return new TNVStopCaptureDialog(p, pcap);
	}


}
