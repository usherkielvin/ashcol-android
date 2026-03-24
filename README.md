<p align="center">
  <img src="app/src/main/res/drawable/ash.JPG" alt="Ashcol Logo" width="200"/>
</p>

<h1 align="center">Ashcol ServiceHub</h1>
<p align="center">A mobile service management app for Ashcol Airconditioning Corporation</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Java-ED8B00?style=flat&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Firebase-Firestore-FFCA28?style=flat&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Firebase-Auth-FFCA28?style=flat&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Firebase-FCM-FFCA28?style=flat&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Firebase-Storage-FFCA28?style=flat&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue?style=flat"/>
</p>

---

## Project Overview

Ashcol ServiceHub is an Android application built for Ashcol Airconditioning Corporation to manage service requests, technician assignments, and branch operations. The app supports four user roles — Customer, Technician, Manager, and Admin — each with a dedicated dashboard and feature set.

The backend is powered entirely by Firebase (Firestore, Authentication, Cloud Functions, and Storage), enabling real-time data sync, push notifications, and secure file handling without a traditional server.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Android (Java) |
| UI | Material Design Components, XML Layouts |
| Authentication | Firebase Authentication, Google Sign-In |
| Database | Firebase Firestore (NoSQL, real-time) |
| File Storage | Firebase Storage |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Backend Logic | Firebase Cloud Functions |
| Maps | Google Maps SDK, Play Services Location |
| Local Storage | SharedPreferences |
| Build System | Gradle (Kotlin DSL) |

---

## Features

### Customer
- Register via Email or Google
- Create service requests with photo attachments
- Track ticket status in real-time
- View service history and receive push notifications

### Technician
- View and manage assigned tickets
- Update ticket status and add comments
- Filter tickets by status

### Manager
- View all branch tickets and assign to technicians
- Monitor employee workload
- Real-time dashboard and branch reports

### Admin
- Full user and branch management (CRUD)
- System-wide analytics
- Manage all tickets across branches

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Android Application                    │
│  Activities / Fragments / Adapters / ViewModels          │
│                                                          │
│  Repositories (UserRepository, TicketRepository, ...)    │
└──────────────────────────┬──────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                      Firebase                            │
│                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │
│  │ Firestore   │  │    Auth     │  │ Cloud Functions │ │
│  └─────────────┘  └─────────────┘  └─────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐                       │
│  │   Storage   │  │     FCM     │                       │
│  └─────────────┘  └─────────────┘                       │
└─────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
app/src/main/java/app/hub/
├── admin/          # Admin dashboard, branches, managers, employees
├── api/            # Legacy data models (Pure POJO)
├── common/         # Shared activities, Firebase managers, auth
├── customer/       # Customer dashboard, tickets, payments
├── employee/       # Technician/employee views
├── manager/        # Manager dashboard and branch operations
├── models/         # Data models (User, Ticket, Branch, Payment)
├── onboarding/     # Intro and onboarding screens
└── repositories/   # Firebase data access layer
```

---

## Setup

1. Clone the repo and open in Android Studio
2. Add your `google-services.json` to the `app/` directory (from Firebase Console)
3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

For Google Sign-In, generate your SHA-1 and register it in the Firebase Console:
```bash
./gradlew signingReport
```

---

## Team

| Role | Name |
|---|---|
| Project Lead & Backend | Usher Kielvin Ponce |
| Android Frontend | Kenji A. Hizon |
| UI/UX Designer | Hans Gabrielee Borillo |
| Backend & Database | Dizon S. Dizon |

**Institution:** NU MOA — Application Development Final Project

---

<p align="center">Built with ❤️ by the NU MOA APPDEV Team</p>
