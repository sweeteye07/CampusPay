CampusPay

CampusPay is an Android-based closed-loop campus wallet and student rewards application.

Instead of functioning like a traditional digital payment app, CampusPay connects student participation in campus activities with digital Campus Credits.

Participate. Earn. Engage.

Overview

Students can:

Create an account and log in securely.

View their Campus Credits balance.

Browse available campus events.

Register for events.

View their registered events.

Show a personal QR code so an organizer can verify attendance.

Earn Campus Credits after verified attendance.

View their current credit balance.

The application is designed as a college project and does not use real money, bank accounts, UPI, or real payment gateways.

Core Concept

CampusPay creates a small campus economy:

Campus Event
     ↓
Student Registers
     ↓
Student Attends Event
     ↓
Organizer Scans Student QR
     ↓
Attendance Verified
     ↓
Campus Credits Awarded
     ↓
Student Uses Credits in the Campus Ecosystem

The wallet is therefore used as a mechanism for student engagement and rewards, rather than as a replacement for services such as PhonePe or Google Pay.

Main Features

Authentication

Student registration

Email/password login

Forgot password by email (Firebase sends the reset link)

Firebase Authentication

Automatic session detection

Logout

Student Profile

Each student has a Firestore profile containing information such as:

Name

Email

Role

Campus Credits

The profile screen opens from the avatar button on the dashboard and shows:

Avatar initials, display name, email and role badge

Current credit balance

Activity stats: events registered, events attended and total credits earned

The display name can be edited from the profile screen, and the Firestore rules allow a student to change only their own name field.

When the signed-in user has the organizer role (role = admin), the profile also shows a Scan student QR button that opens the attendance scanner.

Campus Events

Students can:

View available events

See event descriptions

See event dates

See the number of credits offered

Register for an event

Prevent duplicate registration

View registered events

Organizers (role = admin) additionally get a Create Event button on the dashboard, so events can be published from the app instead of the Firebase console.

QR Attendance

Each student has a personal QR code shown on the My QR screen.

The QR contains an identifier in the following format:

CAMPUSPAY_STUDENT:<userId>

Attendance is verified by an organizer, meaning a user whose role field is set to admin (promoted by hand in the Firebase console).

When an organizer scans a student QR:

The organizer selects the event to reward.

CampusPay validates the QR format.

It checks whether the student registered for the event.

It checks whether the reward was already claimed.

It reads the event's configured credit value.

The organizer confirms the student's name before awarding.

Credits are added to the student's wallet.

An attendance record and a ledger entry are stored in one Firestore transaction.

Credit Rewards

Example:

Java Workshop
Reward: 100 Credits

After successful attendance verification:

Before: 0 Credits
After:  100 Credits

The attendance record prevents the same student from claiming the same event reward repeatedly.

Credit Transfers

Students can send credits to another student using their email address.

Transfers run inside a Firestore transaction with a balance check.

The sender and the receiver each receive a ledger entry in their wallet history.

Technology Stack

Technology

Purpose

Java

Android application logic

XML

Android UI layouts

Android Studio

Android SDK, emulator and project tooling

VS Code

Primary code editor

Firebase Authentication

User authentication

Firebase Firestore

Application database

ZXing Android Embedded

QR scanning

Git

Version control

GitHub

Remote repository

Architecture

The current architecture is:

Android Application
       │
       ├── Java Activities
       │
       ├── XML Layouts
       │
       └── Firebase SDK
              │
              ├── Firebase Authentication
              │
              └── Cloud Firestore

Firebase Collections

Current collections include:

users
events
registrations
attendance
transactions

users

Stores student profile information.

Example:

users/{userId}

name: "Student Name"
email: "student@example.com"
role: "student"
credits: 100

events

Stores campus event information.

Example:

events/{eventId}

title: "Java Workshop"
description: "Learn Java fundamentals"
credits: 100
date: "2026-10-05"

registrations

Stores student event registrations.

Document IDs use:

{userId}_{eventId}

Example fields:

userId
eventId
eventTitle
registeredAt

Using a predictable document ID also helps prevent duplicate registrations.

attendance

Stores verified event attendance and rewards.

Document IDs use:

{userId}_{eventId}

Example fields:

userId
eventId
eventTitle
creditsAwarded
attendedAt

This structure also allows the application to determine whether a student has already claimed the reward for an event.

transactions

Stores the credit ledger shown in the wallet history.

Example fields:

type: "earn" or "transfer"
userId
credits
description
createdAt

Transfer entries also store direction (in or out) and counterpartyEmail, and earning entries store eventId and eventTitle.

Current Application Flow

Login

App Launch
   ↓
Check Firebase Session
   ↓
Logged In? ── Yes → Dashboard
   │
   No
   ↓
Login Screen
   ↓
Firebase Authentication
   ↓
Dashboard

Registration

Register
   ↓
Enter Name / Email / Password
   ↓
Firebase Authentication
   ↓
Create User Profile
   ↓
Firestore users/{userId}

Event Registration

Campus Events
   ↓
Select Event
   ↓
Register
   ↓
registrations/{userId}_{eventId}

Event Attendance

Organizer taps Scan on the dashboard
   ↓
Verify organizer role
   ↓
Select event
   ↓
Scan student QR
   ↓
Validate QR and look up student
   ↓
Check Registration
   ↓
Check Existing Attendance
   ↓
Confirm student name
   ↓
Atomic transaction: increment credits,
create attendance record, write ledger entry

Current Firestore Rules

The full rules text is version-controlled in firestore.rules at the project root. The Firebase console and this file must always hold the same text; deploy with:

firebase deploy --only firestore:rules

The rules enforce, in short:

Everything requires a signed-in user. Profiles are readable by any signed-in user so Send Credits can look a recipient up by email (that query must keep .limit(1)).

Registration may only create your own profile with exactly name, email, role = student and credits = 0. Email and role are locked after creation; a student may change only their own display name, and nothing else.

A balance can only change through three paths: your own debit when sending credits, an organizer award backed by a brand-new attendance record with a matching credit value in the same commit, and an incoming transfer matched by the sender's debit in the same commit.

Attendance records are created only by an organizer, only for a registered student, only once per event, and only for events that organizer created (console-created events without an owner can be rewarded by any organizer).

Ledger entries must be consistent with the balance changes in the same transaction: earn entries must match a new attendance record, transfer entries must match the exact balance deltas.

Events can only be created by organizers, with a reward between 1 and 10000 credits; updates and deletes are organizer-only.

Security note: The rules are enforced server-side with transactional consistency checks (getAfter), so forged balances, self-awards and mismatched ledger entries are rejected even if the client is tampered with. The app remains a college-project prototype: the reward flow still runs from the client, and a production implementation should move reward issuance to a Cloud Function.

Project Structure

CampusPay/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/campuspay/
│   │       │   ├── MainActivity.java
│   │       │   ├── RegisterActivity.java
│   │       │   ├── DashboardActivity.java
│   │       │   ├── EventsActivity.java
│   │       │   ├── MyEventsActivity.java
│   │       │   ├── ScanQRActivity.java
│   │       │   ├── MyQRActivity.java
│   │       │   ├── ProfileActivity.java
│   │       │   ├── CreateEventActivity.java
│   │       │   ├── SendMoneyActivity.java
│   │       │   ├── ReceiveMoneyActivity.java
│   │       │   ├── HistoryActivity.java
│   │       │   ├── Event.java
│   │       │   ├── EventAdapter.java
│   │       │   ├── MyEvent.java
│   │       │   ├── MyEventAdapter.java
│   │       │   ├── CreditTransaction.java
│   │       │   ├── HistoryAdapter.java
│   │       │   ├── UserRole.java
│   │       │   └── QrPayload.java
│   │       │
│   │       ├── res/
│   │       │   └── layout/
│   │       │       ├── activity_login.xml
│   │       │       ├── activity_register.xml
│   │       │       ├── activity_dashboard.xml
│   │       │       ├── activity_events.xml
│   │       │       ├── activity_my_events.xml
│   │       │       ├── activity_my_qr.xml
│   │       │       ├── activity_profile.xml
│   │       │       ├── activity_create_event.xml
│   │       │       ├── activity_send_money.xml
│   │       │       ├── activity_receive_money.xml
│   │       │       ├── activity_history.xml
│   │       │       ├── item_event.xml
│   │       │       ├── item_my_event.xml
│   │       │       └── item_history.xml
│   │       │
│   │       └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts
│   └── google-services.json
│
├── build.gradle.kts
├── settings.gradle.kts
├── firestore.rules
├── firestore.indexes.json
├── firebase.json
├── gradlew
├── gradlew.bat
└── README.md

Setup

Requirements

Android Studio

Java/JDK

Android SDK

Firebase account

Git

GitHub account

Firebase Setup

The Android application is connected to Firebase using the Firebase Android configuration file:

app/google-services.json

Firebase services currently used:

Authentication

Cloud Firestore

Email/password authentication must be enabled in Firebase Authentication.

Firestore rules and composite indexes are version-controlled at the project root (firestore.rules, firestore.indexes.json, firebase.json). To push them to Firebase:

firebase login
firebase deploy --only firestore

Building the Project

From the project root:

.\gradlew.bat assembleDebug

The generated APK will be located under:

app/build/outputs/apk/debug/

Running the Unit Tests

The role check (UserRole) and the QR payload parsing (QrPayload) are plain Java classes covered by JVM unit tests:

.\gradlew.bat testDebugUnitTest

Installing on an Emulator

If ADB is configured:

adb install -r app\build\outputs\apk\debug\app-debug.apk

Launch the application:

adb shell am start -n com.example.campuspay/.MainActivity

Testing the Main Flow

Use the following test sequence:

Create a student account.

Log in.

Open Campus Events.

Register for an event.

In the Firebase console, set a second account's role field to admin.

Log in as the organizer account.

Create an event with a credit value from the dashboard.

Tap Scan on the dashboard and select the event.

Open My QR on the student account and scan it with the organizer account.

Confirm the student's name and tap Award.

Check the dashboard balance and wallet history on the student account.

Scan the same QR again and verify that the reward cannot be claimed twice.

Git Workflow

The project is maintained using Git and GitHub.

Repository:

https://github.com/sweeteye07/CampusPay.git

Main branch:

main

Recommended workflow:

Create feature
    ↓
Implement
    ↓
Test
    ↓
Commit
    ↓
Push to GitHub

Example:

git status
git add <files>
git commit -m "feat: describe the change"
git push origin main

Example commit messages:

feat: add Firebase authentication
feat: add student dashboard
feat: add event registration
feat: add QR attendance
fix: prevent duplicate event registration
ui: improve dashboard layout
docs: update README

Future Scope

Possible future features include:

Campus marketplace

Credit redemption

Vendor accounts

QR-based credit payments

Student reward catalogue

Achievement/badge system

Event participation history

Admin dashboard

Event creation and management

Notifications

Spending/earning analytics

More secure server-side reward processing

Project Scope

CampusPay is intentionally kept within a manageable college-project scope.

Included

Student authentication

Campus events

Event registration

QR attendance

Digital Campus Credits

Firebase backend

Firestore data storage

Not Included

Real money

Bank accounts

UPI

KYC

Real payment gateways

Banking transactions

Project Goal

The goal of CampusPay is to demonstrate how an Android application, Firebase backend, authentication, database operations, event management, QR technology, and a digital reward system can be combined into a practical campus-focused application.

CampusPay — Participate. Earn. Engage.