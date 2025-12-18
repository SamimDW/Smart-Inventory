# Smart Inventory Management App

## Overview

Smart Inventory is an Android application designed to help users track, manage, and monitor inventory items efficiently. The app supports secure authentication, cloud-based data persistence, real-time inventory updates, and search and filtering functionality. It was developed as a multi-phase academic project and enhanced to reflect real-world mobile development practices.

The project emphasizes maintainable architecture, secure data access, and practical system design.

---

## Key Features

### User Authentication
- Secure login using Firebase Authentication
- User-scoped access to inventory data
- Prevents unauthorized access to stored records

### Inventory Management (CRUD)
- Create, read, update, and delete inventory items
- Persistent storage using Firebase Firestore
- Data remains consistent across sessions and devices

### Search and Filtering
- Real-time search across inventory items
- Filtering logic designed to keep the UI responsive as data grows

### Low-Stock Alerts
- Identifies inventory items below a defined threshold
- Designed to support notification-based alerts in later iterations

### Clean UI Design
- Grid-based inventory display
- Focus on readability and efficient user interaction

---

## Technical Stack

- **Platform:** Android  
- **Language:** Java  
- **Architecture:** Separation of UI, logic, and data layers  
- **Database:** Firebase Firestore  
- **Authentication:** Firebase Authentication  
- **Tooling:** Android Studio, GitHub  

---

## Design and Architecture

The application separates responsibilities between the user interface, application logic, and data layer. UI components handle user interaction and rendering, while business logic manages inventory operations, validation, and filtering. Firestore serves as the persistent data store, with all inventory records scoped to authenticated users.

This structure improves maintainability, supports testing, and reduces tight coupling between components. Design decisions prioritized clarity and long-term extensibility over short-term convenience.

---

## Data Management

Inventory data is stored in Firebase Firestore and organized by user scope to prevent cross-user data exposure. Each authenticated user can access only their own inventory records. Firestore’s real-time capabilities support responsive UI updates without manual refresh logic.

Using a cloud-based database removed the limitations of local-only storage and enabled persistence across devices while maintaining controlled access.

---

## Authentication and Security

Authentication is enforced using Firebase Authentication before any inventory data is accessed. User identity is tied directly to Firestore queries to ensure proper data isolation.

Security considerations include:
- Mandatory authentication before data access
- User-scoped Firestore queries
- Minimal trust in client-side state
- Architecture that supports stronger access rules as the app scales

Security was treated as a core design requirement rather than an afterthought.

---

## Search and Filtering Logic

Search and filtering functionality operates on Firestore-backed inventory data. Logic was implemented as part of the data flow rather than UI-only filtering, allowing the app to remain responsive as inventory size increases and simplifying future optimization.

---

## Testing and Validation

The application was developed with testability in mind. Clear separation of concerns supports unit testing and refactoring. Secure coding coursework influenced how edge cases and failure scenarios were handled, including validation logic and defensive design practices.

---

## Project Context

This project was developed and enhanced as part of the Computer Science program at Southern New Hampshire University. It represents cumulative learning across:
- Mobile application development
- Software architecture and design
- Algorithms and data handling
- Cloud database integration
- Secure authentication and access control

---

## Portfolio and Related Work

This project is part of a professional ePortfolio demonstrating applied computer science skills across multiple domains.

- **ePortfolio:**  
  [[Samim-ePortfolio]](https://samimdw.github.io/Samim-ePortfolio/Artifact:%20Smart%20Inventory%20Application.html)

- **Related Projects:**
  - Animal Shelter Dashboard with MongoDB CRUD operations
  - Machine learning model training using TensorFlow in Jupyter
  - Secure hashing implementation to protect web game sessions
  - Secure coding project using Google Test with positive and negative unit tests

---

## Future Enhancements

Planned improvements focus on scalability and real-world use:
- Multi-user support with shared or segmented inventories
- Expanded Firestore data models for multi-collection or role-based access
- Push notifications for low-stock items
- Enhanced reporting and analytics

---

## Author

**Samim Fnu**  
Computer Science Student, Southern New Hampshire University  
Aspiring Software Engineer
