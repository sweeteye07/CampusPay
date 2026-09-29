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

Scan an event QR code to verify attendance.

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
Scans Event QR
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

Firebase Authentication

Automatic session detection

Logout

Student Profile

Each student has a Firestore profile containing information such as:

Name

Email

Role

Campus Credits

Campus Events

Students can:

View available events

See event descriptions

See event dates

See the number of credits offered

Register for an event

Prevent duplicate registration

View registered events

QR Attendance

Each event can generate an event-specific QR code.

The QR contains an identifier in the following format:

CAMPUSPAY_EVENT:<eventId>

When a student scans the QR:

CampusPay validates the QR format.

It checks whether the student registered for the event.

It checks whether the reward was already claimed.

It reads the event's configured credit value.

Credits are added to the student's wallet.

An attendance record is stored.

Credit Rewards

Example:

Java Workshop
Reward: 100 Credits

After successful attendance verification:

Before: 0 Credits
After:  100 Credits

The attendance record prevents the same student from claiming the same event reward repeatedly.

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

Show Event QR
   ↓
Student scans QR
   ↓
Validate QR
   ↓
Check Registration
   ↓
Check Existing Attendance
   ↓
Read Event Reward
   ↓
Increment Student Credits
   ↓
Create Attendance Record

Current Firestore Rules

The current rules provide authenticated access and restrict student-specific records to the logged-in student.

rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{userId} {
      allow read, write: if request.auth != null
        && request.auth.uid == userId;
    }

    match /events/{eventId} {
      allow read: if request.auth != null;
    }

    match /registrations/{registrationId} {
      allow create: if request.auth != null
        && request.resource.data.userId == request.auth.uid;

      allow read: if request.auth != null
        && resource.data.userId == request.auth.uid;
    }

    match /attendance/{attendanceId} {
      allow create: if request.auth != null
        && request.resource.data.userId == request.auth.uid;

      allow read: if request.auth != null
        && resource.data.userId == request.auth.uid;
    }
  }
}

Security note: The current reward flow is suitable for the college-project prototype, but it is not production-grade. The client currently participates in updating the user's credit balance. A production implementation should move reward issuance to a trusted backend/server-side process and make the credit update atomic.

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
│   │       │   └── EventQRActivity.java
│   │       │
│   │       ├── res/
│   │       │   └── layout/
│   │       │       ├── activity_login.xml
│   │       │       ├── activity_register.xml
│   │       │       ├── activity_dashboard.xml
│   │       │       ├── activity_events.xml
│   │       │       ├── activity_my_events.xml
│   │       │       └── activity_event_qr.xml
│   │       │
│   │       └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts
│   └── google-services.json
│
├── build.gradle.kts
├── settings.gradle.kts
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

Building the Project

From the project root:

.\gradlew.bat assembleDebug

The generated APK will be located under:

app/build/outputs/apk/debug/

Installing on an Emulator

If ADB is configured:

adb install -r appuild\outputspk\debugpp-debug.apk

Launch the application:

adb shell am start -n com.example.campuspay/.MainActivity

Testing the Main Flow

Use the following test sequence:

Create a student account.

Log in.

Open Campus Events.

Register for an event.

Open Show Event QR.

Scan the event QR from the student dashboard.

Confirm attendance.

Check the dashboard balance.

Confirm that the event reward has been added.

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

Credit transfer between students

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