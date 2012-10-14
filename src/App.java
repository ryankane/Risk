import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

public class App extends JFrame implements MouseListener, ActionListener {

	private static final long serialVersionUID = 1L;

	// GUI Components
	private final String TITLE = "RISK - Ryan Is So Kewl";
	private final int CENTER = GridBagConstraints.CENTER;
	private static int WIDTH = 1024, HEIGHT = 728;
	private final Dimension SIZE = new Dimension(WIDTH, HEIGHT);

	private final String END_DRAFT = "End Draft Phase";
	private final String END_ATTACK = "End Attack Phase";
	private final String END_TURN = "End Turn";

	// Keep Track of Cash In values
	private int cashInIndex;
	private final int[] cashInValues = { 4, 6, 8, 10, 12, 15 };
	private int cashInVal;

	// Menu
	private JMenuBar menubar = new JMenuBar();
	private JMenu fileMenu = new JMenu("File");
	private JMenuItem newGame = new JMenuItem("New Game");
	private JMenuItem quit = new JMenuItem("Quit");
	private JMenu editMenu = new JMenu("Edit");
	private JMenu helpMenu = new JMenu("Help");
	private JMenuItem about = new JMenuItem("About");

	private JPanel mainPanel;
	private JPanel playerInfoPanel;
	private PlayerPanel[] playerPanel;
	private JPanel mapWrapper;
	private MapPanel mapPanel;
	private JPanel viewPanel;
	private JPanel actionPanel;
	private JPanel selectPanel;
	private JPanel commandPanel;
	private JLabel selectPanelTitle;

	private JLabel sourceCountryTitleLabel;
	private JLabel sourceCountryImageLabel;
	private JLabel sourceCountryNameLabel;
	private JLabel destCountryTitleLabel;
	private JLabel destCountryImageLabel;
	private JLabel destCountryNameLabel;
	private JLabel commandPanelTitle;
	private JButton chooseSourceButton;
	private JButton chooseDestButton;
	private JButton actionButton;
	private JButton endPhaseButton;
	private JPanel cardListPanel;
	private JLabel cardPanelLabel;
	private JList<String> cardList;
	private JScrollPane cardListScroll;
	private final ImageIcon DEFAULT_ICON = new ImageIcon(readImage("resources/default.png"));

	private Country sourceCountry;
	private Country destCountry;

	private Object lastButton;

	private ArrayList<Player> players;
	private Board gameboard;
	private Deck deck;
	private int turn;
	private boolean capturedTerritory = false;

	private Player currPlayer;

	private enum State {
		DRAFT, ATTACK, FORTIFY, END
	};

	private State gameState;

	// private Deck territories;

	public App() {
		cashInIndex = 0;
		cashInVal = cashInValues[cashInIndex];
		gameState = State.DRAFT;
		lastButton = null;
		// Create Board
		this.gameboard = new Board();
		createPlayers();
		this.turn = 0;
		dealCards();
		assignTerritories();
		initGUI();
		initialArmyPlacement();
		takeTurn();
		System.out.println("SIZE: " + gameboard.getWorld().size());
	}

	private int cashIn(int index) {
		int matchBonus = 0;
		int initHandSize = currPlayer.getHand().size() - 1;
		for (int j = initHandSize; j >= 0; j--) {
			Card card = currPlayer.getHand().get(j);
			if (card.getUnitType().id() == index) {
				for (Country t : currPlayer.getTerritories()) {
					if (t.getName().equalsIgnoreCase(card.getCountryName())) {
						if (matchBonus == 0) {
							matchBonus = 2;
							System.out.println("Matched");
						}
					}
				}
				deck.addToDiscard(card);
				currPlayer.getHand().remove(j);
				System.out.println("[Same] Removed Card: " + card.getCountryName());
			}
		}
		return matchBonus;
	}

	private void draft() {
		if (gameState == State.DRAFT) {
			/** Territory Bonus */
			int territoryBonus = (int) Math
					.floor(currPlayer.getTerritories().size() / 3);
			/** Continent Bonus */
			int continentBonus = 0;
			Iterator<Continent> ic = gameboard.getWorld().iterator();
			while (ic.hasNext()) {
				boolean contains = true;
				Continent continent = (Continent) ic.next();
				Iterator<Country> it = continent.getCountries().iterator();
				while (it.hasNext() && contains) {
					Country country = it.next();
					if (!(currPlayer.getTerritories().contains(country)))
						contains = false;
					else
						contains = true;
				}
				if (contains)
					continentBonus += continent.getBonusArmies();
			}
			/** Cash-in Bonus */
			int cash_inBonus = 0;
			int match_bonus = 0;
			boolean cashed = false;
			ArrayList<Card> cards = currPlayer.getHand();
			if (cards.size() > 2) {
				int[] check = new int[4];
				for (Card c : cards)
					check[c.getUnitType().id()]++;
				int count = 0;
				for (int i = 0; i < check.length - 1; i++) {
					// If you have 3 of the same type of card
					if (check[i] > 2) {
						cashed = true;
						match_bonus = cashIn(i);
						break;
					} else if (check[i] > 0)
						count++;
				}
				// If you have 3 different type cards
				if (count > 2) {
					cashed = true;
					for (int k = 0; k < check.length - 1; k++) {
						if (check[k] > 0)
							match_bonus = cashIn(k);
					}
				}
				if (cashed) {
					cash_inBonus = cashInVal;
					// Increment cash in value
					cashInIndex++;
					cashInVal = (cashInIndex > cashInValues.length - 1) ? cashInVal + 5
							: cashInValues[cashInIndex];
				}
			}

			popup(String.format("T: %d, C: %d, $: %d, M: %d", territoryBonus,
					continentBonus, cash_inBonus, match_bonus), 1);
			currPlayer.setDraftedArmies(currPlayer.getDraftedArmies()
					+ territoryBonus + continentBonus + cash_inBonus + match_bonus);
			updateGUI();
		}
	}

	public void takeTurn() {
		adjust();
		currPlayer = players.get(turn % 2);
		JOptionPane.showMessageDialog(null, "Get Ready " + currPlayer.getName(),
				"RISK", JOptionPane.PLAIN_MESSAGE);
		sourceCountryImageLabel.setIcon(DEFAULT_ICON);
		destCountryImageLabel.setIcon(DEFAULT_ICON);
		capturedTerritory = false;
		gameState = State.DRAFT;
		clearSelection();
		actionButton.setText("<Action>");
		chooseSourceButton.setEnabled(false);
		chooseDestButton.setEnabled(false);
		actionButton.setEnabled(false);
		endPhaseButton.setEnabled(true);
		endPhaseButton.setText(END_DRAFT);

		draft();

		// Handle AI, Turn
		if (currPlayer != players.get(0) && currPlayer == players.get(1)) {
			// Draft
			Country risky = null;
			String s = "";
			for (Country country : currPlayer.getTerritories()) {
				if (risky == null) {
					risky = country;
					s += country.getName() + "\n";
				}
				if (country.getWeight() > risky.getWeight()) {
					risky = country;
					s += country.getName() + "\n";
				}
			}
			popup(s, 1);
			currPlayer.draftUnits(currPlayer.getDraftedArmies(), risky);
			updateGUI();
			// Attack

			// Fortify
		}
	}

	/** Create players */
	private void createPlayers() {
		// Create Players
		this.players = new ArrayList<Player>();
		players.add(new Player("Human", PlayerColors.BLUE, 30)); // Change back to
																															// 80
		players.add(new PlayerAI("Computer", PlayerColors.RED, 80));
		players.add(new Player("Neutral", PlayerColors.GRAY, 40));
	}

	/** Create a new deck and deal cards out to players */
	private void dealCards() {
		this.deck = new Deck();
		for (int i = 0; deck.deckSize() > 0; i++) {
			players.get(i % players.size()).getHand().add(deck.draw());
		}
	}

	private void adjust() {
		gameboard.setWeightsHard(players.get(1), players.get(0), players.get(2));
		gameboard.printWeights();
	}

	/** Based on the cards that players have, assign them territories */
	private void assignTerritories() {
		for (Player player : players) {
			for (Continent continent : gameboard.getWorld()) {
				for (Country country : continent.getCountries()) {
					for (Card card : player.getHand()) {
						if (country.getName().equalsIgnoreCase(card.getCountryName())) {
							// Add a reference to player territories
							player.getTerritories().add(country);
							// Assign territory traits
							country.setPlayer(player);
							player.draftUnits(1, country);
						}
					}
				}
			}
		}

		for (Player p : players) {
			while (p.getHand().size() > 0) {
				deck.addToDiscard(p.getHand().get(0));
				p.getHand().remove(p.getHand().get(0));
			}
		}
		System.out.println("Done");
		deck.emptyDeck();
	}

	private void initialArmyPlacement() {
		if (gameState == State.DRAFT) {
			for (int i = 1; i < players.size(); i++) {
				adjust(); // Make sure the Neutral player knows where to place units in
									// relation to AI
				Player p = players.get(i);
				int ida = p.getDraftedArmies();
				ArrayList<Country> l = p.getTerritories();
				float tw = 0;
				for (Country c : l) {
					tw += c.getWeight();
				}
				while (p.getDraftedArmies() > 0) {
					for (Country c : l) {
						int draftable = (int) ((c.getWeight() / tw) * ida);
						draftable = (draftable > p.getDraftedArmies()) ? p
								.getDraftedArmies() : draftable;
						p.draftUnits(draftable, c);
					}
				}
			}
			chooseSourceButton.setEnabled(false);
			chooseDestButton.setEnabled(false);
			actionButton.setEnabled(false);
			endPhaseButton.setEnabled(false);
			endPhaseButton.setText(END_DRAFT);
			currPlayer = players.get(turn % 2);
			updateGUI();
			while (players.get(0).getDraftedArmies() > 0)
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	private void initGUI() {
		initMenu();
		initComponents();

		this.setTitle(TITLE);
		this.setPreferredSize(SIZE);
		this.setMinimumSize(SIZE);
		this.setMaximumSize(SIZE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private void initComponents() {
		// Initialize components
		mainPanel = new JPanel(new BorderLayout());
		playerInfoPanel = new JPanel(new GridLayout(3, 1));
		playerPanel = new PlayerPanel[3];
		playerPanel[0] = new PlayerPanel();
		playerPanel[1] = new PlayerPanel();
		playerPanel[2] = new PlayerPanel();
		mapWrapper = new JPanel(new GridBagLayout());
		mapPanel = new MapPanel(gameboard);
		System.out.println("SIZE: " + gameboard.getWorld().size());
		viewPanel = new JPanel(new BorderLayout());
		actionPanel = new JPanel(new GridBagLayout());
		selectPanel = new JPanel(new GridBagLayout());
		commandPanel = new JPanel(new BorderLayout());
		selectPanelTitle = new JLabel("Selected Territories");
		sourceCountryTitleLabel = new JLabel("Source");
		sourceCountryImageLabel = new JLabel(DEFAULT_ICON);
		sourceCountryNameLabel = new JLabel("<SOURCE>");
		destCountryTitleLabel = new JLabel("Desination");
		destCountryImageLabel = new JLabel(DEFAULT_ICON);
		destCountryNameLabel = new JLabel("<DESTINATION>");
		commandPanelTitle = new JLabel("Commands");
		chooseSourceButton = new JButton("Choose Source");
		chooseDestButton = new JButton("Choose Destination");
		actionButton = new JButton("<Action>");
		endPhaseButton = new JButton(END_DRAFT);
		cardListPanel = new JPanel(new GridBagLayout());
		cardPanelLabel = new JLabel("Hand");
		cardList = new JList<String>(new String[] { "<Empty>" });
		cardListScroll = new JScrollPane(cardList);

		// Add Listeners
		mainPanel.addMouseListener(this);
		chooseSourceButton.addActionListener(this);
		chooseDestButton.addActionListener(this);
		actionButton.addActionListener(this);
		endPhaseButton.addActionListener(this);

		Font titleFont = new Font("Arial", Font.BOLD, 14);
		selectPanelTitle.setFont(titleFont);
		commandPanelTitle.setFont(titleFont);
		cardPanelLabel.setFont(titleFont);

		// Player Info panel
		// playerInfoPanel.setPreferredSize(new Dimension(150, 0));
		// rightToolbar.setLayout(new GridLayout(4,1,0,15));
		playerInfoPanel.setBorder(LineBorder.createGrayLineBorder());
		playerInfoPanel.setBackground(new Color(0x888888));
		mainPanel.setBackground(new Color(0x0));

		sourceCountryImageLabel.setBorder(LineBorder.createGrayLineBorder());
		destCountryImageLabel.setBorder(LineBorder.createGrayLineBorder());

		sourceCountryImageLabel.addMouseListener(this);

		// Add components
		this.add(mainPanel);
		mainPanel.add(playerInfoPanel, BorderLayout.WEST);
		playerInfoPanel.add(playerPanel[0]);
		playerInfoPanel.add(playerPanel[1]);
		playerInfoPanel.add(playerPanel[2]);
		mainPanel.add(viewPanel, BorderLayout.CENTER);
		viewPanel.add(mapWrapper, BorderLayout.CENTER);
		addComponent(mapWrapper, mapPanel, 0, 0, 1, 1, CENTER);
		viewPanel.add(commandPanel, BorderLayout.SOUTH);
		commandPanel.add(selectPanel, BorderLayout.WEST);
		selectPanel.setBorder(LineBorder.createBlackLineBorder());
		addComponent(selectPanel, selectPanelTitle, 0, 0, 2, 1,
				GridBagConstraints.NORTH);
		addComponent(selectPanel, sourceCountryTitleLabel, 0, 1, 1, 1, CENTER);
		addComponent(selectPanel, sourceCountryImageLabel, 0, 2, 1, 1, CENTER);
		addComponent(selectPanel, sourceCountryNameLabel, 0, 3, 1, 1, CENTER);
		addComponent(selectPanel, destCountryTitleLabel, 1, 1, 1, 1, CENTER);
		addComponent(selectPanel, destCountryImageLabel, 1, 2, 1, 1, CENTER);
		addComponent(selectPanel, destCountryNameLabel, 1, 3, 1, 1, CENTER);
		commandPanel.add(actionPanel, BorderLayout.CENTER);
		addComponent(actionPanel, commandPanelTitle, 0, 0, 1, 1, CENTER);
		addComponent(actionPanel, chooseSourceButton, 0, 1, 1, 1, CENTER);
		addComponent(actionPanel, chooseDestButton, 0, 2, 1, 1, CENTER);
		addComponent(actionPanel, actionButton, 0, 3, 1, 1, CENTER);
		addComponent(actionPanel, endPhaseButton, 0, 4, 1, 1, CENTER);
		commandPanel.add(cardListPanel, BorderLayout.EAST);
		addComponent(cardListPanel, cardPanelLabel, 0, 0, 1, 1,
				GridBagConstraints.NORTH);
		addComponent(cardListPanel, cardListScroll, 0, 1, 1, 1, CENTER);

		mapPanel.addMouseListener(this);

		mapWrapper.setBackground(new Color(0xbbccff));
		// sourceCountryImageLabel.setPreferredSize(new Dimension(20, 20));
		// destCountryImageLabel.setPreferredSize(new Dimension(20, 20));

		// Add players to the playerPanel and update
		for (int i = 0; i < players.size(); i++) {
			playerPanel[i].addPlayer(players.get(i));
			playerPanel[i].updatePanel();
		}
	}

	private void initMenu() {
		menubar = new JMenuBar();
		menubar.add(fileMenu);
		menubar.add(editMenu);
		menubar.add(helpMenu);
		fileMenu.add(newGame);
		fileMenu.add(quit);
		helpMenu.add(about);
		fileMenu.setMnemonic(KeyEvent.VK_F);
		editMenu.setMnemonic(KeyEvent.VK_E);
		helpMenu.setMnemonic(KeyEvent.VK_H);
		newGame.setMnemonic(KeyEvent.VK_N);
		quit.setMnemonic(KeyEvent.VK_Q);
		about.setMnemonic(KeyEvent.VK_A);
		newGame.addActionListener(new NewGameMenuListener());
		quit.addActionListener(new QuitMenuListener());
		about.addActionListener(new AboutMenuListener());
		this.setJMenuBar(menubar);
	}

	private void updateGUI() {
		for (PlayerPanel p : playerPanel)
			p.updatePanel();
		mapPanel.update();
		this.repaint();

		if (!(currPlayer.getHand().isEmpty())) {
			cardList.ensureIndexIsVisible(cardList.getSelectedIndex());
			String[] cardsByName = new String[currPlayer.getHand().size()];
			int i = 0;
			for (Card c : currPlayer.getHand())
				cardsByName[i++] = (String.format("[%s] (%s)", c.getCountryName(), c
						.getUnitType().getName()));
			cardList.setListData(cardsByName);
		}
	}

	private void clearSelection() {
		sourceCountry = null;
		destCountry = null;
		sourceCountryNameLabel.setText("<SOURCE>");
		destCountryNameLabel.setText("<DESTINATION>");
		sourceCountryImageLabel.setIcon(DEFAULT_ICON);
		destCountryImageLabel.setIcon(DEFAULT_ICON);
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
	}

	public BufferedImage readImage(String fileLocation) {
		System.out.println("LOADING: " + fileLocation);
		BufferedImage img = null;
		try {
			URL url = this.getClass().getClassLoader().getResource(fileLocation);
			// System.out.println(url.getPath());
			img = ImageIO.read(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return img;
	}

	private class NewGameMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			JOptionPane.showMessageDialog(null, "This button starts a new game.",
					"New Game", JOptionPane.PLAIN_MESSAGE);
		}
	}

	private class QuitMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			System.exit(0);
		}
	}

	private class AboutMenuListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			JOptionPane.showMessageDialog(null, "RISK v1.0\nCreated By: Ryan Kane",
					"About", JOptionPane.PLAIN_MESSAGE);
		}
	}

	public static void main(String[] args) {
		new App();
	}

	public void trace(Object obj) {
		System.err.println(obj);
	}

	public Country getSelectedCountry(int pixel) {
		if (pixel != 0) {
			Iterator<Country> it = gameboard.getTerritories().iterator();
			while (it.hasNext()) {
				Country country = (Country) it.next();
				if (country.getColorVal() == pixel) {
					return country;
				}
			}
		}
		return null;
	}

	private void popup(String msg, int val) {
		JOptionPane.showMessageDialog(null, msg, TITLE, val);
	}

	private int confirmation() {
		int messageType = JOptionPane.WARNING_MESSAGE;
		String[] options = { "Yes", "No" };
		int response = JOptionPane.showOptionDialog(this, "Are you sure?",
				endPhaseButton.getText() + "?!", 0, messageType, null, options, "No");
		return response;
	}

	private int transfer(int min) {
		int bound = min == 0 ? -1 : -min;
		Object[] vals = new Object[sourceCountry.getArmySize() + bound];
		for (int i = 0; i < vals.length; i++)
			vals[i] = Integer.toString(i + min);
		String val = (String) JOptionPane.showInputDialog(this, "Transfer Amount",
				"Fortify", JOptionPane.QUESTION_MESSAGE, null, vals, "1");
		return Integer.parseInt(val);
	}

	public void fortify() {
		int val = transfer(1);
		if (val > 0) {
			currPlayer.removeUnits(val, sourceCountry);
			currPlayer.addUnits(val, destCountry);
			chooseSourceButton.setEnabled(false);
			chooseDestButton.setEnabled(false);
			actionButton.setEnabled(false);
			updateGUI();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// JOptionPane.showMessageDialog(this, "Message", "Title",
		// JOptionPane.INFORMATION_MESSAGE);
		// players.get(0).setColor(PlayerColors.BLUE.color());
		System.err.print("Changed");

		if (gameState == State.DRAFT) {
			int p = mapPanel.getCurrPixel();
			Country c = getSelectedCountry(p);
			if (c != null) {
				if (c.getPlayer() == currPlayer) {
					currPlayer.draftUnits(1, c);
					updateGUI();
				}
			}
		}

		if (gameState == State.ATTACK || gameState == State.FORTIFY) {
			int p = mapPanel.getCurrPixel();
			Country c = getSelectedCountry(p);
			if (c != null) {
				if (lastButton != null) {
					if (lastButton == chooseSourceButton) {
						if (c.getPlayer() == currPlayer) {
							sourceCountry = c;
							sourceCountryImageLabel.setIcon(c.getImage());
							sourceCountryNameLabel.setText(c.getName());
						} else {
							chooseCountryErr();
						}
					} else if (lastButton == chooseDestButton) {
						Set<Country> neightbors = null;
						try {
							neightbors = gameboard.getNeighbors(sourceCountry);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						if (neightbors.contains(c)) {
							if ((c.getPlayer() != currPlayer && gameState == State.ATTACK)
									|| (c.getPlayer() == currPlayer && gameState == State.FORTIFY)) {
								destCountry = c;
								destCountryImageLabel.setIcon(c.getImage());
								destCountryNameLabel.setText(c.getName());
							} else {
								chooseCountryErr();
							}
						} else {
							String title = "Destination Choice Error";
							String message = "";
							if (gameState == State.ATTACK) {
								message = c.getName()
										+ " is not an ajacent enemy country. \n\nAvailable countries to attack:";
								for (Country n : neightbors) {
									if (n.getPlayer() != currPlayer)
										message += String.format("\n - %s (%s)", n.getName(), n
												.getPlayer().getName());
								}
							} else if (gameState == State.FORTIFY) {
								message = c.getName()
										+ " is not an ajacent allied country. \n\nAvailable countries to fortify:";
								for (Country n : neightbors) {
									if (n.getPlayer() == currPlayer)
										message += String.format("\n - %s", n.getName());
								}
							}
							JOptionPane.showMessageDialog(null, message, title,
									JOptionPane.PLAIN_MESSAGE);
						}
					}
				}
			}
		}

		if (e.getSource() == sourceCountryImageLabel) {
			if (sourceCountry != null) {
				String text = "Owner: " + sourceCountry.getPlayer().getName()
						+ "\nArmy Size: " + sourceCountry.getArmySize();

				JOptionPane.showMessageDialog(null, text, sourceCountry.getName(),
						JOptionPane.PLAIN_MESSAGE);
			}
		}
	}

	public void chooseCountryErr() {
		popup("You cannot pick this country!", 0);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		lastButton = e.getSource();
		// TODO Auto-generated method stub
		if (e.getSource() == actionButton) {
			if (actionButton.getText().equalsIgnoreCase("Attack")) {
				if (sourceCountry != null && destCountry != null) {
					// ****************************************************//
					// Number starting of units
					int iua = sourceCountry.getArmySize(), attackerUnits = iua;
					int iud = destCountry.getArmySize(), defenderUnits = iud;
					float diffFact = 0.25f; // Afford to lose

					int attackerDice = 0;
					int defenderDice = 0;
					while (attackerUnits > 1 && defenderUnits > 0
							&& attackerUnits > (iua * diffFact)) {
						attackerDice = attackerUnits > 3 ? 3 : attackerUnits > 2 ? 2 : 1;
						defenderDice = defenderUnits > 1 ? 2 : 1;

						byte result = Die.rollOff(attackerDice, defenderDice);
						int numberOfAttackWins = result & 0xF;
						int numberOfDefendWins = result >> 4;

						// Decrement units
						attackerUnits -= numberOfDefendWins;
						defenderUnits -= numberOfAttackWins;
						destCountry.getPlayer()
								.removeUnits(numberOfAttackWins, destCountry);
						sourceCountry.getPlayer().removeUnits(numberOfDefendWins,
								sourceCountry);
					}
					updateGUI();
					String msg = "";
					if (attackerUnits > 1 && defenderUnits == 0) {
						capturedTerritory = true;
						int val = transfer(attackerDice);
						String loser = destCountry.getPlayer().getName();
						sourceCountry.getPlayer()
								.winBattle(sourceCountry, destCountry, val);
						capturedTerritory = true;
						updateGUI();
						msg = String.format("%s gained control of %s (%s).", sourceCountry
								.getPlayer().getName(), destCountry.getName(), loser);
					} else if (attackerUnits > (iua * diffFact)) {
						msg = String.format("%s gave up the battle.", sourceCountry
								.getPlayer().getName());
					} else {
						msg = String.format("%s lost the battle.", sourceCountry
								.getPlayer().getName());
					}
					popup(msg, 1);
					// ****************************************************//
				} else {
					popup("Please choose a source and destination", 0);
				}
				clearSelection();
			} else if (actionButton.getText().equalsIgnoreCase("Fortify")) {

				if (sourceCountry != null && destCountry != null) {
					fortify();
				} else {
					popup("You have not selected your countries!", 0);
				}
			}
		}

		if (e.getSource() == chooseSourceButton
				|| e.getSource() == chooseDestButton) {
			// Check this out, yo.
		}

		if (e.getSource() == endPhaseButton) {
			if (endPhaseButton.getText().equalsIgnoreCase(END_DRAFT)) {
				if (currPlayer.getDraftedArmies() > 0) {
					popup(
							"Please draft all of your available armies before proceeding to attack.",
							1);
				} else {
					gameState = State.ATTACK;
					endPhaseButton.setText(END_ATTACK);
					actionButton.setText("Attack");
					actionButton.setEnabled(true);
					chooseSourceButton.setEnabled(true);
					chooseDestButton.setEnabled(true);
					clearSelection();
				}
			} else if (endPhaseButton.getText().equalsIgnoreCase(END_ATTACK)) {
				int response = confirmation();
				if (response == 0) {
					gameState = State.FORTIFY;
					endPhaseButton.setText(END_TURN);
					actionButton.setText("Fortify");
					clearSelection();
				} else {
					popup("Aborted Action!", 1);
				}
			} else if (endPhaseButton.getText().equalsIgnoreCase(END_TURN)) {
				int response = confirmation();
				if (response == 0) {
					// Set up next player
					if (capturedTerritory) {
						//
						// Give the player a card...
						Card draw = deck.draw();
						currPlayer.getHand().add(draw);
						popup(String.format("Card: %s - %s", draw.getCountryName(), draw
								.getUnitType().getName()), 1);
					}
					updateGUI();
					capturedTerritory = false;
					turn++;
					takeTurn();
				} else {
					popup("Aborted Action!", 1);
				}
			}
		}
		updateGUI();
	}
}
