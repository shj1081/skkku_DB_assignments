import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Auction {
	private static Scanner scanner = new Scanner(System.in);
	private static String user_id;
	private static Connection conn;

	enum Category {
		ELECTRONICS,
		BOOKS,
		HOME,
		CLOTHING,
		SPORTINGGOODS,
		OTHERS
	}

	enum Condition {
		NEW,
		LIKE_NEW,
		GOOD,
		ACCEPTABLE
	}

	private static boolean LoginMenu() {
		String userpass;

		System.out.print("----< User Login >\n" +
				" ** To go back, enter 'back' in user_id.\n" +
				"     user ID: ");

		try {
			user_id = scanner.next();
			scanner.nextLine();

			if (user_id.equalsIgnoreCase("back")) {
				return false;
			}

			System.out.print("     password: ");
			userpass = scanner.next();
			scanner.nextLine();

			try {
				// SQL query to check if the user with the given user_id and password exists
				String query = "SELECT * FROM users WHERE user_id = ? AND password = ?";
				var pstmt = conn.prepareStatement(query);
				pstmt.setString(1, user_id);
				pstmt.setString(2, userpass);

				ResultSet rs = pstmt.executeQuery();

				if (rs.next()) {
					user_id = rs.getString("user_id");
					System.out.println("You are successfully logged in.\n");
					return true;
				} else {
					// If no matching user, login fails
					System.out.println("Error: Incorrect user name or password");
					user_id = null;
					return false;
				}
			} catch (InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Please select again.");
				scanner.nextLine(); // Clear buffer
				return false;
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			user_id = null;
			return false;
		}
	}

	private static boolean SellMenu() {
		Category category = null;
		Condition condition = null;
		char choice;
		int buyItNowPrice;
		boolean flag_catg = true, flag_cond = true;

		do {
			System.out.println(
					"----< Sell Item >\n" +
							"---- Choose a category.\n" +
							"    1. Electronics\n" +
							"    2. Books\n" +
							"    3. Home\n" +
							"    4. Clothing\n" +
							"    5. Sporting Goods\n" +
							"    6. Other Categories\n" +
							"    P. Go Back to Previous Menu");

			try {
				choice = scanner.next().charAt(0);
				;
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			flag_catg = true;

			switch ((int) choice) {
				case '1':
					category = Category.ELECTRONICS;
					continue;
				case '2':
					category = Category.BOOKS;
					continue;
				case '3':
					category = Category.HOME;
					continue;
				case '4':
					category = Category.CLOTHING;
					continue;
				case '5':
					category = Category.SPORTINGGOODS;
					continue;
				case '6':
					category = Category.OTHERS;
					continue;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_catg = false;
					continue;
			}
		} while (!flag_catg);

		do {
			System.out.println(
					"---- Select the condition of the item to sell.\n" +
							"   1. New\n" +
							"   2. Like-new\n" +
							"   3. Used (Good)\n" +
							"   4. Used (Acceptable)\n" +
							"   P. Go Back to Previous Menu");

			try {
				choice = scanner.next().charAt(0);
				;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			flag_cond = true;

			switch (choice) {
				case '1':
					condition = Condition.NEW;
					break;
				case '2':
					condition = Condition.LIKE_NEW;
					break;
				case '3':
					condition = Condition.GOOD;
					break;
				case '4':
					condition = Condition.ACCEPTABLE;
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_cond = false;
					continue;
			}
		} while (!flag_cond);

		// Item Details
		try {
			System.out.println("---- Description of the item (one line): ");
			String description = scanner.nextLine().trim();
			if (description.isEmpty()) {
				System.out.println("Error: Description cannot be empty.");
				return false;
			}

			System.out.println("---- Buy-It-Now price: ");
			while (true) {
				try {
					buyItNowPrice = Integer.parseInt(scanner.nextLine());
					if (buyItNowPrice <= 0) {
						System.out.println("Error: Price must be greater than 0.");
						continue;
					}
					break;
				} catch (NumberFormatException e) {
					System.out.println("Error: Please enter a valid number for the price.");
				}
			}

			System.out.print("---- Bid closing date and time (YYYY-MM-DD HH:MM): ");
			String dateStr = scanner.nextLine().trim();
			LocalDateTime bidClosingDate;
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
				bidClosingDate = LocalDateTime.parse(dateStr, formatter);

				// Validate that closing date is in the future
				if (bidClosingDate.isBefore(LocalDateTime.now())) {
					System.out.println("Error: Bid closing date must be in the future.");
					return false;
				}
			} catch (Exception e) {
				System.out.println("Error: Invalid date format. Please use YYYY-MM-DD HH:MM");
				return false;
			}

			// Insert item into the database
			String sql = "INSERT INTO item (category, condition, description, buy_it_now_price, bid_closing_date, seller_id) "
					+
					"VALUES (?, ?, ?, ?, ?, ?)";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, category.toString());
				pstmt.setString(2, condition.toString());
				pstmt.setString(3, description);
				pstmt.setInt(4, buyItNowPrice);
				pstmt.setTimestamp(5, Timestamp.valueOf(bidClosingDate));
				pstmt.setString(6, user_id);

				pstmt.executeUpdate();
				System.out.println("Your item has been successfully listed.\n");
				return true;
			}

		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
			return false;
		} catch (Exception e) {
			System.out.println("Error: Invalid input is entered. Going back to the previous menu.");
			return false;
		}
	}

	private static boolean SignupMenu() {
		String new_user_id, userpass;

		System.out.print("----< Sign Up >\n" +
				" ** To go back, enter 'back' in user ID.\n" +
				"---- user ID: ");

		try {
			new_user_id = scanner.next();
			scanner.nextLine();

			if (new_user_id.equalsIgnoreCase("back")) {
				return false; // Go back if user enters 'back'
			}

			System.out.print("---- password: ");
			userpass = scanner.next();
			scanner.nextLine();

			System.out.print("---- Is this user an administrator? (Y/N): ");
			String isAdmin = scanner.nextLine();

			// Validate user input
			if (new_user_id.trim().isEmpty() || userpass.trim().isEmpty()) {
				System.out.println("Error: User ID and password cannot be empty");
				return false;
			}

			boolean isAdminFlag = isAdmin.equalsIgnoreCase("Y");

			// First check if user already exists
			String checkSQL = "SELECT user_id FROM users WHERE user_id = ?";
			try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
				checkStmt.setString(1, new_user_id);
				ResultSet rs = checkStmt.executeQuery();

				if (rs.next()) {
					System.out.println("Error: User ID already exists. Please choose a different ID.");
					return false;
				}
			}

			// Insert new user
			String insertSQL = "INSERT INTO users (user_id, password, is_admin) VALUES (?, ?, ?)";
			try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
				pstmt.setString(1, new_user_id);
				pstmt.setString(2, userpass);
				pstmt.setBoolean(3, isAdminFlag);

				int rowsInserted = pstmt.executeUpdate();

				if (rowsInserted > 0) {
					System.out.println("Your account has been successfully created.\n");
					return true;
				} else {
					System.out.println("Error: Failed to create account. Please try again.");
					return false;
				}
			}
		} catch (InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Please try again.");
			scanner.nextLine(); // Clear buffer
			return false;
		} catch (SQLException e) {
			System.out.println("Error: Database error occurred. " + e.getMessage());
			return false;
		}
	}

	private static boolean AdminMenu() {
		char choice;
		String admin_id, adminpass;
		String keyword, seller;

		System.out.print("----< Login as Administrator >\n" +
				" ** To go back, enter 'back' in user ID.\n" +
				"---- admin ID: ");

		try {
			admin_id = scanner.next();
			scanner.nextLine();

			if (admin_id.equalsIgnoreCase("back")) {
				return false; // Return to previous menu if 'back' is entered
			}

			System.out.print("---- password: ");
			adminpass = scanner.nextLine();

			// SQL query to check if the user with the given user_id and password exists
			// and is an admin
			String sql = "SELECT is_admin FROM users WHERE user_id = ? AND password = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setString(1, admin_id);
				pstmt.setString(2, adminpass);
				ResultSet rs = pstmt.executeQuery();

				if (!rs.next() || !rs.getBoolean("is_admin")) {
					System.out.println("Error: Invalid admin credentials.");
					return false;
				}
			} catch (SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				return false;
			}
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		do {
			System.out.println(
					"----< Admin menu > \n" +
							"    1. Print Sold Items per Category \n" +
							"    2. Print Account Balance for Seller \n" +
							"    3. Print Seller Ranking \n" +
							"    4. Print Buyer Ranking \n" +
							"    P. Go Back to Previous Menu");

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			switch (choice) {
				case '1':
					System.out.println("----Enter Category to search: ");
					keyword = scanner.nextLine();

					// check if the input is in category
					if (!keyword.equalsIgnoreCase("electronics") && !keyword.equalsIgnoreCase("books") &&
							!keyword.equalsIgnoreCase("home") && !keyword.equalsIgnoreCase("clothing") &&
							!keyword.equalsIgnoreCase("sporting goods") && !keyword.equalsIgnoreCase("others")) {
						System.out.println("Error: Invalid input is entered. Try again.");
						continue;
					}

					printSoldItemsPerCategory(keyword);
					break;
				case '2':
					System.out.println("---- Enter Seller ID to search: ");
					seller = scanner.nextLine();
					printAccountBalanceForSeller(seller);
					break;
				case '3':
					printSellerRanking();
					break;
				case '4':
					printBuyerRanking();
					break;
				case 'p':
				case 'P':
					user_id = null;
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
			}
		} while (true);
	}

	private static void printSoldItemsPerCategory(String category) {
		processAllExpiredItems();

		String sql = "SELECT i.description, b.purchase_date, b.seller_id, b.buyer_id, " +
				"b.amount_due_buyers_need_to_pay, b.commission_fee " +
				"FROM billing b " +
				"JOIN item i ON b.sold_item_id = i.id " +
				"WHERE i.category = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, category.toUpperCase());
			ResultSet rs = pstmt.executeQuery();

			System.out.printf("%-30s | %-35s | %-12s | %-12s | %-12s | %-12s\n",
					"sold item", "sold date", "seller ID", "buyer ID", "price", "commissions");
			System.out.println("-".repeat(128));

			while (rs.next()) {
				System.out.printf("%-30s | %-35s | %-12s | %-12s | %-12d | %-12d\n",
						rs.getString("description"),
						rs.getTimestamp("purchase_date"),
						rs.getString("seller_id"),
						rs.getString("buyer_id"),
						rs.getInt("amount_due_buyers_need_to_pay"),
						rs.getInt("commission_fee"));
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		}
	}

	private static void printAccountBalanceForSeller(String sellerID) {
		processAllExpiredItems();

		// check if the user_id exists
		String checkSql = "SELECT * FROM users WHERE user_id = ?";
		try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
			checkStmt.setString(1, sellerID);
			if (!checkStmt.executeQuery().next()) {
				System.out.println("Error: Invalid seller ID.");
				return;
			}
		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
			return;
		}

		String sql = "SELECT i.description, b.purchase_date, b.buyer_id, " +
				"b.amount_due_buyers_need_to_pay, b.commission_fee " +
				"FROM billing b " +
				"JOIN item i ON b.sold_item_id = i.id " +
				"WHERE b.seller_id = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, sellerID);
			ResultSet rs = pstmt.executeQuery();

			System.out.printf("%-30s | %-35s | %-10s | %-10s | %-12s\n",
					"sold item", "sold date", "buyer ID", "price", "commissions");
			System.out.println("-".repeat(108));

			while (rs.next()) {
				System.out.printf("%-30s | %-35s | %-10s | %-10d | %-12d\n",
						rs.getString("description"),
						rs.getTimestamp("purchase_date"),
						rs.getString("buyer_id"),
						rs.getInt("amount_due_buyers_need_to_pay"),
						rs.getInt("commission_fee"));
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		}
	}

	private static void printSellerRanking() {
		processAllExpiredItems();

		String sql = "SELECT seller_id, " +
				"COUNT(*) AS items_sold, " +
				"SUM(amount_of_money_sellers_need_to_get_paid) AS total_profit " +
				"FROM billing " +
				"GROUP BY seller_id " +
				"ORDER BY total_profit DESC";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();

			System.out.printf("%-10s | %-15s | %-35s\n",
					"seller ID", "# of items sold", "Total Profit (excluding commissions)");
			System.out.println("-".repeat(65));

			while (rs.next()) {
				System.out.printf("%-10s | %-15s | %-35s\n",
						rs.getString("seller_id"),
						String.valueOf(rs.getInt("items_sold")),
						String.valueOf(rs.getInt("total_profit")));
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		}
	}

	private static void printBuyerRanking() {
		processAllExpiredItems();

		String sql = "SELECT buyer_id, " +
				"COUNT(*) AS items_purchased, " +
				"SUM(amount_due_buyers_need_to_pay) AS total_spent " +
				"FROM billing " +
				"GROUP BY buyer_id " +
				"ORDER BY total_spent DESC";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			ResultSet rs = pstmt.executeQuery();

			System.out.printf("%-10s | %-20s | %-20s\n",
					"buyer ID", "# of items purchased", "Total Money Spent");
			System.out.println("-".repeat(55));

			while (rs.next()) {
				System.out.printf("%-10s | %-20s | %-20s\n", // Changed %d to %s
						rs.getString("buyer_id"),
						String.valueOf(rs.getInt("items_purchased")), // Convert to String
						String.valueOf(rs.getInt("total_spent"))); // Convert to String
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
		}
	}

	public static void CheckSellStatus() {
		processAllExpiredItems(); // Process any expired items first

		String sql = "SELECT i.id AS item_id, " +
				"i.description, " +
				"i.condition, " +
				"b.bidder_id AS buyer_id, " +
				"b.bid_price AS bidding_price, " +
				"b.date_posted AS bidding_date " +
				"FROM item i " +
				"LEFT JOIN bid b ON i.id = b.item_id " +
				"WHERE i.seller_id = ? " +
				"ORDER BY i.id, b.bid_price DESC";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, user_id);
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n----< Your Items Listed on Auction >----");
			System.out.printf("%-10s | %-30s | %-12s | %-10s | %-15s | %-16s\n",
					"Item ID", "Description", "Condition", "Buyer ID", "Bidding Price", "Bidding Date/Time");
			System.out.println("-".repeat(115));

			int currentItemId = -1;
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

			while (rs.next()) {
				int itemId = rs.getInt("item_id");

				// Check if the current item is new
				if (itemId != currentItemId) {
					// Print a divider line for each new item (but not before the first item)
					if (currentItemId != -1) {
						System.out.println("*".repeat(115));
					}

					// Print item details with placeholders for bid info
					System.out.printf("%-10d | %-30s | %-12s | %-10s | %-15s | %-16s\n",
							itemId,
							rs.getString("description"),
							rs.getString("condition"),
							"", // Buyer ID placeholder
							"", // Bidding Price placeholder
							""); // Bidding Date/Time placeholder

					// Update current item ID
					currentItemId = itemId;
				}

				// Print bid details if they exist
				if (rs.getString("buyer_id") != null) {
					System.out.printf("%-10s | %-30s | %-12s | %-10s | %-15d | %-16s\n",
							"", // Indent to align bid details under item (empty column for item_id)
							"", // Empty column for description
							"", // Empty column for condition
							rs.getString("buyer_id"),
							rs.getInt("bidding_price"),
							rs.getTimestamp("bidding_date").toLocalDateTime().format(dateFormatter));
				}
			}

		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
		}
	}

	public static void CheckBuyStatus() {
		processAllExpiredItems(); // Process any expired items first

		String sql = "WITH UserBids AS ( " +
				"    SELECT DISTINCT ON (item_id) " +
				"        item_id, " +
				"        bid_price as your_bid " +
				"    FROM bid " +
				"    WHERE bidder_id = ? " +
				"    ORDER BY item_id, date_posted DESC " +
				"), " +
				"MaxBids AS ( " +
				"    SELECT " +
				"        b.item_id, " +
				"        b.bid_price as highest_bid, " +
				"        b.bidder_id as highest_bidder " +
				"    FROM bid b " +
				"    INNER JOIN ( " +
				"        SELECT " +
				"            item_id, " +
				"            MAX(bid_price) as max_bid_price " +
				"        FROM bid " +
				"        GROUP BY item_id " +
				"    ) max_bids ON b.item_id = max_bids.item_id AND b.bid_price = max_bids.max_bid_price " +
				"    ORDER BY b.item_id, b.date_posted ASC " +
				") " +
				"SELECT " +
				"    i.id AS item_id, " +
				"    i.description, " +
				"    i.bid_closing_date, " +
				"    ub.your_bid, " +
				"    COALESCE(mb.highest_bid, 0) as highest_bid, " +
				"    mb.highest_bidder " +
				"FROM item i " +
				"JOIN UserBids ub ON i.id = ub.item_id " +
				"LEFT JOIN MaxBids mb ON i.id = mb.item_id " +
				"ORDER BY i.bid_closing_date DESC";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, user_id);
			ResultSet rs = pstmt.executeQuery();

			System.out.println("\n----< Your Bidding Status >----");
			System.out.printf("%-10s | %-30s | %-20s | %-20s | %-20s | %-20s\n",
					"Item ID", "Description", "Highest Bidder", "Highest Bidding Price", "Your Bidding Price",
					"Closing Date/Time");
			System.out.println("-".repeat(132));

			while (rs.next()) {
				double yourBid = rs.getDouble("your_bid");
				double highestBid = rs.getDouble("highest_bid");
				String highestBidder = rs.getString("highest_bidder");
				Timestamp bidClosingDate = rs.getTimestamp("bid_closing_date");

				// Format the closing date to "yyyy-MM-dd HH:mm"
				String formattedClosingDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(bidClosingDate);

				System.out.printf("%-10d | %-30s | %-20s | %-20.0f | %-20.0f | %-20s\n",
						rs.getInt("item_id"),
						rs.getString("description"),
						highestBidder != null ? highestBidder : "N/A",
						highestBid,
						yourBid,
						formattedClosingDate);
			}

		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
		}
	}

	public static boolean BuyItem() throws SQLException {

		Category category = null;
		Condition condition = null;
		char choice;
		String keyword, seller, datePosted;
		boolean flag_catg = true, flag_cond = true;

		// Step 1: Select Category
		do {
			System.out.println("----< Select category > : \n" +
					"    1. Electronics\n" +
					"    2. Books\n" +
					"    3. Home\n" +
					"    4. Clothing\n" +
					"    5. Sporting Goods\n" +
					"    6. Other categories\n" +
					"    7. Any category\n" +
					"    P. Go Back to Previous Menu");

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				return false;
			}

			flag_catg = true;
			switch (choice) {
				case '1':
					category = Category.ELECTRONICS;
					break;
				case '2':
					category = Category.BOOKS;
					break;
				case '3':
					category = Category.HOME;
					break;
				case '4':
					category = Category.CLOTHING;
					break;
				case '5':
					category = Category.SPORTINGGOODS;
					break;
				case '6':
					category = Category.OTHERS;
					break;
				case '7':
					category = null;
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_catg = false;
			}
		} while (!flag_catg);

		// Step 2: Select Condition
		do {
			System.out.println("----< Select the condition > \n" +
					"   1. New\n" +
					"   2. Like-new\n" +
					"   3. Good\n" +
					"   4. Acceptable\n" +
					"   P. Go Back to Previous Menu");

			try {
				choice = scanner.next().charAt(0);
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				return false;
			}

			flag_cond = true;
			switch (choice) {
				case '1':
					condition = Condition.NEW;
					break;
				case '2':
					condition = Condition.LIKE_NEW;
					break;
				case '3':
					condition = Condition.GOOD;
					break;
				case '4':
					condition = Condition.ACCEPTABLE;
					break;
				case 'p':
				case 'P':
					return false;
				default:
					System.out.println("Error: Invalid input is entered. Try again.");
					flag_cond = false;
			}
		} while (!flag_cond);

		// Step 3: Get Additional Filters
		try {
			System.out.println("---- Enter keyword to search the description : ");
			keyword = scanner.nextLine().trim();

			System.out.println("---- Enter Seller ID to search : ");
			System.out.println(" ** Enter 'any' if you want to see items from any seller. ");
			seller = scanner.nextLine();

			System.out.println("---- Enter date posted (YYYY-MM-DD): ");
			System.out.println(" ** This will search items that have been posted after the designated date.");
			datePosted = scanner.nextLine();
		} catch (java.util.InputMismatchException e) {
			System.out.println("Error: Invalid input is entered. Try again.");
			return false;
		}

		// Step 4: Display Items
		StringBuilder sql = new StringBuilder(
				"SELECT i.*, " +
						"EXTRACT(EPOCH FROM (i.bid_closing_date - CURRENT_TIMESTAMP)) / 60 AS minutes_left, " +
						"b.highest_bid, " +
						"u.user_id AS highest_bidder " +
						"FROM item i " +
						"LEFT JOIN (" +
						"    SELECT item_id, MAX(bid_price) AS highest_bid, " +
						"           (SELECT bidder_id FROM bid " +
						"            WHERE bid_price = (SELECT MAX(bid_price) FROM bid WHERE item_id = b.item_id) " +
						"            AND item_id = b.item_id LIMIT 1) AS highest_bidder_id " +
						"    FROM bid b " +
						"    GROUP BY item_id " +
						") b ON i.id = b.item_id " +
						"LEFT JOIN users u ON b.highest_bidder_id = u.user_id " +
						"WHERE i.status = 'AVAILABLE' " +
						"AND i.bid_closing_date > CURRENT_TIMESTAMP ");

		if (category != null) {
			sql.append(" AND i.category = ? ");
		}
		if (condition != null) {
			sql.append(" AND i.condition = ? ");
		}
		if (!keyword.isEmpty()) {
			sql.append(" AND i.description ILIKE ? ");
			keyword = "%" + keyword + "%"; // Prepare wildcard for partial matching
		}
		if (!seller.equalsIgnoreCase("any")) {
			sql.append(" AND i.seller_id = ? ");
		}
		if (!datePosted.isEmpty()) {
			sql.append(" AND i.date_posted >= ? ");
		}
		sql.append(" GROUP BY i.id, b.highest_bid, u.user_id");

		HashSet<Integer> validItemIds = new HashSet<>(); // Store valid item IDs

		try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
			int paramIndex = 1;
			if (category != null)
				pstmt.setString(paramIndex++, category.name());
			if (condition != null)
				pstmt.setString(paramIndex++, condition.name());
			if (!keyword.isEmpty())
				pstmt.setString(paramIndex++, keyword);
			if (!seller.equalsIgnoreCase("any"))
				pstmt.setString(paramIndex++, seller);
			if (!datePosted.isEmpty())
				pstmt.setDate(paramIndex++, java.sql.Date.valueOf(datePosted));

			ResultSet rs = pstmt.executeQuery();
			System.out.printf("%-8s | %-30s | %-9s | %-15s | %-11s | %-20s | %-11s | %-35s\n",
					"Item ID", "Description", "Condition", "Buy-It-Now Price", "Current Bid", "Highest Bidder",
					"Time Left", "Bid Closing Date");
			System.out.println(
					"-----------------------------------------------------------------------------------------------------------------------------------------------");

			// Display each row from the result set and store valid item IDs
			while (rs.next()) {
				int itemId = rs.getInt("id");
				validItemIds.add(itemId); // Add item ID to the set

				int minutes_left = rs.getInt("minutes_left");
				String timeLeft = minutes_left > 0 ? minutes_left + " min" : "Closed";

				System.out.printf("%-8d | %-30s | %-9s | %-15d | %-11d | %-20s | %-11s | %-35s\n",
						itemId,
						rs.getString("description"),
						rs.getString("condition"),
						rs.getInt("buy_it_now_price"),
						rs.getInt("highest_bid"),
						rs.getObject("highest_bidder") != null ? rs.getString("highest_bidder") : "None",
						timeLeft,
						rs.getTimestamp("bid_closing_date").toString());

			}

			// Step 5: Select Item and Bid or Buy
			System.out.println("---- Select Item ID to buy or bid: ");
			String buyChoice = scanner.nextLine();

			if (buyChoice.equalsIgnoreCase("none"))
				return true;

			int selectedItemId;
			try {
				selectedItemId = Integer.parseInt(buyChoice);
			} catch (NumberFormatException e) {
				System.out.println("Error: Invalid input. Please enter a valid Item ID number.");
				return false;
			}

			// Check if item ID is in validItemIds
			if (!validItemIds.contains(selectedItemId)) {
				System.out.println("Error: Invalid Item ID. Please choose from the listed items.");
				return false;
			}

			int bidPrice;
			System.out.println("---- Enter your bid price: ");
			try {
				bidPrice = scanner.nextInt();
				scanner.nextLine(); // Clear the buffer
			} catch (InputMismatchException e) {
				System.out.println("Error: Invalid input. Please enter a numeric bid price.");
				scanner.nextLine(); // Clear the buffer
				return false;
			}

			// Check if bid price is higher than current highest bid
			String currentBidSQL = "SELECT COALESCE(MAX(bid_price), 0) as highest_bid FROM bid WHERE item_id = ?";
			try (PreparedStatement bidCheckStmt = conn.prepareStatement(currentBidSQL)) {
				bidCheckStmt.setInt(1, selectedItemId);
				ResultSet bidRS = bidCheckStmt.executeQuery();
				if (bidRS.next() && bidPrice <= bidRS.getInt("highest_bid")) {
					System.out.println("Error: Bid price must be higher than current highest bid.");
					return false;
				}
			} catch (SQLException e) {
				e.printStackTrace(); // Handle SQL exceptions
				return false;
			}

			// Step 6: Check and Place Bid or Buy
			String getPriceSQL = "SELECT buy_it_now_price, seller_id FROM item WHERE id = ?";
			try (PreparedStatement priceStmt = conn.prepareStatement(getPriceSQL)) {
				priceStmt.setInt(1, selectedItemId);
				ResultSet priceRS = priceStmt.executeQuery();

				if (priceRS.next()) {
					int buyItNowPrice = priceRS.getInt("buy_it_now_price");
					String sellerId = priceRS.getString("seller_id");

					// Prevent user from bidding on their own item
					if (sellerId == user_id) {
						System.out.println("Error: You cannot bid on your own items.");
						return false;
					}

					// Purchase item if bid price meets or exceeds buy-it-now price
					if (bidPrice >= buyItNowPrice) {
						System.out.println("Congratulations, the item is yours now.\n");

						// Insert bid into bid table
						String bidSQL = "INSERT INTO bid (bidder_id, item_id, bid_price) VALUES (?, ?, ?)";
						try (PreparedStatement bidStmt = conn.prepareStatement(bidSQL)) {
							bidStmt.setString(1, user_id);
							bidStmt.setInt(2, selectedItemId);
							bidStmt.setInt(3, buyItNowPrice);
							bidStmt.executeUpdate();
						}

						// Insert transaction into billing
						String billingSQL = "INSERT INTO billing (sold_item_id, seller_id, buyer_id, amount_due_buyers_need_to_pay) VALUES (?, ?, ?, ?)";
						try (PreparedStatement billingStmt = conn.prepareStatement(billingSQL)) {
							conn.setAutoCommit(false); // Start transaction
							billingStmt.setInt(1, selectedItemId);
							billingStmt.setString(2, sellerId);
							billingStmt.setString(3, user_id);
							billingStmt.setInt(4, buyItNowPrice);
							billingStmt.executeUpdate();

							// Update item status
							String updateSQL = "UPDATE item SET status = 'SOLD' WHERE id = ?";
							try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
								updateStmt.setInt(1, selectedItemId);
								updateStmt.executeUpdate();
							}
							conn.commit();
						} catch (SQLException e) {
							conn.rollback();
							throw e;
						} finally {
							conn.setAutoCommit(true);
						}
					} else {
						System.out.println("Congratulations, you are the highest bidder.\n");

						// Insert bid into bid table
						String bidSQL = "INSERT INTO bid (bidder_id, item_id, bid_price) VALUES (?, ?, ?)";
						try (PreparedStatement bidStmt = conn.prepareStatement(bidSQL)) {
							bidStmt.setString(1, user_id);
							bidStmt.setInt(2, selectedItemId);
							bidStmt.setInt(3, bidPrice);
							bidStmt.executeUpdate();
						}
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("SQLException: " + e.getMessage());
			return false;
		}

		return true;
	}

	public static void CheckAccount() throws SQLException {

		processAllExpiredItems();

		// Display sold items
		String sqlSoldItems = "SELECT item.category, " +
				"item.id AS item_id, " +
				"billing.purchase_date AS sold_date, " +
				"billing.amount_due_buyers_need_to_pay AS sold_price, " +
				"billing.buyer_id, " +
				"CAST(billing.commission_fee AS VARCHAR) AS commission_fee " +
				"FROM billing " +
				"JOIN item ON billing.sold_item_id = item.id " +
				"WHERE billing.seller_id = ?";

		try (PreparedStatement pstmtSold = conn.prepareStatement(sqlSoldItems)) {
			pstmtSold.setString(1, user_id);
			ResultSet rsSold = pstmtSold.executeQuery();

			System.out.println("[Sold Items] \n");
			System.out.printf(
					"%-15s | %-9s | %-35s | %-12s | %-10s | %-11s\n",
					"item category", "item ID", "sold date", "sold price", "buyer ID", "commission fee");
			System.out.println("-".repeat(100));

			while (rsSold.next()) {
				System.out.printf("%-15s | %-9s | %-35s | %-12s | %-10s | %-11s\n",
						rsSold.getString("category"),
						String.valueOf(rsSold.getInt("item_id")),
						rsSold.getTimestamp("sold_date").toString(),
						String.valueOf(rsSold.getInt("sold_price")),
						rsSold.getString("buyer_id"),
						rsSold.getString("commission_fee"));
			}
		} catch (SQLException e) {
			System.out.println("SQLException (Sold Items): " + e.getMessage());
		}

		// Display purchased items
		String sqlPurchasedItems = "SELECT item.category, " +
				"item.id AS item_id, " +
				"billing.purchase_date AS purchased_date, " +
				"CAST(billing.amount_due_buyers_need_to_pay AS VARCHAR) AS purchased_price, " +
				"billing.seller_id " +
				"FROM billing " +
				"JOIN item ON billing.sold_item_id = item.id " +
				"WHERE billing.buyer_id = ?";

		try (PreparedStatement pstmtPurchased = conn.prepareStatement(sqlPurchasedItems)) {
			pstmtPurchased.setString(1, user_id);
			ResultSet rsPurchased = pstmtPurchased.executeQuery();

			System.out.println("\n[Purchased Items] \n");
			System.out.printf(
					"%-15s | %-9s | %-35s | %-17s | %-9s\n",
					"item category", "item ID", "purchased date", "purchased price", "seller ID");
			System.out.println("-".repeat(93));

			while (rsPurchased.next()) {
				System.out.printf("%-15s | %-9s | %-35s | %-17s | %-9s\n",
						rsPurchased.getString("category"),
						String.valueOf(rsPurchased.getInt("item_id")),
						rsPurchased.getTimestamp("purchased_date").toString(),
						rsPurchased.getString("purchased_price"),
						rsPurchased.getString("seller_id"));
			}
		} catch (SQLException e) {
			System.out.println("SQLException (Purchased Items): " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		char choice;
		boolean ret;

		if (args.length < 2) {
			System.out.println("Usage: java Auction postgres_id password");
			System.exit(1);
		}

		try {
			conn = DriverManager.getConnection("jdbc:postgresql://localhost/s20310083", "s20310083", "changethis");
		} catch (SQLException e) {
			System.out.println("SQLException : " + e);
			System.exit(1);
		}

		do {
			user_id = null;
			System.out.println(
					"----< Login menu >\n" +
							"----(1) Login\n" +
							"----(2) Sign up\n" +
							"----(3) Login as Administrator\n" +
							"----(Q) Quit");

			try {
				choice = scanner.next().charAt(0);
				;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			try {
				switch ((int) choice) {
					case '1':
						ret = LoginMenu();
						if (!ret)
							continue;
						break;
					case '2':
						ret = SignupMenu();
						if (!ret)
							continue;
						break;
					case '3':
						ret = AdminMenu();
						if (!ret)
							continue;
					case 'q':
					case 'Q':
						System.out.println("Good Bye");
						if (scanner != null) {
							scanner.close();
						}
						if (conn != null && !conn.isClosed()) {
							conn.close();
						}
						System.exit(0);
					default:
						System.out.println("Error: Invalid input is entered. Try again.");
				}
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
			}
		} while (user_id == null || user_id.equalsIgnoreCase("back"));

		// logged in as a normal user
		do {
			System.out.println(
					"---< Main menu > :\n" +
							"----(1) Sell Item\n" +
							"----(2) Status of Your Item Listed on Auction\n" +
							"----(3) Buy Item\n" +
							"----(4) Check Status of your Bid \n" +
							"----(5) Check your Account \n" +
							"----(Q) Quit");

			try {
				choice = scanner.next().charAt(0);
				;
				scanner.nextLine();
			} catch (java.util.InputMismatchException e) {
				System.out.println("Error: Invalid input is entered. Try again.");
				continue;
			}

			try {
				switch (choice) {
					case '1':
						ret = SellMenu();
						if (!ret)
							continue;
						break;
					case '2':
						CheckSellStatus();
						break;
					case '3':
						ret = BuyItem();
						if (!ret)
							continue;
						break;
					case '4':
						CheckBuyStatus();
						break;
					case '5':
						CheckAccount();
						break;
					case 'q':
					case 'Q':
						System.out.println("Good Bye");
						if (scanner != null) {
							scanner.close();
						}
						if (conn != null && !conn.isClosed()) {
							conn.close();
						}
						System.exit(0);
				}
			} catch (SQLException e) {
				System.out.println("SQLException : " + e);
				System.exit(1);
			}
		} while (true);
	} // End of main

	// helper function to update item info whose ibd_closing_date is expired
	private static void processAllExpiredItems() {
		// Find all expired items that haven't been processed yet
		String findExpiredSQL = "SELECT i.id, i.seller_id, " +
				"b.bidder_id as winner_id, " +
				"b.bid_price as winning_bid " +
				"FROM item i " +
				"LEFT JOIN ( " +
				"    SELECT DISTINCT ON (item_id) " +
				"        item_id, bidder_id, bid_price " +
				"    FROM bid " +
				"    ORDER BY item_id, bid_price DESC, date_posted ASC " +
				") b ON i.id = b.item_id " +
				"WHERE i.status = 'AVAILABLE' " +
				"AND i.bid_closing_date < CURRENT_TIMESTAMP " +
				"AND i.id NOT IN (SELECT sold_item_id FROM billing)";

		try {
			conn.setAutoCommit(false);

			try (PreparedStatement findStmt = conn.prepareStatement(findExpiredSQL)) {
				ResultSet rs = findStmt.executeQuery();

				while (rs.next()) {
					int itemId = rs.getInt("id");
					String winnerId = rs.getString("winner_id");
					String sellerId = rs.getString("seller_id");

					if (winnerId != null) {
						// Insert into billing if there was a winner
						String billingSQL = "INSERT INTO billing (sold_item_id, seller_id, buyer_id, amount_due_buyers_need_to_pay) VALUES (?, ?, ?, ?)";
						try (PreparedStatement billingStmt = conn.prepareStatement(billingSQL)) {
							billingStmt.setInt(1, itemId);
							billingStmt.setString(2, sellerId);
							billingStmt.setString(3, winnerId);
							billingStmt.setInt(4, rs.getInt("winning_bid"));
							billingStmt.executeUpdate();
						}
					}

					// Update item status
					String updateSQL = "UPDATE item SET status = ? WHERE id = ?";
					try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
						updateStmt.setString(1, winnerId != null ? "SOLD" : "EXPIRED");
						updateStmt.setInt(2, itemId);
						updateStmt.executeUpdate();
					}
				}

				conn.commit();
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			}

		} catch (SQLException e) {
			System.out.println("Database error: " + e.getMessage());
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e) {
				System.out.println("Error restoring auto-commit: " + e.getMessage());
			}
		}
	}

} // End of class
