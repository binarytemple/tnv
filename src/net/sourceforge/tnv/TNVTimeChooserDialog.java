/**
 * Created on May 7, 2006
 * @author jgood
 *
 * Dialog window for choosing the time range to open
 */
package net.sourceforge.tnv;

import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * TNVPreferenceDialog
 */
public class TNVTimeChooserDialog extends JDialog {
	
	private static final int MIN_FOCUS_TIME_SLIDER = 0, MAX_FOCUS_TIME_SLIDER = 100;
	
	private static final String START_LABEL = "Start time to visualize:   ";
	private static final String END_LABEL = "End time to visualize:   ";
	private static final String PACKET_LABEL = "Number of packets:   ";
	
	private Timestamp dbStartTimestamp, dbEndTimestamp;
	private Timestamp startTimestamp, endTimestamp;
	
	private JLabel startTimeLabel, endTimeLabel, packetCountLabel;
	
	/**
	 * Constructor
	 * @throws HeadlessException
	 */
	private TNVTimeChooserDialog() throws HeadlessException {
		super( );

		dbStartTimestamp = TNVDbUtil.getInstance().getTotalMinTime();
		Timestamp last = TNVDbUtil.getInstance().getTotalMaxTime();
		dbEndTimestamp = new Timestamp(last.getTime() + 1000); // add one second to end timestamp
		
		// default to entire data set
		startTimestamp = dbStartTimestamp;
		endTimestamp = dbEndTimestamp;
		
		JPanel timePanel = new JPanel( );
		timePanel.setLayout( new BoxLayout( timePanel, BoxLayout.Y_AXIS) );
	
		// Create the label table for time sliders
		String startLabel = TNVUtil.NORMAL_FORMAT.format(dbStartTimestamp);
		String centerLabel = TNVUtil.NORMAL_FORMAT.format( new Date(
						dbStartTimestamp.getTime() + 
						((dbEndTimestamp.getTime() - dbStartTimestamp.getTime()) / 2) ) );
		String endLabel = TNVUtil.NORMAL_FORMAT.format(dbEndTimestamp);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put( new Integer( MIN_FOCUS_TIME_SLIDER ), new JLabel(startLabel) );
		labelTable.put( new Integer( (MAX_FOCUS_TIME_SLIDER + MIN_FOCUS_TIME_SLIDER) / 2 ), new JLabel(centerLabel) );
		labelTable.put( new Integer( MAX_FOCUS_TIME_SLIDER ), new JLabel(endLabel) );

		// Start slider
		JSlider startTimeSlider = new JSlider(SwingConstants.HORIZONTAL);
		startTimeSlider.setMinorTickSpacing( 1 );
		startTimeSlider.setMajorTickSpacing( 25 );
		startTimeSlider.setLabelTable( labelTable );
		startTimeSlider.setMinimum( MIN_FOCUS_TIME_SLIDER );
		startTimeSlider.setMaximum( MAX_FOCUS_TIME_SLIDER );
		startTimeSlider.setValue( MIN_FOCUS_TIME_SLIDER );
		startTimeSlider.setPaintLabels( true );
		startTimeSlider.setPaintTicks( true );
		startTimeSlider.setSnapToTicks( true );
		startTimeSlider.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				JSlider source = (JSlider) e.getSource();
				if ( ! source.getValueIsAdjusting() ) {
					// Convert integers of Slider to timestamps and reset focus time of parent panel
					int val = source.getValue();
					double minTime = dbStartTimestamp.getTime();
					double maxTime = dbEndTimestamp.getTime();
					double percentage = val / ((double) MAX_FOCUS_TIME_SLIDER - (double) MIN_FOCUS_TIME_SLIDER);
					long newTime = (long) (minTime + ( (maxTime - minTime) * percentage ));
					TNVTimeChooserDialog.this.startTimestamp = new Timestamp(newTime);
					TNVTimeChooserDialog.this.startTimeLabel.setText(START_LABEL + 
							TNVUtil.NORMAL_FORMAT.format(TNVTimeChooserDialog.this.startTimestamp));
					TNVTimeChooserDialog.this.packetCountLabel.setText(PACKET_LABEL + 
							TNVDbUtil.getInstance().getPacketCount
								(TNVTimeChooserDialog.this.startTimestamp, 
								TNVTimeChooserDialog.this.endTimestamp ));
				}
			}
		} );
		this.startTimeLabel = new JLabel(START_LABEL + TNVUtil.NORMAL_FORMAT.format(this.startTimestamp) );
		
		// End slider
		JSlider endTimeSlider = new JSlider(SwingConstants.HORIZONTAL);
		endTimeSlider.setMinorTickSpacing( 1 );
		endTimeSlider.setMajorTickSpacing( 25 );
		endTimeSlider.setLabelTable( labelTable );
		endTimeSlider.setMinimum( MIN_FOCUS_TIME_SLIDER );
		endTimeSlider.setMaximum( MAX_FOCUS_TIME_SLIDER );
		endTimeSlider.setValue( MAX_FOCUS_TIME_SLIDER );
		endTimeSlider.setPaintLabels( true );
		endTimeSlider.setPaintTicks( true );
		endTimeSlider.setSnapToTicks( true );
		endTimeSlider.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				JSlider source = (JSlider) e.getSource();
				if ( ! source.getValueIsAdjusting() ) {
					// Convert integers of Slider to timestamps and reset focus time of parent panel
					int val = source.getValue();
					double minTime = dbStartTimestamp.getTime();
					double maxTime = dbEndTimestamp.getTime();
					double percentage = val / ((double) MAX_FOCUS_TIME_SLIDER - (double) MIN_FOCUS_TIME_SLIDER);
					long newTime = (long) (minTime + ( (maxTime - minTime) * percentage ));
					TNVTimeChooserDialog.this.endTimestamp = new Timestamp(newTime);
					TNVTimeChooserDialog.this.endTimeLabel.setText(END_LABEL + 
							TNVUtil.NORMAL_FORMAT.format(TNVTimeChooserDialog.this.endTimestamp));
					TNVTimeChooserDialog.this.packetCountLabel.setText(PACKET_LABEL + 
							TNVDbUtil.getInstance().getPacketCount
								(TNVTimeChooserDialog.this.startTimestamp, 
								TNVTimeChooserDialog.this.endTimestamp ));
				}
			}
		} );
		this.endTimeLabel = new JLabel(END_LABEL + TNVUtil.NORMAL_FORMAT.format(this.endTimestamp) );
		
		this.packetCountLabel = new JLabel(PACKET_LABEL + 
				TNVDbUtil.getInstance().getPacketCount(this.startTimestamp, this.endTimestamp));
		
		timePanel.add(Box.createVerticalStrut(15));
		timePanel.add(this.startTimeLabel);
		timePanel.add(Box.createVerticalStrut(10));
		timePanel.add(startTimeSlider);
		timePanel.add(Box.createVerticalStrut(25));
		timePanel.add(this.endTimeLabel);
		timePanel.add(Box.createVerticalStrut(10));
		timePanel.add(endTimeSlider);
		timePanel.add(Box.createVerticalStrut(30));
		timePanel.add(this.packetCountLabel);
		timePanel.add(Box.createVerticalGlue());
		
		// Add settings panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout( FlowLayout.TRAILING ) );
		JButton saveButton = new JButton( "Go" );
		saveButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVDbUtil.getInstance().setMinTime(TNVTimeChooserDialog.this.startTimestamp);
				TNVDbUtil.getInstance().setMaxTime(TNVTimeChooserDialog.this.endTimestamp);
				TNVTimeChooserDialog.this.dispose();
			}
		} );
		buttonPanel.add( saveButton );
		this.getRootPane().setDefaultButton( saveButton );
		
		this.getContentPane().add( timePanel, java.awt.BorderLayout.CENTER );
		this.getContentPane().add( buttonPanel, java.awt.BorderLayout.SOUTH );
		
		this.pack();
		this.setTitle("Choose time frame to open");
		this.setModal(true);
		this.setResizable(false);

		this.setSize(500, 350);
		this.setLocationRelativeTo(this.getParent());
		this.setVisible( true );

	}
	
	/**
	 * Factory constructor
	 * @param dbStart
	 * @param dbEnd
	 * @return dialog
	 * @throws HeadlessException
	 */
	protected static TNVTimeChooserDialog createTNVTimeChooserDialog() throws HeadlessException {
		return new TNVTimeChooserDialog();
	}

}
