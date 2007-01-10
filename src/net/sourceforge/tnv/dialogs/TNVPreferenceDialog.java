/**
 * Created on Mar 6, 2005
 * @author jgood
 *
 * Dialog window for changing preferences 
 */
package net.sourceforge.tnv.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.SortedMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.tnv.ui.TNVPreferenceData;
import net.sourceforge.tnv.util.TNVUtil;

/**
 * TNVPreferenceDialog
 */
public class TNVPreferenceDialog extends JFrame {

	private Color tcpCol, udpCol, icmpCol, synCol, ackCol, finCol, pshCol, urgCol, rstCol;

	private JPanel linkColorPanel, hostColorPanel;

	private ColorChooserPanel colChooser;
	private String[] colorList = { 
			"Blue (1-500 packets)", "Yellow to Blue (500)", "Green (500)", "Yellow to Green (500)",
			"Purple (500)", "Brown (500)", "Gray  (500)", 
			"Blue (1-100 packets)", "Yellow to Blue (100)", "Green (100)", "Yellow to Green (100)",
			"Purple (100)", "Brown (100)", "Gray  (100)" 
			};
	private JComboBox colorMapBox;
	private ColorPanel colorMapPanel;
	

	/**
	 * Constructor
	 * @throws HeadlessException
	 */
	private TNVPreferenceDialog() throws HeadlessException {
		super( "Color Preferences" );
		loadPrefs();

		JTabbedPane tabPane = new JTabbedPane();

		this.linkColorPanel = new JPanel();
		setupLinkColorPanel();

		this.hostColorPanel = new JPanel();
		setupHostColorPanel();

		tabPane.addTab( "Protocol Color", null, this.linkColorPanel, "Link and packet protocol color Preferences" );
		tabPane.addTab( "Host Color", null, this.hostColorPanel, "Host color Preferences" );

		// Add settings panel
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout( new FlowLayout( FlowLayout.TRAILING ) );
		JButton cancelButton = new JButton( "Cancel" );
		cancelButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVPreferenceDialog.this.dispose();
			}
		} );
		JButton saveButton = new JButton( "Save" );
		saveButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				savePrefs();
				TNVPreferenceDialog.this.dispose();
			}
		} );
		settingsPanel.add( cancelButton );
		settingsPanel.add( saveButton );

		this.getRootPane().setDefaultButton( saveButton );
		this.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		this.getContentPane().add( tabPane, java.awt.BorderLayout.CENTER );
		this.getContentPane().add( settingsPanel, java.awt.BorderLayout.SOUTH );
		this.pack();
		this.setSize( new Dimension( 480, 520 ) );
		this.setLocationRelativeTo( this.getParent() );
		this.setVisible( true );

	}


	/**
	 * Load preference data
	 */
	private void loadPrefs( ) {
		this.tcpCol = TNVPreferenceData.getInstance().getTcpColor();
		this.udpCol = TNVPreferenceData.getInstance().getUdpColor();
		this.icmpCol = TNVPreferenceData.getInstance().getIcmpColor();
		this.synCol = TNVPreferenceData.getInstance().getSynColor();
		this.ackCol = TNVPreferenceData.getInstance().getAckColor();
		this.finCol = TNVPreferenceData.getInstance().getFinColor();
		this.urgCol = TNVPreferenceData.getInstance().getUrgColor();
		this.pshCol = TNVPreferenceData.getInstance().getPshColor();
		this.rstCol = TNVPreferenceData.getInstance().getRstColor();
	}


	/**
	 * Save preference data 
	 */
	private void savePrefs( ) {
		TNVPreferenceData.getInstance().setTcpColor( this.tcpCol );
		TNVPreferenceData.getInstance().setUdpColor( this.udpCol );
		TNVPreferenceData.getInstance().setIcmpColor( this.icmpCol );
		TNVPreferenceData.getInstance().setSynColor( this.synCol );
		TNVPreferenceData.getInstance().setAckColor( this.ackCol );
		TNVPreferenceData.getInstance().setFinColor( this.finCol );
		TNVPreferenceData.getInstance().setUrgColor( this.urgCol );
		TNVPreferenceData.getInstance().setPshColor( this.pshCol );
		TNVPreferenceData.getInstance().setRstColor( this.rstCol );
		TNVPreferenceData.getInstance().saveProperties();
	}


	/**
	 * Setup the link color chooser panel 
	 */
	private void setupLinkColorPanel( ) {
		String[] protocols = { "TCP", "UDP", "ICMP/Other", 
				"TCP SYN", "TCP ACK", "TCP FIN", "TCP URG", "TCP PSH", "TCP RST" };
		this.colChooser = new ColorChooserPanel( "TCP", this.tcpCol );
		JPanel typePanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		JLabel typeLabel = new JLabel( "Set color for " );
		JComboBox typeCombo = new JComboBox( protocols );
		typeCombo.setSelectedIndex( 0 );
		typeCombo.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String s = (String) ( (JComboBox) e.getSource() ).getSelectedItem();
				Color c;
				if ( s.equals( "TCP" ) )
					c = TNVPreferenceDialog.this.tcpCol;
				else if ( s.equals( "UDP" ) )
					c = TNVPreferenceDialog.this.udpCol;
				else if ( s.equals( "TCP SYN" ) )
					c = TNVPreferenceDialog.this.synCol;
				else if ( s.equals( "TCP ACK" ) )
					c = TNVPreferenceDialog.this.ackCol;
				else if ( s.equals( "TCP FIN" ) )
					c = TNVPreferenceDialog.this.finCol;
				else if ( s.equals( "TCP URG" ) )
					c = TNVPreferenceDialog.this.urgCol;
				else if ( s.equals( "TCP PSH" ) )
					c = TNVPreferenceDialog.this.pshCol;
				else if ( s.equals( "TCP RST" ) )
					c = TNVPreferenceDialog.this.rstCol;
				else
					c = TNVPreferenceDialog.this.icmpCol;
				TNVPreferenceDialog.this.colChooser.changeType( s, c );
			}
		} );
		typePanel.add( typeLabel );
		typePanel.add( typeCombo );

		this.linkColorPanel.setLayout( new BoxLayout( this.linkColorPanel, BoxLayout.Y_AXIS ) );
		this.linkColorPanel.add( Box.createVerticalGlue() );
		this.linkColorPanel.add( typePanel );
		this.linkColorPanel.add( this.colChooser );
		this.linkColorPanel.add( Box.createVerticalGlue() );
	}


	/**
	 * Setup the host color chooser panel 
	 */
	private void setupHostColorPanel( ) {

		this.hostColorPanel.setLayout( new BoxLayout( this.hostColorPanel, BoxLayout.Y_AXIS ) );

		this.colorMapBox = new JComboBox( this.colorList );
		this.colorMapBox.setToolTipText( "Change the machine color scheme" );
		this.colorMapBox.setSelectedIndex( TNVPreferenceData.getInstance().getColorMapIndex() );
		this.colorMapBox.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent e ) {
				int index = ( (JComboBox) e.getSource() ).getSelectedIndex();
				TNVPreferenceData.getInstance().setColorMapIndex( index );
				TNVPreferenceDialog.this.colorMapPanel.changeColorMap();
			}
		} );

		this.colorMapPanel = new ColorPanel();
		this.colorMapPanel.setToolTipText( "Shows the number of packets for selected shades of color" );

		JLabel colorLabel = new JLabel( "Packet number for host / time" );

		this.hostColorPanel.add( Box.createVerticalGlue() );
		JPanel colorMapBoxPanel = new JPanel();
		colorMapBoxPanel.add( this.colorMapBox );
		this.hostColorPanel.add( colorMapBoxPanel );
		this.hostColorPanel.add( this.colorMapPanel );
		JPanel colorLabelPanel = new JPanel();
		colorLabelPanel.add( colorLabel );
		this.hostColorPanel.add( colorLabelPanel );
		this.hostColorPanel.add( Box.createVerticalGlue() );

		// TODO: user defined color choices
		/*
		 * JLabel title = new JLabel("Configure the range of packets for each
		 * color");
		 * 
		 * JPanel p1 = new JPanel(); HostColorBox b1 = new
		 * HostColorBox(Color.RED); JTextField start1 = new JTextField("" + 1,
		 * 4); start1.setEditable(false); JTextField end1 = new JTextField("" +
		 * 10, 4); p1.add(b1); p1.add(new JLabel("Start")); p1.add(start1);
		 * p1.add(new JLabel("End")); p1.add(end1);
		 * 
		 * JPanel p2 = new JPanel(); HostColorBox b2 = new
		 * HostColorBox(Color.RED); JTextField start2 = new
		 * JTextField(Integer.parseInt( start1.getText()) + 1 + "", 4);
		 * start1.setEditable(false); JTextField end2 = new JTextField("" + 40,
		 * 4); p2.add(b2); p2.add(new JLabel("Start")); p2.add(start2);
		 * p2.add(new JLabel("End")); p2.add(end2);
		 * 
		 * HostColorBox b3 = new HostColorBox(Color.RED); HostColorBox b4 = new
		 * HostColorBox(Color.RED); HostColorBox b5 = new
		 * HostColorBox(Color.RED); HostColorBox b6 = new
		 * HostColorBox(Color.RED); HostColorBox b7 = new
		 * HostColorBox(Color.RED); HostColorBox b8 = new
		 * HostColorBox(Color.RED);
		 * 
		 * hostColorPanel.setLayout(new
		 * BoxLayout(hostColorPanel,BoxLayout.Y_AXIS));
		 * hostColorPanel.add(title); hostColorPanel.add(p1);
		 * hostColorPanel.add(p2);
		 */
	}

	
	
	/********************************************************************************
	 * 								INNER CLASSES
	 *******************************************************************************/

	/**
	 * ColorChooserPanel inner class
	 */
	private class ColorChooserPanel extends JPanel implements ChangeListener {

		private String type;
		private Color col;
		private JColorChooser cc;
		private JLabel prevLabel;

		/**
		 * Constructor
		 * @param t
		 * @param c
		 */
		public ColorChooserPanel(String t, Color c) {
			this.type = t;
			this.col = c;
			this.cc = new JColorChooser( this.col );
			this.cc.getSelectionModel().addChangeListener( this );
			this.add( this.cc );
		}

		/**
		 * @param t
		 * @param c
		 */
		public void changeType( String t, Color c ) {
			this.type = t;
			this.col = c;
			this.cc.setColor( this.col );
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged( ChangeEvent e ) {
			this.col = this.cc.getColor();
			if ( this.type.equals( "TCP" ) )
				TNVPreferenceDialog.this.tcpCol = this.col;
			else if ( this.type.equals( "UDP" ) )
				TNVPreferenceDialog.this.udpCol = this.col;
			else if ( this.type.equals( "TCP SYN" ) )
				TNVPreferenceDialog.this.synCol = this.col;
			else if ( this.type.equals( "TCP ACK" ) )
				TNVPreferenceDialog.this.ackCol = this.col;
			else if ( this.type.equals( "TCP FIN" ) )
				TNVPreferenceDialog.this.finCol = this.col;
			else if ( this.type.equals( "TCP URG" ) )
				TNVPreferenceDialog.this.urgCol = this.col;
			else if ( this.type.equals( "TCP PSH" ) )
				TNVPreferenceDialog.this.pshCol = this.col;
			else if ( this.type.equals( "TCP RST" ) )
				TNVPreferenceDialog.this.rstCol = this.col;
			else
				TNVPreferenceDialog.this.icmpCol = this.col;
		}
	}


	/**
	 * HostColorBox inner class to show color for a range of numbers
	 */
	private class HostColorBox extends JPanel implements ChangeListener {

		private Color col;

		/**
		 * Constructor
		 * @param c
		 */
		public HostColorBox(Color c) {
			this.col = c;
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paint(java.awt.Graphics)
		 */
		@Override
		public void paint( Graphics g ) {
			g.setColor( this.col );
			g.fillRect( 0, 0, 30, 30 );
		}

		/**
		 * @param c
		 */
		public void changeColor( Color c ) {
			this.col = c;
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged( ChangeEvent e ) {

		}
	}


	
	/**
	 * ColorPanel inner class to draw the color map for hosts packet frequency
	 */
	private class ColorPanel extends JPanel {

		private SortedMap colorMap;

		/**
		 * Constructor
		 */
		public ColorPanel() {
			super();
			this.colorMap = TNVUtil.getColorMap( TNVPreferenceData.getInstance().getColorMapIndex() );
		}

		/**
		 * Change the color map
		 */
		void changeColorMap( ) {
			this.colorMap = TNVUtil.getColorMap( TNVPreferenceData.getInstance().getColorMapIndex() );
			this.repaint();
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		@Override
		public void paintComponent( Graphics g ) {
			Graphics2D g2 = (Graphics2D) g;
			int panelWidth = this.getParent().getWidth();
			int boxSize = 20;
			int boxNum = ( panelWidth - 10 ) / boxSize;
			int c = 1;
			int colorInterval = ( this.colorMap.size() / boxNum );
			for ( int i = 5; i <= ( boxSize * boxNum ); i += boxSize ) {
				// Set color and fill in colored rectangle
				g2.setColor( (Color) this.colorMap.get( new Integer( c ) ) );
				Rectangle2D r = new Rectangle2D.Float( i, 0, boxSize, boxSize );
				g2.fill( r );
				g2.setColor( Color.darkGray );
				g2.draw( r );
				// Reset color and draw size
				if ( c < ( this.colorMap.size() / 2 ) )
					g2.setColor( Color.darkGray );
				else
					g2.setColor( Color.lightGray );
				g2.setFont( TNVUtil.LABEL_FONT );
				g2.drawString( "" + c, ( i + ( boxSize / 4 ) ), ( boxSize - ( boxSize / 4 ) ) );
				c += colorInterval;
			}
		}
	}
	
	/**
	 * Factory constructor
	 * @return dialog
	 * @throws HeadlessException
	 */
	public static TNVPreferenceDialog createTNVPreferenceDialog() throws HeadlessException {
		return new TNVPreferenceDialog();
	}

}
