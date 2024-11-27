-- Insert users (to satisfy foreign key constraints for items and bids)
INSERT INTO users (user_id, password, is_admin) VALUES
('user1', 'password1', FALSE),
('user2', 'password2', FALSE),
('user3', 'password3', FALSE),
('user4', 'password4', FALSE),
('user5', 'password5', FALSE),
('user6', 'password6', FALSE),
('user7', 'password7', FALSE),
('user8', 'password8', FALSE),
('admin', 'admin', TRUE);

-- Insert items (with adjusted bid closing dates)
INSERT INTO item (category, condition, description, buy_it_now_price, bid_closing_date, seller_id, status) VALUES
('ELECTRONICS', 'NEW', 'iPhone 15 Pro', 100000, '2024-11-20 18:00:00', 'user1', 'SOLD'),
('CLOTHING', 'LIKE_NEW', 'Nike Air Max', 80000, '2024-11-21 18:00:00', 'user2','SOLD'),
('BOOKS', 'GOOD', 'Harry Potter Collection', 50000, '2024-11-22 18:00:00', 'user3', 'AVAILABLE'),
('HOME', 'ACCEPTABLE', 'Coffee Table', 30000, '2024-11-23 18:00:00', 'user1', 'AVAILABLE'),
('SPORTINGGOODS', 'NEW', 'Tennis Racket', 45000, '2024-11-24 18:00:00', 'user2', 'AVAILABLE'),
('ELECTRONICS', 'NEW', 'Samsung Galaxy S24', 95000, '2024-11-25 18:00:00', 'user1', 'SOLD'),
('BOOKS', 'GOOD', 'Lord of the Rings Trilogy', 30000, '2024-11-26 18:00:00', 'user3', 'SOLD'),
('HOME', 'GOOD', 'Leather Sofa', 120000, '2024-12-01 18:00:00', 'user6', 'AVAILABLE'),
('OTHERS', 'NEW', 'Lego Star Wars Set', 55000, '2024-12-05 18:00:00', 'user2', 'AVAILABLE'),
('HOME', 'NEW', 'Dyson Vacuum Cleaner', 85000, '2024-11-04 20:40:00', 'user3', 'AVAILABLE');

-- Insert bids (referencing `item_id` from `item` and `bidder_id` from `users`)
INSERT INTO bid (bidder_id, item_id, bid_price) VALUES
('user4', 1, 90000),
('user5', 1, 100000),  -- Buy-it-now price reached
('user4', 2, 70000),
('user5', 2, 75000),
('user4', 2, 82000),
('user7', 6, 95000),   -- Buy-it-now price reached
('user4', 7, 29000),
('user5', 7, 30000),   -- Buy-it-now price reached
('user8', 8, 35000),
('user8', 10, 75000),
('user1', 9, 20000);

-- Insert billing data (for sold items, referencing `sold_item_id` from `item`, `seller_id` and `buyer_id` from `users`)
INSERT INTO billing (sold_item_id, purchase_date, seller_id, buyer_id, amount_due_buyers_need_to_pay) VALUES
-- Item 1 sold at buy-it-now price
(1, '2024-11-20 18:00:00', 'user1', 'user5', 100000),
-- Item 2 highest bid after bid closing
(2, '2024-11-21 18:00:00', 'user2', 'user4', 82000),
-- Item 6 sold at buy-it-now price
(6, '2024-11-25 18:00:00', 'user1', 'user7', 95000),
-- Item 7 sold at buy-it-now price
(7, '2024-11-26 18:00:00', 'user3', 'user5', 30000);


