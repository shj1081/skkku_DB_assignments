# Database Assignments

This repository contains implementations of assignments from the Database Systems course, organized into separate folders. Each folder includes its implementation files and documentation, adhering to course requirements and coding standards.

## Assignments Included

### PA1: Auction Database

- **Objective:** Design and implement a database system for an online auction platform.
- **Grade:** full score
- **Key Features:**
  - Developed a text-based menu-driven application for auction management.
  - Database supports functionalities such as user authentication, item listing, bidding, and account management.
  - Commission calculation: Sellers are charged a 10% commission (rounded down for amounts less than 1 KRW).
  - Dynamic auction management:
    - Handles auction closures and winning bid determination without a time-driven scheduler.
    - Bid closing time is checked during specific interactions like searching items, placing bids, or viewing account status.
  - Comprehensive schema design and SQL query implementation for managing users, items, bids, and billing data.

### PA2: Simple DBMS modification

- **Objective:** Modify and enhance the SimpleDB system to understand large-scale codebases and implement improvements.
- **Grade:** full score
- **Key Features:**
  - **Task 1: Improving Buffer Manager**
    - Implemented LRU (Least Recently Used) buffer replacement strategy.
    - Used a map for efficient buffer lookup, replacing sequential scans.
    - Modified the buffer pool to display detailed status, including pinned/unpinned buffers.
  - **Task 2: Wait-Die Scheme**
    - Replaced the default deadlock detection mechanism with the wait-die strategy.
    - Enhanced the lock management system to handle multiple transaction types and prioritize based on transaction age.
    - Implemented and tested transaction scenarios to ensure proper handling of locks and deadlock prevention.
