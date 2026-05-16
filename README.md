# BrickedUp — LEGO Collector Android App

## Overview

BrickedUp is an Android application for LEGO enthusiasts to manage their collection, track want lists, and buy or sell sets. It integrates with the BrickLink marketplace API for live pricing and the Brickset API for set catalog data, with Firebase powering authentication and real-time persistence.

## Tech Stack

| Category | Stack |
|----------|-------|
| Mobile | Kotlin, Jetpack Compose |
| Backend | Firebase (Firestore, Auth) |
| APIs | BrickLink (OAuth1), Brickset (SOAP), Geoapify |
| Local DB | SQLite (`lego_sets.db`) |
| Build | Gradle (Kotlin DSL), Android SDK 36 |
| Utilities | Python scripts (data management) |

## Features

- **My Sets** — track owned LEGO sets with details from the Brickset catalog
- **Want List** — save sets to a wishlist and monitor market prices
- **Buy / Sell** — browse and post listings using BrickLink marketplace data
- **QR Code Scanning** — identify LEGO sets by scanning their QR codes
- **Location Services** — GPS-based discovery of nearby LEGO collectors via Geoapify
- **Price Tracking** — automated periodic price updates via BrickLink price guide API

## App Structure

```
app/src/main/java/com/cs407/brickcollector/
├── screens/
│   ├── MySetsScreen.kt
│   ├── BuyScreen.kt
│   ├── SellScreen.kt
│   ├── WantListScreen.kt
│   ├── LoginPage.kt
│   └── SettingsScreen.kt
└── viewModels/
    ├── LocationViewModel.kt   ← GPS + Geoapify integration
    ├── qrViewModel.kt         ← QR code scanning
    └── LatlngToCity.kt        ← Reverse geocoding
```

## Data Pipeline (Python)

Python utilities for offline data management:

- `database_lego.py` — fetches set data from Brickset SOAP API and populates SQLite
- `bricklink.py` — OAuth1 client for BrickLink (inventories, price guides, orders)
- `update_set_prices.py` — periodic price sync script
- `remove_empty_sets.py` — cleans up sets missing catalog data

## API Integrations

| API | Protocol | Purpose |
|-----|----------|---------|
| BrickLink | REST + OAuth1 | Market prices, buy/sell listings |
| Brickset | SOAP/XML | Set catalog, metadata |
| Geoapify | REST | Location lookup, reverse geocoding |
| Firebase | SDK | Auth, real-time database |

## Getting Started

1. Clone the repo and open in Android Studio
2. Add your API keys to `secrets.properties`:
   ```
   BRICKLINK_CONSUMER_KEY=...
   BRICKLINK_CONSUMER_SECRET=...
   BRICKSET_API_KEY=...
   GEOAPIFY_API_KEY=...
   ```
3. Connect `google-services.json` from your Firebase project
4. Run on an Android device or emulator (API 26+)

## Course

ECE/CS 407 — Mobile Application Development, UW–Madison
