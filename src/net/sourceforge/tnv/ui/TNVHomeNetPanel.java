/**
 * Created on Apr 29, 2006
 * @author jgood
 *
 * Panel for choosing the home network
 */
package net.sourceforge.tnv.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.StringTokenizer;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * TNVHomeNetPanel
 */
public class TNVHomeNetPanel extends JPanel {

	private String localNet = "";
	private JComboBox netmaskBox;
	private JTextField netFieldA, netFieldB, netFieldC, netFieldD;

	/**
	 * Constructor
	 */
	public TNVHomeNetPanel() {
		super();
		initGUI();
	}
	
	/**
	 * Constructor
	 * @param n
	 */
	public TNVHomeNetPanel(String n) {
		super();
		this.localNet = n;
		initGUI();
	}

	/**
	 * returns string of the network address from the text fields
	 * @return network string
	 */
	public String getNetworkAddr( ) {
		String a = this.netFieldA.getText();
		String b = this.netFieldB.getText();
		String c = this.netFieldC.getText();
		String d = this.netFieldD.getText();
		String addr = a + "." + b + "." + c + "." + d; // default address, get
		// all fields
		switch ( this.netmaskBox.getSelectedIndex() ) {
		case 0:
			addr = a + ".0.0.0";
			break;
		case 1:
			addr = a + "." + b + ".0.0";
			break;
		case 2:
			addr = a + "." + b + "." + c + ".0";
			break;
		}
		return addr;
	}
	

	private void initGUI() {
		if ( ! this.localNet.equals("") ) {
			String localDomain = "";
			try {
				localDomain = InetAddress.getByName( this.localNet ).getCanonicalHostName();
			}
			catch ( Exception e ) {
				localDomain = this.localNet;
			}
		}

		String[] parsedNet = new String[4];
		StringTokenizer st = new StringTokenizer( this.localNet, "." );
		int i = 0;
		int nm = 2; // determines the netmask from loaded network address (based on non-0 quad)
		boolean found = false;
		while ( st.hasMoreTokens() ) {
			String s = st.nextToken();
			if ( s.equals( "0" ) & found == false ) {
				found = true;
				nm = i - 1;
			}
			parsedNet[i] = s;
			i++;
		}

		this.netFieldA = new JTextField( parsedNet[0], 2 );
		this.netFieldB = new JTextField( parsedNet[1], 2 );
		this.netFieldC = new JTextField( parsedNet[2], 2 );
		this.netFieldD = new JTextField( parsedNet[3], 2 );
		setFieldsEditable( nm );

		JPanel netmaskBoxPanel = new JPanel( new FlowLayout(FlowLayout.LEADING) );
		String[] netSizes = { "/8  (255.0.0.0)", "/16 (255.255.0.0)", "/24 (255.255.255.0)", "/32 (255.255.255.255)" };
		this.netmaskBox = new JComboBox( netSizes );
		this.netmaskBox.setToolTipText("Use to adjust the netmask for home (local) network; adjust before setting fields");
		this.netmaskBox.setSelectedIndex( nm );
		this.netmaskBox.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				int index = ( (JComboBox) e.getSource() ).getSelectedIndex();
				setFieldsEditable( index );
			}
		} );
		netmaskBoxPanel.add(netmaskBox);

		this.setLayout( new BorderLayout() );

		
		JPanel netFieldPanel = new JPanel( new FlowLayout(FlowLayout.LEADING) );
		netFieldPanel.add( this.netFieldA );
		netFieldPanel.add( this.netFieldB );
		netFieldPanel.add( this.netFieldC );
		netFieldPanel.add( this.netFieldD );
		this.add(netFieldPanel, BorderLayout.NORTH);
		
		this.add(netmaskBoxPanel, BorderLayout.CENTER);
	}
	
	
	/**
	 * Set which network address octet fields can be edited based on netmask
	 * @param index
	 */
	private void setFieldsEditable( int index ) {
		if ( index == 0 ) {
			this.netFieldA.setEnabled( true );
			this.netFieldB.setEnabled( false );
			this.netFieldC.setEnabled( false );
			this.netFieldD.setEnabled( false );
		}
		else if ( index == 1 ) {
			this.netFieldA.setEnabled( true );
			this.netFieldB.setEnabled( true );
			this.netFieldC.setEnabled( false );
			this.netFieldD.setEnabled( false );
		}
		else if ( index == 2 ) {
			this.netFieldA.setEnabled( true );
			this.netFieldB.setEnabled( true );
			this.netFieldC.setEnabled( true );
			this.netFieldD.setEnabled( false );
		}
		else {
			this.netFieldA.setEnabled( true );
			this.netFieldB.setEnabled( true );
			this.netFieldC.setEnabled( true );
			this.netFieldD.setEnabled( true );
		}
	}

}
