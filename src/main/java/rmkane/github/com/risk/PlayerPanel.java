package rmkane.github.com.risk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


public class PlayerPanel extends JPanel {
	private JPanel mainPanel, titlePanel, infoPanel;
	private Player player;
	private JLabel playerName;
	private Color bgColor;
	private JScrollPane scrollPane;
	private JList<String> territoriesList;
	private JLabel armySizeLabel;
	private JLabel draftedUnits;
	private final int CENTER = GridBagConstraints.CENTER;
	
	public PlayerPanel() {
		super();		
		init();
	}
	
	public void init() {
		bgColor = new Color(0x88aabb);
		playerName = new JLabel("Player X");
		armySizeLabel = new JLabel("Army Size: 0");
		draftedUnits = new JLabel("Drafted Units: 0");
		territoriesList = new JList<String>(new String[] {"Country 1", "Country 2", "Country 3"});
		playerName.setFont(new Font("Serif", Font.BOLD, 14));
		
		mainPanel = new JPanel();
		infoPanel = new JPanel();
		titlePanel = new JPanel();
		
		this.add(mainPanel);
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBackground(bgColor);
		mainPanel.add(titlePanel, BorderLayout.NORTH);
		mainPanel.add(infoPanel, BorderLayout.CENTER);
		
		titlePanel.setLayout(new GridLayout(0, 1));
		titlePanel.add(playerName).setForeground(new Color(0xFFFF00));
		titlePanel.add(armySizeLabel).setForeground(new Color(0xFFFF00));
		titlePanel.add(draftedUnits).setForeground(new Color(0xFFFF00));
				
		scrollPane = new JScrollPane(territoriesList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		infoPanel.setLayout(new BorderLayout());
		
		infoPanel.add(scrollPane);
		//addComponent(infoPanel, scrollPane, 0, 0, 1, 1, CENTER);
		
		
		//this.setPreferredSize(new Dimension(140, 200));
		mainPanel.setPreferredSize(new Dimension((int)(this.getPreferredSize().getSize().getWidth()*1.75), (int)(this.getPreferredSize().getSize().getHeight())));
		//titlePanel.setPreferredSize(new Dimension(120, 40));
	}
	
	public void addPlayer(Player player) {
		this.player = player;
	}
	
	public void updatePanel() {
		titlePanel.setBackground(player.getColor());
		playerName.setText("Player: " + player.getName());
		playerName.setForeground(new Color(0xFFFFFF));
		armySizeLabel.setText("Army Size: " + player.getArmySize());
		draftedUnits.setText("Drafted Units: " + player.getDraftedArmies());
		territoriesList.ensureIndexIsVisible(territoriesList.getSelectedIndex());
		territoriesList.setListData(player.getTerritoriesByName());
		territoriesList.revalidate();
		territoriesList.repaint();
	}
	
	/** Adds a component to a panel using the GridBagConstraints layout manager. */
	private void addComponent(JPanel panel, JComponent component, int xPos,
			int yPos, int width, int height, int align) {
		GridBagConstraints grid = new GridBagConstraints();
		grid.gridx = xPos; // Column.
		grid.gridy = yPos; // Row.
		grid.gridwidth = width; // Width.
		grid.gridheight = height; // Height.
		grid.anchor = align; // Position
		grid.insets = new Insets(3, 3, 3, 3); // Add space between components.
		grid.fill = GridBagConstraints.NONE; // Resize component.
		panel.add(component, grid); // Adds component to the grid to the panel.
	} // End addComponent.
}
