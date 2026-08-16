# BudgetMate

BudgetMate is a streamlined personal finance management application designed to help users transition from cumbersome spreadsheets to a sleek, intuitive mobile experience. Whether you're a college student managing a tight budget or a busy housewife tracking household expenses, BudgetMate provides the tools to simplify your bookkeeping.

---

## Project Origin
This application was developed as a school project for the **Mobilno računarstvo** (Mobile Computing) course at my university. It aims to demonstrate modern Android development practices, cloud integration, and clean code architecture.

## Key Features
*   **Real-time Cloud Sync:** Built on **Firebase Firestore**, your financial data is securely stored and accessible across multiple devices instantly.
*   **Comprehensive Reporting:** Gain insights into your spending habits with five distinct report types:
    *   Daily, Monthly, Quarterly, and Yearly snapshots.
    *   Custom Date Range reports for specific tracking needs.
*   **Smart Categorization:** Organize transactions into custom categories with an intelligent auto-complete interface.
*   **Multilingual Support:** Localized for a global audience with support for 4 languages:
    *   **Serbian**.
    *   **English**, 
    * **German**, and 
    * **Spanish**.
*   **Multi-Currency Ready:** Support for local and international currencies including **BAM, RSD, EUR, and USD**.
*   **Infinite Scrolling:** Efficiently browse through your history with a paginated transaction list that loads data only as you need it.

## Technical Stack
### Architecture
The project follows an **N-Tier (Layered) Architecture** with a strong **Service-Oriented** approach:
*   **Presentation Layer:** Utilizes the **Template Method Pattern** via a base `TemplateActivity` to ensure consistent UI initialization and lifecycle management across all screens.
*   **Business Logic Layer:** Encapsulated in specialized Services (`DatabaseService`, `AuthService`, `LocalisationService`) to decouple UI from API implementations.
*   **State Management:** Implements a centralized **Cache Layer** (`CacheService`) to optimize performance and facilitate seamless data sharing between activities without heavy Intent extras.

### Technologies
*   **Language:** Java
*   **Database:** Google Firebase Firestore
*   **Authentication:** Firebase Auth
*   **UI Components:** Material Design 3, RecyclerView (with custom ViewHolders), ConstraintLayout.
*   **Minimum SDK:** Android 11.0 (API level 30)

## Code Quality & Refactoring
This project has been refactored with a focus on high-quality standards (monitored via **SonarQube**). Contributors and developers are expected to follow:
*   **DRY (Don't Repeat Yourself):** Logic is centralized in services and base classes.
*   **KISS (Keep It Simple, Stupid):** Avoiding over-engineering while maintaining robustness.
*   **Boy Scout Rule:** Always leave the code cleaner than you found it.
*   **Centralized Error Handling:** Standardized exception wrapping for all database and auth operations.

## Setup & Installation
### Prerequisites
*   Android Studio Hedgehog or newer.
*   A Firebase Project.

### Configuration
1.  **Clone the repository.**
2.  **Add Firebase:**
    *   Create a project in the [Firebase Console](https://console.firebase.google.com/).
    *   Download `google-services.json` and place it in the `app/` directory.
3.  **Firestore Rules:** Ensure your security rules allow authenticated users to manage their own data.
4.  **Sync Project with Gradle Files.**

## Future Roadmap
While there are no active plans for immediate updates, BudgetMate is a living project. Planned future explorations include:
*   Interactive Data Visualization (Charts/Graphs).
*   Biometric Authentication.
*   Exporting reports to PDF/CSV.

## License
This project is licensed under the **MIT License** - see the LICENSE file for details.

---
*Developed with ❤️.*
