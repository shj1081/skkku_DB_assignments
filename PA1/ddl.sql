--Drop All Tables for preventing error
drop table if exists billing;
drop table if exists bid;
drop table if exists item;
drop table if exists users;

-- Users Table
create table users (
    user_id varchar(255) primary key,
    password varchar(255) not null,
    is_admin boolean default false
);

-- Item Table
create table item (
    id serial primary key,
    category varchar(255) not null,
    condition varchar(255) not null,
    description varchar(255),
    buy_it_now_price int check (buy_it_now_price > 0),
    bid_closing_date timestamp not null,
    seller_id varchar(255) not null,
    date_posted timestamp default current_timestamp,
    status varchar(20) DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'SOLD', 'EXPIRED')),
    
    constraint fk_seller_id foreign key(seller_id) references users(user_id) on delete cascade
);

-- Bid Table
create table bid (
    id serial primary key,
    bidder_id varchar(255) not null,
    item_id int not null,
    bid_price int check (bid_price > 0),
    date_posted timestamp default current_timestamp,
    
    constraint fk_item_id foreign key(item_id) references item(id) on delete cascade,
    constraint fk_bidder_id foreign key(bidder_id) references users(user_id) on delete cascade
);

-- Billing Table
create table billing (
    id serial primary key,
    sold_item_id int not null unique,
    purchase_date timestamp default current_timestamp,
    seller_id varchar(255) not null,
    buyer_id varchar(255) not null,
    amount_due_buyers_need_to_pay int not null check (amount_due_buyers_need_to_pay >= 0),
    amount_of_money_sellers_need_to_get_paid int not null check (amount_of_money_sellers_need_to_get_paid >= 0),
    commission_fee int,
    
    constraint fk_sold_item_id foreign key(sold_item_id) references item(id) on delete cascade,
    constraint fk_seller_id foreign key(seller_id) references users(user_id) on delete cascade,
    constraint fk_buyer_id foreign key(buyer_id) references users(user_id) on delete cascade
);

-- Function to calculate commission_fee according to business rules
create or replace function calculate_commission_fee()
returns trigger as $$
begin
    -- Calculate 10% of the item price and round down
    new.commission_fee := floor(new.amount_due_buyers_need_to_pay * 0.1)::int;
    
    -- If the calculated commission is less than 1 KRW, set it to 0
    if new.commission_fee < 1 then
        new.commission_fee := 0;
    end if;

    -- Set amount for seller after commission deduction
    new.amount_of_money_sellers_need_to_get_paid := new.amount_due_buyers_need_to_pay - new.commission_fee;
    return new;
end;
$$ language plpgsql;

-- Trigger to automatically set commission_fee and update seller payment on insert or update
create trigger trg_calculate_commission_fee
before insert or update on billing
for each row
execute function calculate_commission_fee();
