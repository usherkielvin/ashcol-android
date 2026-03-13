# Ashcol ServiceHub - Complete Application Documentation

## Table of Contents
1. [Overview](#overview)
2. [User Roles](#user-roles)
3. [Customer Features](#customer-features)
4. [Technician/Employee Features](#technicianemployee-features)
5. [Manager Features](#manager-features)
6. [Admin Features](#admin-features)
7. [Technical Architecture](#technical-architecture)
8. [API Reference](#api-reference)

---

## Overview

Ashcol ServiceHub is a comprehensive service management mobile application built for Android that connects customers with service technicians through an efficient branch-based routing system. The application supports four distinct user roles with specialized dashboards and workflows.

### Key Technologies
- **Frontend**: Android (Java), Material Design Components
- **Backend**: Laravel 11 REST API
- **Database**: MySQL + Firebase Firestore (real-time sync)
- **Authentication**: Sanctum Token + Google OAuth 2.0
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Maps**: Google Maps API

---

## User Roles

The application supports four distinct user roles, each with specialized interfaces and capabilities:

### 1. Customer
Creates service requests, tracks tickets, and manages payments

### 2. Technician/Employee
Accepts assigned work, updates job status, and completes service requests

### 3. Manager
Manages branch operations, assigns tickets to technicians, and monitors team performance

### 4. Admin
System-wide management of branches, managers, employees, and analytics

---

## Customer Features

### Dashboard Overview
**Main Activity**: `DashboardActivity`

**Navigation Tabs**:
- 🏠 Home
- 🎫 My Tickets
- 🔔 Notifications
- 👤 Profile


### Customer UI Screens

#### 1. Home Screen (`UserHomeFragment`)
**Purpose**: Main dashboard with quick access to services

**UI Components**:
- Service banners with promotional content
- Quick service buttons (Cleaning, Repair, Installation, Maintenance)
- Ticket count overview (Pending, In Progress, Completed)
- Recent activity feed
- Floating chatbot button for assistance

**Actions**:
- Tap service button → Opens service selection
- View ticket counts → Navigate to My Tickets
- Access chatbot for help

#### 2. Service Selection Screen (`ServiceSelectActivity`)
**Purpose**: Create new service request

**Workflow Steps**:
1. **Service Type Selection**: Choose from AC Maintenance, Repair, Installation, etc.
2. **Unit Configuration**: Select unit types and quantities
3. **Date & Time**: Set preferred service date
4. **Location**: Pick location on map or enter address manually
5. **Image Upload**: Attach up to 3 photos of the issue
6. **Review & Confirm**: Verify all details before submission

**UI Components**:
- Service type dropdown
- Unit type selector with quantity controls
- Date picker
- Google Maps integration for location
- Image picker with preview
- Cost calculator (auto-calculated based on units)

**Validation**:
- All required fields must be filled
- Valid date (not in the past)
- Valid location coordinates
- Image size limits enforced

#### 3. Ticket Confirmation Screen (`TicketConfirmationActivity`)
**Purpose**: Final review before ticket submission

**Displays**:
- Service summary
- Total cost breakdown
- Selected date and location
- Uploaded images preview
- Terms and conditions

**Actions**:
- Confirm → Submit ticket to backend
- Edit → Return to service selection
- Cancel → Return to home

#### 4. My Tickets Screen (`UserTicketsFragment`)
**Purpose**: View and manage all service tickets

**UI Components**:
- Tab filters: All, Pending, In Progress, Completed, Cancelled
- Ticket cards with:
  - Ticket ID (e.g., TCK-001)
  - Service type and description
  - Status badge (color-coded)
  - Assigned technician info
  - Scheduled date/time
  - Action buttons

**Ticket Statuses**:
- 🟡 Pending: Awaiting assignment
- 🔵 Assigned: Technician assigned
- 🟠 In Progress: Work in progress
- 🟢 Completed: Work finished
- 🔴 Cancelled: Request cancelled

**Actions per Status**:
- Pending: View details, Cancel request
- Assigned: View technician info, Track location
- In Progress: View progress updates, Contact technician
- Completed: View summary, Rate service, Make payment

#### 5. Ticket Detail Screen (`TicketDetailActivity`)
**Purpose**: Detailed view of a specific ticket

**Information Displayed**:
- Ticket ID and status
- Service details (type, units, description)
- Customer information
- Assigned technician (name, phone, photo)
- Location with map view
- Scheduled date and time
- Uploaded images gallery
- Status history timeline
- Comments and updates

**Actions**:
- Call technician
- View location on map
- Cancel ticket (if pending)
- Make payment (if completed)
- Rate service (after completion)


#### 6. Payment Screen (`UserPaymentActivity`)
**Purpose**: Process payment for completed services

**UI Components**:
- Payment amount display
- Payment method selection:
  - Cash
  - Credit/Debit Card
  - GCash
  - PayMaya
- Payment confirmation button
- Payment history

**Workflow**:
1. View payment details
2. Select payment method
3. Confirm payment
4. Receive confirmation notification
5. View receipt

#### 7. Notifications Screen (`UserNotificationFragment`)
**Purpose**: View all app notifications

**Notification Types**:
- Ticket status updates
- Technician assignment
- Payment requests
- Service reminders
- Promotional messages

**UI Components**:
- Notification list with timestamps
- Unread indicators
- Swipe to dismiss
- Tap to view details

#### 8. Profile Screen (`UserProfileFragment`)
**Purpose**: Manage account settings and personal information

**Sections**:
- **Profile Photo**: Upload/change profile picture
- **Personal Information**: Name, email, phone, birthdate, gender
- **Address Management**: Region, city, location
- **Account Settings**:
  - Change password
  - Notification preferences
  - Language settings
- **About**: App version, terms, privacy policy
- **Logout**: Sign out of account

### Customer Process Flow

```
1. Login/Register
   ↓
2. View Dashboard (Home)
   ↓
3. Select Service Type
   ↓
4. Configure Service Details
   ↓
5. Choose Location & Date
   ↓
6. Upload Images (optional)
   ↓
7. Review & Confirm
   ↓
8. Submit Ticket
   ↓
9. Track Status in My Tickets
   ↓
10. Receive Notifications
   ↓
11. Make Payment (when completed)
   ↓
12. Rate Service
```

---

## Technician/Employee Features

### Dashboard Overview
**Main Activity**: `EmployeeDashboardActivity`

**Navigation Tabs**:
- 📊 Dashboard
- 🔧 Work
- 📅 Schedule
- 👤 Profile

### Employee UI Screens

#### 1. Dashboard Screen (`EmployeeDashboardFragment`)
**Purpose**: Overview of assigned work and daily tasks

**UI Components**:
- Today's schedule summary
- Active jobs count
- Completed jobs count
- Pending assignments
- Quick action buttons
- Performance metrics

**Displays**:
- Jobs for today
- Upcoming scheduled work
- Recent completions
- Earnings summary


#### 2. Work Screen (`EmployeeWorkFragment`)
**Purpose**: Manage assigned tickets and job execution

**Tab Filters**:
- All Jobs
- Assigned (new assignments)
- In Progress (active work)
- Completed (finished jobs)

**Ticket Card Components**:
- Ticket ID
- Customer name and contact
- Service type and description
- Location address
- Scheduled date/time
- Status indicator
- Action buttons

**Job Statuses & Actions**:

**Assigned Status**:
- Accept Assignment → Move to In Progress
- Reject Assignment → Return to manager
- View Details → See full ticket info
- View Location → Open map

**In Progress Status**:
- Arrived at Location → Update status
- Start Work → Begin timer
- Update Progress → Add notes/photos
- Complete Work → Finish job
- View Customer Info → Contact details

**Completed Status**:
- View Summary
- Request Payment
- View Payment Status

#### 3. Employee Ticket Detail Screen (`EmployeeTicketDetailActivity`)
**Purpose**: Detailed view with job execution controls

**Information Sections**:
- Customer Information (name, phone, address)
- Service Details (type, units, description)
- Location Map (with navigation button)
- Scheduled Date/Time
- Images from customer
- Work Notes (add updates)
- Status Timeline

**Action Buttons by Status**:

**Assigned**:
- ✅ Accept Job
- ❌ Reject Job
- 📍 Navigate to Location
- 📞 Call Customer

**Arrived**:
- ▶️ Start Work
- 📸 Add Photos
- 📝 Add Notes

**In Progress**:
- ✔️ Complete Work
- 📸 Add Progress Photos
- 📝 Update Notes
- ⏱️ View Time Elapsed

**Completed**:
- 💰 Request Payment
- 📄 View Summary
- 📊 View Payment Status

#### 4. Work Execution Workflow

**Multi-Step Process**:

```
Step 1: Assignment Received
  ↓ (Accept)
Step 2: Assigned
  ↓ (Arrive at Location)
Step 3: Arrived
  ↓ (Start Work)
Step 4: In Progress
  ↓ (Complete Work)
Step 5: Work Completed
  ↓ (Request Payment)
Step 6: Payment Requested
  ↓ (Manager Approves)
Step 7: Payment Confirmed
```

**At Each Step**:
- Update status in real-time
- Add notes and photos
- Track time spent
- Notify customer and manager


#### 5. Schedule Screen (`EmployeeScheduleFragment`)
**Purpose**: View daily and weekly work schedule

**UI Components**:
- Calendar view (month/week/day)
- Scheduled jobs list
- Time slots with job details
- Color-coded by status
- Swipe to navigate dates

**Schedule Information**:
- Date and time
- Customer name
- Service type
- Location
- Estimated duration
- Status

**Actions**:
- Tap job → View details
- Navigate to location
- Call customer
- Update status

#### 6. Employee Map Screen (`EmployeeMapActivity`)
**Purpose**: Navigate to customer location

**Features**:
- Current location tracking
- Customer location marker
- Route navigation
- Distance and ETA
- Traffic information
- Directions

**Actions**:
- Start Navigation
- Call Customer
- Mark as Arrived
- View Job Details

#### 7. Payment Management

**Payment Request Process**:
1. Complete work
2. Enter payment amount
3. Add payment notes
4. Submit to manager
5. Wait for approval
6. Confirm payment received

**Payment History**:
- View all payments
- Filter by status (Pending, Approved, Paid)
- Total earnings
- Payment details

#### 8. Profile Screen (`EmployeeProfileFragment`)
**Purpose**: Manage employee account and view performance

**Sections**:
- Profile Photo
- Personal Information
- Branch Assignment
- Performance Metrics:
  - Total jobs completed
  - Average rating
  - Total earnings
  - On-time completion rate
- Payment History
- Job History
- Settings
- Logout

### Employee Process Flow

```
1. Login
   ↓
2. View Dashboard
   ↓
3. Receive Assignment Notification
   ↓
4. Review Job Details
   ↓
5. Accept/Reject Assignment
   ↓ (Accept)
6. Navigate to Location
   ↓
7. Mark as Arrived
   ↓
8. Start Work
   ↓
9. Update Progress (add photos/notes)
   ↓
10. Complete Work
   ↓
11. Request Payment
   ↓
12. Confirm Payment Received
```

---

## Manager Features

### Dashboard Overview
**Main Activity**: `ManagerDashboardActivity`

**Navigation Tabs**:
- 🏠 Home
- 👥 Employees
- 🔧 Work
- 📋 Records
- 👤 Profile


### Manager UI Screens

#### 1. Home Screen (`ManagerHomeFragment`)
**Purpose**: Branch overview and key metrics

**Dashboard Cards**:
- **Pending Tickets**: Count of unassigned tickets
- **In Progress**: Active jobs count
- **Completed Today**: Jobs finished today
- **Total Employees**: Branch staff count
- **Revenue Today**: Daily earnings

**Recent Activity**:
- Latest ticket updates
- Recent assignments
- Completed jobs
- Payment requests

**Quick Actions**:
- Assign Tickets
- View All Tickets
- Manage Employees
- View Reports

**Real-Time Updates**:
- Firebase listeners for live data
- Auto-refresh on changes
- Push notifications for urgent items

#### 2. Employees Screen (`ManagerEmployeeFragment`)
**Purpose**: Manage branch technicians

**Employee List Display**:
- Profile photo
- Name
- Role
- Active tickets count
- Status (Available/Busy)
- Contact button

**Employee Card Actions**:
- View Details
- Assign Ticket
- View Job History
- Call Employee
- Edit Information

**Filters**:
- All Employees
- Available
- Busy
- Sort by workload

**Add Employee**:
- Button to add new technician
- Form with required fields
- Branch assignment

#### 3. Work Screen (`ManagerWorkFragment`)
**Purpose**: Ticket management and assignment

**Tab Filters**:
- All Tickets
- Pending (unassigned)
- Assigned
- In Progress
- Completed
- Cancelled

**Ticket List Display**:
- Ticket ID
- Customer name
- Service type
- Status badge
- Assigned technician (if any)
- Priority indicator
- Action buttons

**Ticket Actions by Status**:

**Pending**:
- Assign to Technician
- View Details
- Set Priority
- Cancel Ticket

**Assigned**:
- Reassign
- View Progress
- Contact Technician
- View Customer Info

**In Progress**:
- View Updates
- Monitor Progress
- Contact Technician
- View Location

**Completed**:
- Review Work
- Process Payment
- View Summary
- Archive


#### 4. Ticket Assignment Screen (`AssignEmployeeActivity`)
**Purpose**: Assign tickets to available technicians

**UI Components**:
- Ticket summary at top
- List of available employees
- Employee workload indicators
- Schedule date/time picker
- Assignment notes field
- Confirm button

**Employee Selection Criteria**:
- Current workload (ticket count)
- Availability status
- Skills/specialization
- Location proximity
- Performance rating

**Assignment Process**:
1. Select ticket from pending list
2. View ticket details
3. Choose technician from available list
4. Set schedule date and time
5. Add assignment notes
6. Confirm assignment
7. Notification sent to technician

#### 5. Manager Ticket Detail Screen (`ManagerTicketDetailActivity`)
**Purpose**: Comprehensive ticket management

**Information Sections**:
- Ticket Overview (ID, status, priority)
- Customer Information (name, contact, location)
- Service Details (type, units, description, images)
- Assignment Details (technician, schedule)
- Status Timeline (all updates)
- Payment Information (amount, status)
- Comments and Notes

**Management Actions**:
- Assign/Reassign Technician
- Update Schedule
- Change Priority
- Add Notes
- Process Payment
- Cancel Ticket
- Contact Customer
- Contact Technician
- View Location on Map

#### 6. Records Screen (`ManagerRecordsFragment`)
**Purpose**: View completed work and payment history

**Tabs**:
- Completed Jobs
- Payment History
- Reports

**Completed Jobs List**:
- Ticket ID
- Customer name
- Technician name
- Service type
- Completion date
- Payment status
- Rating (if available)

**Payment History**:
- Payment ID
- Ticket reference
- Technician name
- Amount
- Payment method
- Status (Pending, Approved, Paid)
- Date

**Actions**:
- View job details
- Process payment
- Generate invoice
- Export records

#### 7. Reports Screen (`ManagerReportsActivity`)
**Purpose**: Branch performance analytics

**Report Types**:
- Daily Summary
- Weekly Performance
- Monthly Overview
- Employee Performance
- Revenue Reports

**Metrics Displayed**:
- Total tickets processed
- Completion rate
- Average completion time
- Customer satisfaction
- Revenue generated
- Employee productivity
- Service type breakdown

**Visualizations**:
- Charts and graphs
- Trend analysis
- Comparison data
- Export to PDF/Excel


#### 8. Payment Management

**Payment Request Workflow**:
1. Technician completes work
2. Technician requests payment
3. Manager receives notification
4. Manager reviews work completion
5. Manager approves payment
6. Payment processed
7. Technician and customer notified

**Payment Actions**:
- Review payment request
- Approve/Reject payment
- Add payment notes
- Process payment
- View payment history
- Generate payment reports

#### 9. Profile Screen (`ManagerProfileFragment`)
**Purpose**: Manager account and branch information

**Sections**:
- Profile Photo
- Personal Information
- Branch Assignment
- Manager Metrics:
  - Total tickets managed
  - Team size
  - Branch performance
  - Revenue managed
- Settings
- About
- Logout

### Manager Process Flow

```
1. Login
   ↓
2. View Dashboard (Branch Overview)
   ↓
3. Monitor Pending Tickets
   ↓
4. Review Employee Availability
   ↓
5. Assign Ticket to Technician
   ↓
6. Set Schedule
   ↓
7. Monitor Job Progress
   ↓
8. Receive Completion Notification
   ↓
9. Review Work
   ↓
10. Approve Payment Request
   ↓
11. Process Payment
   ↓
12. Update Records
```

---

## Admin Features

### Dashboard Overview
**Main Activity**: `AdminDashboardActivity`

**Navigation Tabs**:
- 🏠 Home
- ⚙️ Operations
- 📊 Reports
- 👤 Profile

### Admin UI Screens

#### 1. Home Screen (`AdminHomeFragment`)
**Purpose**: System-wide overview

**Dashboard Cards**:
- Total Branches
- Total Managers
- Total Employees
- Total Customers
- Active Tickets
- System Revenue

**Quick Stats**:
- Branches by region
- Top performing branches
- Recent registrations
- System alerts

**Recent Activity**:
- New user registrations
- Branch updates
- Manager assignments
- System notifications

**Quick Actions**:
- Add Branch
- Add Manager
- View All Branches
- View Reports
- System Settings


#### 2. Operations Screen (`AdminOperationsFragment`)
**Purpose**: Manage branches and managers

**Tabs**:
- Branches
- Managers

**Branches Tab** (`AdminBranchesFragment`):

**Branch List Display**:
- Branch name
- Location (region, city)
- Manager assigned
- Employee count
- Active tickets
- Status (Active/Inactive)

**Branch Card Actions**:
- View Details
- Edit Branch
- View Tickets
- View Employees
- Deactivate/Activate

**Add Branch**:
- Branch name
- Location (region, city, address)
- Coordinates (latitude, longitude)
- Contact information
- Status

**Managers Tab** (`AdminEmployeesFragment`):

**Manager List Display**:
- Profile photo
- Name
- Email
- Branch assignment
- Status
- Action buttons

**Manager Actions**:
- View Details
- Edit Information
- Reassign Branch
- Deactivate Account
- Delete Manager

**Add Manager** (`AdminAddManager`):
- Personal information (name, email, phone)
- Branch assignment
- Initial password
- Contact details

#### 3. Branch Detail Screen (`BranchDetailActivity`)
**Purpose**: Comprehensive branch management

**Information Sections**:
- Branch Overview (name, location, status)
- Manager Information
- Employee List
- Ticket Statistics
- Performance Metrics
- Location Map

**Branch Metrics**:
- Total employees
- Active tickets
- Completed tickets
- Revenue generated
- Customer satisfaction
- Average completion time

**Actions**:
- Edit Branch Info
- View All Tickets
- Add Employee
- View Reports
- Deactivate Branch

#### 4. Branch Tickets Screen (`BranchTicketsAdapter`)
**Purpose**: View all tickets for a specific branch

**Filters**:
- All
- Pending
- Assigned
- In Progress
- Completed
- Cancelled

**Ticket Information**:
- Ticket ID
- Customer name
- Service type
- Status
- Assigned technician
- Date created

**Actions**:
- View ticket details
- Monitor progress
- View customer info
- Export data


#### 5. Manager Detail Screen (`ManagerDetailActivity`)
**Purpose**: View and edit manager information

**Information Displayed**:
- Profile photo
- Personal details (name, email, phone)
- Branch assignment
- Account status
- Date joined
- Performance metrics

**Manager Metrics**:
- Tickets managed
- Team size
- Branch performance
- Revenue managed

**Actions**:
- Edit Information
- Change Branch Assignment
- Reset Password
- Deactivate Account
- Delete Manager
- View Activity Log

#### 6. Reports Screen (`AdminReportsFragment`)
**Purpose**: System-wide analytics and reporting

**Report Categories**:
- Branch Performance
- Manager Performance
- Employee Performance
- Revenue Reports
- Customer Analytics
- Service Type Analysis

**Branch Reports** (`BranchReportsAdapter`):
- Branch name
- Total tickets
- Completion rate
- Revenue
- Employee count
- Customer satisfaction
- Average response time

**Report Actions**:
- View detailed report
- Compare branches
- Export to PDF/Excel
- Generate custom reports
- Schedule reports

**Visualizations**:
- Performance charts
- Revenue trends
- Comparison graphs
- Heat maps
- Pie charts for service types

#### 7. Branch Report Detail Screen (`BranchReportDetailActivity`)
**Purpose**: Detailed analytics for a specific branch

**Metrics Displayed**:
- Ticket volume (daily, weekly, monthly)
- Completion rates
- Revenue breakdown
- Employee productivity
- Customer satisfaction scores
- Service type distribution
- Peak hours analysis
- Response time metrics

**Time Filters**:
- Today
- This Week
- This Month
- Custom Date Range

**Export Options**:
- PDF Report
- Excel Spreadsheet
- CSV Data
- Email Report

#### 8. Employee Management (`AdminAddEmployee`)
**Purpose**: Add employees to branches

**Form Fields**:
- Personal Information (name, email, phone)
- Branch Assignment
- Role (Technician)
- Initial Password
- Contact Details
- Skills/Specialization

**Actions**:
- Add Employee
- Assign to Branch
- Set Initial Password
- Send Welcome Email


#### 9. User Management

**Admin User Actions**:
- View all users (customers, employees, managers)
- Search and filter users
- View user details
- Edit user information
- Deactivate accounts
- Delete users
- Reset passwords
- View user activity

**User Filters**:
- By Role (Customer, Employee, Manager)
- By Branch
- By Status (Active, Inactive)
- By Registration Date

#### 10. Profile Screen (`AdminProfileFragment`)
**Purpose**: Admin account management

**Sections**:
- Profile Photo
- Personal Information
- Admin Privileges
- System Settings
- Activity Log
- About
- Logout

### Admin Process Flow

```
1. Login
   ↓
2. View System Dashboard
   ↓
3. Monitor Branch Performance
   ↓
4. Add/Manage Branches
   ↓
5. Add/Manage Managers
   ↓
6. Assign Managers to Branches
   ↓
7. Monitor System Metrics
   ↓
8. Generate Reports
   ↓
9. Review Analytics
   ↓
10. Make System Adjustments
```

---

## Technical Architecture

### Application Structure

```
Ashcol ServiceHub
├── Authentication Layer
│   ├── Email/Password Login
│   ├── Google OAuth 2.0
│   ├── Token Management (Sanctum)
│   └── Role-Based Access Control
│
├── Customer Module
│   ├── Service Request Creation
│   ├── Ticket Tracking
│   ├── Payment Processing
│   └── Profile Management
│
├── Employee Module
│   ├── Job Assignment
│   ├── Work Execution
│   ├── Schedule Management
│   └── Payment Requests
│
├── Manager Module
│   ├── Ticket Assignment
│   ├── Employee Management
│   ├── Payment Approval
│   └── Branch Reports
│
├── Admin Module
│   ├── Branch Management
│   ├── Manager Management
│   ├── System Analytics
│   └── User Management
│
└── Shared Services
    ├── Firebase Firestore (Real-time sync)
    ├── Firebase Cloud Messaging (Notifications)
    ├── Google Maps Integration
    ├── Image Upload/Storage
    └── Location Services
```

### Technology Stack

**Frontend (Android)**:
- Language: Java
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- UI Framework: Material Design Components
- Architecture: MVVM (Model-View-ViewModel)

**Key Libraries**:
- Retrofit 2: REST API communication
- Gson: JSON serialization
- Glide: Image loading
- Google Play Services: Maps, Location, Auth
- Firebase: Firestore, Cloud Messaging
- Material Components: UI elements


**Backend (Laravel 11)**:
- Framework: Laravel 11
- Authentication: Sanctum (Token-based)
- Database: MySQL
- Real-time: Firebase Firestore
- Storage: Local + Cloud Storage
- API: RESTful architecture

**Database Schema**:
- Users (customers, employees, managers, admins)
- Tickets (service requests)
- Branches (service locations)
- Payments (transaction records)
- Ticket Statuses (workflow states)
- Comments (ticket updates)

### Data Flow

```
Mobile App → Retrofit → Laravel API → MySQL Database
                                    ↓
                              Firebase Firestore
                                    ↓
                         Real-time Updates → Mobile App
```

### Authentication Flow

```
1. User Login (Email/Password or Google)
   ↓
2. Backend Validates Credentials
   ↓
3. Generate Sanctum Token
   ↓
4. Return Token + User Data
   ↓
5. Store Token in SharedPreferences
   ↓
6. Register FCM Token for Notifications
   ↓
7. Navigate to Role-Based Dashboard
```

### Real-Time Synchronization

**Firebase Firestore Integration**:
- Tickets collection synced in real-time
- Status updates pushed instantly
- Manager dashboard auto-refreshes
- Employee receives instant assignments
- Customer sees live progress updates

**Firestore Listeners**:
- `EmployeeFirebaseListener`: Employee ticket updates
- `ManagerFirebaseListener`: Manager dashboard updates
- `CustomerFirebaseListener`: Customer ticket updates
- `FirestoreManager`: Central Firestore management

### Push Notifications

**FCM Integration**:
- Token registration on login
- Server-side notification triggers
- Custom notification handling
- Deep linking to specific screens

**Notification Types**:
- Ticket status changes
- Assignment notifications
- Payment requests
- Payment confirmations
- Schedule reminders
- System announcements

### Location Services

**Google Maps Integration**:
- Location picker for service requests
- Technician navigation
- Real-time location tracking
- Distance calculation
- Route optimization

**Location Features**:
- Current location detection
- Address geocoding
- Reverse geocoding
- Map markers and routes
- Location permissions handling

### Image Handling

**Image Upload**:
- Camera capture
- Gallery selection
- Multiple image support (up to 3)
- Image compression
- Preview before upload
- Server storage

**Image Display**:
- Lazy loading with Glide
- Image gallery view
- Zoom and pan support
- Placeholder images
- Error handling

---

## API Reference

### Base URL
```
https://your-backend-url.com/api/v1/
```

### Authentication Header
```
Authorization: Bearer {token}
```


### Authentication Endpoints

#### POST /login
**Purpose**: User login with email and password

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "token": "sanctum_token_here",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "role": "customer",
      "firstName": "John",
      "lastName": "Doe",
      "branch": "ASHCOL Valenzuela"
    }
  }
}
```

#### POST /register
**Purpose**: New user registration

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "user@example.com",
  "password": "password123",
  "phone": "09123456789",
  "region": "NCR",
  "city": "Valenzuela"
}
```

#### POST /google-signin
**Purpose**: Login with Google OAuth

**Request Body**:
```json
{
  "idToken": "google_id_token",
  "email": "user@gmail.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### POST /logout
**Purpose**: User logout

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

### User Management Endpoints

#### GET /user
**Purpose**: Get authenticated user profile

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "johndoe",
    "firstName": "John",
    "lastName": "Doe",
    "email": "user@example.com",
    "role": "customer",
    "phone": "09123456789",
    "branch": "ASHCOL Valenzuela",
    "profile_photo": "url_to_photo"
  }
}
```

#### POST /user/update
**Purpose**: Update user profile

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phone": "09123456789",
  "region": "NCR",
  "city": "Valenzuela"
}
```

#### POST /profile/photo
**Purpose**: Upload profile photo

**Headers**: `Authorization: Bearer {token}`

**Request**: Multipart form data with `photo` field

#### POST /change-password
**Purpose**: Change user password

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "current_password": "oldpassword",
  "new_password": "newpassword",
  "new_password_confirmation": "newpassword"
}
```


### Ticket Management Endpoints

#### POST /tickets
**Purpose**: Create new service ticket

**Headers**: `Authorization: Bearer {token}`

**Request**: Multipart form data
```
title: "AC Repair"
description: "AC not cooling properly"
address: "123 Main St, Valenzuela"
contact: "09123456789"
service_type: "Repair"
unit_type: "Split Type"
preferred_date: "2024-03-20"
latitude: "14.6991"
longitude: "120.9842"
amount: "1500.00"
image: [file]
```

**Response**:
```json
{
  "success": true,
  "data": {
    "ticket": {
      "id": 1,
      "ticket_id": "TCK-001",
      "title": "AC Repair",
      "status": {
        "name": "Pending",
        "color": "#FFA500"
      },
      "branch": {
        "name": "ASHCOL Valenzuela"
      }
    }
  }
}
```

#### GET /tickets
**Purpose**: Get user tickets (role-based)

**Headers**: `Authorization: Bearer {token}`

**Query Parameters**:
- `status` (optional): Filter by status

**Response**:
```json
{
  "success": true,
  "tickets": [
    {
      "id": 1,
      "ticket_id": "TCK-001",
      "title": "AC Repair",
      "service_type": "Repair",
      "status": "Pending",
      "status_color": "#FFA500",
      "customer_name": "John Doe",
      "assigned_staff": "Jane Smith",
      "branch": "ASHCOL Valenzuela",
      "created_at": "2024-03-15 10:30:00"
    }
  ]
}
```

#### GET /tickets/{ticketId}
**Purpose**: Get ticket details

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "ticket": {
    "id": 1,
    "ticket_id": "TCK-001",
    "title": "AC Repair",
    "description": "AC not cooling properly",
    "service_type": "Repair",
    "unit_type": "Split Type",
    "address": "123 Main St",
    "contact": "09123456789",
    "status": "In Progress",
    "customer": {
      "name": "John Doe",
      "phone": "09123456789"
    },
    "assigned_staff": {
      "name": "Jane Smith",
      "phone": "09187654321"
    },
    "scheduled_date": "2024-03-20",
    "scheduled_time": "10:00 AM",
    "latitude": 14.6991,
    "longitude": 120.9842,
    "image_path": "url_to_image",
    "amount": 1500.00
  }
}
```

#### PUT /tickets/{ticketId}/status
**Purpose**: Update ticket status

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "status": "In Progress",
  "notes": "Started working on the unit"
}
```


### Employee/Technician Endpoints

#### GET /technician/tickets
**Purpose**: Get assigned tickets for employee

**Headers**: `Authorization: Bearer {token}`

**Query Parameters**:
- `status` (optional): Filter by status

**Response**: Same as GET /tickets

#### POST /tickets/{ticketId}/accept
**Purpose**: Accept ticket assignment

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "message": "Ticket accepted successfully"
}
```

#### POST /tickets/{ticketId}/reject
**Purpose**: Reject ticket assignment

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "reason": "Not available at scheduled time"
}
```

#### PUT /tickets/{ticketId}/schedule
**Purpose**: Set ticket schedule

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "scheduled_date": "2024-03-20",
  "scheduled_time": "10:00 AM",
  "notes": "Customer prefers morning"
}
```

#### POST /tickets/{ticketId}/complete-work
**Purpose**: Mark work as completed and request payment

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "amount": 1500.00,
  "notes": "Work completed successfully",
  "payment_method": "Cash"
}
```

#### GET /technician/schedule
**Purpose**: Get employee schedule

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "schedule": [
    {
      "date": "2024-03-20",
      "tickets": [
        {
          "ticket_id": "TCK-001",
          "customer_name": "John Doe",
          "service_type": "Repair",
          "scheduled_time": "10:00 AM",
          "address": "123 Main St"
        }
      ]
    }
  ]
}
```

### Manager Endpoints

#### GET /manager/tickets
**Purpose**: Get all branch tickets

**Headers**: `Authorization: Bearer {token}`

**Response**: Same as GET /tickets

#### GET /manager/dashboard
**Purpose**: Get manager dashboard statistics

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "stats": {
    "pending_count": 5,
    "in_progress_count": 3,
    "completed_count": 12,
    "total_revenue": 25000.00,
    "employee_count": 8
  },
  "recent_tickets": [
    {
      "ticket_id": "TCK-001",
      "customer_name": "John Doe",
      "status": "In Progress",
      "created_at": "2024-03-15 10:30:00"
    }
  ]
}
```

#### GET /technicians/by-branch
**Purpose**: Get employees by branch

**Headers**: `Authorization: Bearer {token}`

**Query Parameters**:
- `branch`: Branch name

**Response**:
```json
{
  "success": true,
  "branch": "ASHCOL Valenzuela",
  "employee_count": 8,
  "employees": [
    {
      "id": 1,
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane@example.com",
      "role": "technician",
      "ticket_count": 3,
      "profile_photo": "url_to_photo"
    }
  ]
}
```


#### GET /manager/payments
**Purpose**: Get payment history for branch

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "payments": [
    {
      "id": 1,
      "ticket_id": "TCK-001",
      "technician_name": "Jane Smith",
      "amount": 1500.00,
      "payment_method": "Cash",
      "status": "Pending",
      "created_at": "2024-03-15 15:30:00"
    }
  ]
}
```

### Payment Endpoints

#### POST /payment-request
**Purpose**: Request payment from manager

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "ticket_id": "TCK-001",
  "amount": 1500.00,
  "payment_method": "Cash",
  "notes": "Work completed"
}
```

#### POST /payment-confirm
**Purpose**: Confirm payment received

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "payment_id": 1,
  "confirmation_notes": "Payment received"
}
```

#### GET /payments/by-ticket/{ticketId}
**Purpose**: Get payment details by ticket

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "payment": {
    "id": 1,
    "ticket_id": "TCK-001",
    "amount": 1500.00,
    "payment_method": "Cash",
    "status": "Paid",
    "technician": "Jane Smith",
    "created_at": "2024-03-15 15:30:00"
  }
}
```

### Admin Endpoints

#### GET /branches
**Purpose**: Get all branches

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "branches": [
    {
      "id": 1,
      "name": "ASHCOL Valenzuela",
      "location": "NCR, Valenzuela",
      "address": "123 Main St",
      "latitude": 14.6991,
      "longitude": 120.9842,
      "is_active": true,
      "manager": "John Manager",
      "employee_count": 8,
      "active_tickets": 5
    }
  ]
}
```

#### GET /branches/reports
**Purpose**: Get branch performance reports

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "reports": [
    {
      "branch_id": 1,
      "branch_name": "ASHCOL Valenzuela",
      "total_tickets": 50,
      "completed_tickets": 45,
      "completion_rate": 90,
      "total_revenue": 75000.00,
      "employee_count": 8,
      "average_rating": 4.5
    }
  ]
}
```

#### GET /branches/{branchId}/tickets
**Purpose**: Get tickets for specific branch

**Headers**: `Authorization: Bearer {token}`

**Query Parameters**:
- `status` (optional): Filter by status

**Response**: Same as GET /tickets

#### DELETE /admin/users/{userId}
**Purpose**: Delete user account (admin only)

**Headers**: `Authorization: Bearer {token}`

**Response**:
```json
{
  "success": true,
  "message": "User deleted successfully"
}
```


### Other Endpoints

#### POST /register-fcm-token
**Purpose**: Register FCM token for push notifications

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "fcm_token": "firebase_token_here"
}
```

#### POST /update-location
**Purpose**: Update user location

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "email": "user@example.com",
  "latitude": 14.6991,
  "longitude": 120.9842
}
```

#### POST /chatbot
**Purpose**: Send message to chatbot

**Headers**: `Authorization: Bearer {token}`

**Request Body**:
```json
{
  "message": "How do I track my ticket?"
}
```

**Response**:
```json
{
  "success": true,
  "response": "You can track your ticket in the My Tickets tab..."
}
```

#### GET /about
**Purpose**: Get app information

**Response**:
```json
{
  "success": true,
  "data": {
    "app_name": "Ashcol ServiceHub",
    "version": "1.0.0",
    "description": "Service management application",
    "contact": "support@ashcol.com"
  }
}
```

---

## Security & Permissions

### Required Permissions

**Android Manifest Permissions**:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Security Features

**Authentication**:
- Token-based authentication (Sanctum)
- Password hashing (bcrypt)
- Google OAuth 2.0 integration
- Token expiration and refresh
- Secure token storage (SharedPreferences)

**Authorization**:
- Role-based access control (RBAC)
- API endpoint protection
- Resource ownership validation
- Permission checks per action

**Data Protection**:
- HTTPS/TLS encryption
- SQL injection prevention (Eloquent ORM)
- XSS protection
- CSRF protection
- Input validation and sanitization
- File upload validation

**Privacy**:
- User data encryption
- Secure credential storage
- Privacy policy compliance
- Data deletion on account removal

---

## Error Handling

### Common Error Codes

**Authentication Errors**:
- `401 Unauthorized`: Invalid or expired token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: User account not found

**Validation Errors**:
- `422 Unprocessable Entity`: Invalid input data

**Server Errors**:
- `500 Internal Server Error`: Server-side error
- `503 Service Unavailable`: Server maintenance

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "errors": {
    "field_name": ["Error message"]
  }
}
```


---

## Installation & Setup

### Prerequisites

**Development Environment**:
- Android Studio (latest version)
- JDK 11 or higher
- Android SDK (API 24+)
- Git

**Backend Requirements**:
- Laravel 11 backend server
- MySQL database
- Firebase project setup

### Android App Setup

1. **Clone Repository**:
```bash
git clone <repository-url>
cd Ashcol_ServiceHub
```

2. **Open in Android Studio**:
- Launch Android Studio
- Select "Open an Existing Project"
- Navigate to project directory

3. **Configure API Base URL**:
- Edit `app/src/main/java/app/hub/api/ApiClient.java`
- Update `BASE_URL` to your backend URL

4. **Google Services Configuration**:
- Place `google-services.json` in `app/` directory
- Configure Google Sign-In in Firebase Console
- Add SHA-1 fingerprint

5. **Build Project**:
```bash
./gradlew build
```

6. **Run Application**:
- Connect Android device or start emulator
- Click Run in Android Studio

### Backend Setup

1. **Install Dependencies**:
```bash
composer install
npm install
```

2. **Environment Configuration**:
```bash
cp .env.example .env
php artisan key:generate
```

3. **Database Setup**:
```bash
php artisan migrate --seed
```

4. **Start Server**:
```bash
php artisan serve
```

---

## Testing

### Manual Testing Checklist

**Authentication**:
- [ ] Email/password login
- [ ] Google Sign-In
- [ ] Registration
- [ ] Password reset
- [ ] Logout

**Customer Flow**:
- [ ] Create service request
- [ ] Upload images
- [ ] View ticket list
- [ ] Track ticket status
- [ ] Make payment
- [ ] View notifications

**Employee Flow**:
- [ ] Accept/reject assignment
- [ ] Navigate to location
- [ ] Update job status
- [ ] Complete work
- [ ] Request payment
- [ ] View schedule

**Manager Flow**:
- [ ] View dashboard
- [ ] Assign tickets
- [ ] Monitor employees
- [ ] Approve payments
- [ ] Generate reports

**Admin Flow**:
- [ ] Manage branches
- [ ] Manage managers
- [ ] View analytics
- [ ] Delete users

### Automated Testing

**Unit Tests**:
```bash
./gradlew test
```

**Instrumented Tests**:
```bash
./gradlew connectedAndroidTest
```

---

## Troubleshooting

### Common Issues

**Google Sign-In Not Working**:
- Verify SHA-1 fingerprint in Firebase Console
- Check package name matches
- Ensure google-services.json is up to date
- Verify OAuth client ID configuration

**API Connection Failed**:
- Check BASE_URL in ApiClient.java
- Verify backend server is running
- Check network permissions
- Test API endpoints with Postman

**Push Notifications Not Received**:
- Verify FCM token registration
- Check notification permissions
- Ensure Firebase configuration is correct
- Test with Firebase Console

**Location Not Working**:
- Check location permissions granted
- Enable GPS on device
- Verify Google Maps API key
- Check network connectivity

**Images Not Uploading**:
- Check storage permissions
- Verify file size limits
- Check network connection
- Ensure backend accepts multipart data

---

## Performance Optimization

### Best Practices

**Data Loading**:
- Implement pagination for large lists
- Use RecyclerView for efficient scrolling
- Cache data locally with SharedPreferences
- Lazy load images with Glide

**Network Optimization**:
- Batch API requests when possible
- Implement retry mechanisms
- Use compression for images
- Cache API responses

**Real-Time Updates**:
- Use Firebase listeners efficiently
- Unregister listeners when not needed
- Implement debouncing for frequent updates
- Use background threads for processing

**Battery Optimization**:
- Stop location updates when not needed
- Use WorkManager for background tasks
- Implement doze mode compatibility
- Optimize notification frequency

---

## Future Enhancements

### Planned Features

**Phase 1**:
- [ ] Offline mode with local database sync
- [ ] In-app chat between customer and technician
- [ ] Advanced search and filters
- [ ] Multi-language support

**Phase 2**:
- [ ] Payment gateway integration
- [ ] Service rating and review system
- [ ] Advanced analytics dashboard
- [ ] Automated report generation

**Phase 3**:
- [ ] AI-powered chatbot
- [ ] Predictive maintenance scheduling
- [ ] Route optimization for technicians
- [ ] Customer loyalty program

---

## Support & Contact

### Development Team

**Project Lead**: Usher Kielvin Ponce
**UI/UX Designer**: Hans Gabrielee Borillo
**Android Developer**: Kenji A. Hizon
**Backend Developer**: Dizon S. Dizon

### Resources

- **Documentation**: This file
- **API Documentation**: Available at backend `/api/documentation`
- **Issue Tracker**: GitHub Issues
- **Support Email**: support@ashcol.com

---

## License

This project is developed as an academic project for NU MOA (National University - Mall of Asia) Application Development course.

**Built with ❤️ by the NU MOA APPDEV Team**

---

*Last Updated: March 2024*
*Version: 1.0.0*
