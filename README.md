# Fitto - AI-Supported Healthy Lifestyle, Nutrition & Social Tracking App

> 🇹🇷 Fitto, kullanıcıların kalori, makro besin, su, egzersiz, uyku, kilo ve vücut ölçüsü gibi sağlıklı yaşam verilerini takip edebildiği; Gemini API destekli yapay zekâ rehberliği, tarifler, diyet planları, kamera ile yemek tanıma, sosyal medya, mesajlaşma, hatırlatıcılar ve gelişim raporlarını tek bir mobil platformda birleştiren kapsamlı bir Android uygulamasıdır.

Fitto is an AI-supported Android healthy lifestyle platform that combines nutrition tracking, personalized goal guidance, recipe and diet plan management, camera-based food recognition, social sharing, messaging, reminders and progress analytics in a single mobile application.

---

## Project Overview

Fitto was designed as a comprehensive healthy lifestyle assistant rather than a simple calorie tracking application. The application helps users track daily nutrition and lifestyle habits while also offering personalized onboarding, AI-supported guidance, recipe management, diet planning, social interaction, messaging, reminders and progress visualization.

The project focuses on making healthy living more sustainable by allowing users to manage nutrition, hydration, physical activity, sleep, weight, body measurements and personal goals from one integrated mobile experience.

One of the key strengths of Fitto is its AI-supported guidance flow. Gemini API is used to support users during target weight selection and to provide meaningful feedback based on daily user inputs such as meals, water intake, exercise records and weight progress.

---

## Project Highlights

- Personalized onboarding and calorie goal calculation
- AI-supported target weight guidance with Gemini API
- AI-powered daily progress interpretation based on user inputs
- Meal, calorie and macro tracking
- Water, exercise, sleep, step and weight tracking
- Weight and body measurement tracking
- Recipe browsing, filtering and custom recipe creation
- Shopping list and fridge inventory support
- Diet plan management and custom diet plan creation
- Intermittent fasting tracking
- Camera-based food recognition flow
- Social media features such as posts, likes, comments and following
- Direct messaging and group messaging support
- Firebase-based authentication, social data and media storage
- Health Connect integration for step and active calorie data
- Progress reports and chart-based visualization
- Reminder and notification system
- Modern Android UI with Material Design components

---

## Main Modules

Fitto is structured around several major modules that together create a full healthy lifestyle platform.

### 1. Personalized Onboarding & AI Guidance

Fitto starts with a detailed onboarding flow to understand the user and generate personalized goals.

During onboarding, the application collects information such as:

- Age
- Height
- Weight
- Gender
- Activity level
- Goal type
- Target weight
- Goal motivation

Based on this information, Fitto calculates daily calorie and macro targets. The application also evaluates whether the selected target is healthy, aggressive or not recommended, and provides safer guidance when needed.

Gemini API is used to support this process by generating AI-assisted explanations and recommendations for the user’s target and plan.

---

### 2. Daily Nutrition & Lifestyle Tracking

The daily tracking module is the central part of the application. It allows users to monitor their daily health and nutrition data in one place.

Users can track:

- Meals
- Calories
- Protein, carbohydrate and fat values
- Water intake
- Exercise records
- Sleep duration
- Step count
- Active calories
- Weight updates
- Body measurements

Fitto also provides AI-supported daily interpretation based on the user’s current data. This helps the application act not only as a tracker, but also as a smart assistant that gives contextual feedback.

---

### 3. Camera-Based Food Recognition

Fitto includes a camera-supported meal entry flow. Users can take a photo of a meal and receive food suggestions through an external food recognition service.

The camera-based flow includes:

- Capturing a food image with CameraX
- Sending the optimized image to the food recognition API
- Listing detected food candidates
- Allowing the user to select and adjust the correct food item
- Adding selected food items to the meal log

This feature reduces manual search effort and improves the meal entry experience.

---

### 4. Recipes & Kitchen Support

The recipe module helps users discover, save and manage healthy meal options.

Users can:

- Browse recipes
- Filter recipes by category
- View recipe details
- Save favorite recipes
- Create custom recipes
- Use cooking mode
- Add ingredients to a shopping list
- Track available ingredients with fridge inventory support

This module makes Fitto more than a tracking app by supporting meal planning and daily kitchen organization.

---

### 5. Diet Plans & Intermittent Fasting

Fitto supports structured diet planning for users with different goals.

Users can:

- View predefined diet plans
- Activate a selected diet plan
- Follow planned meals
- Create custom diet plans
- Manage diet plan preferences
- Track intermittent fasting sessions
- Create custom fasting plans

The diet plan and fasting features help users follow a more organized and sustainable nutrition routine.

---

### 6. Social Media & Messaging

Fitto includes social features designed to increase motivation and user interaction.

Users can:

- Create posts
- Share healthy lifestyle content
- Like posts
- Comment on posts
- Follow other users
- View notifications
- Send direct messages
- Create and manage group conversations

Firebase services are used to support social data, real-time messaging states and media storage.

---

### 7. Reports & Progress Analytics

Fitto provides visual progress tracking through reports and charts.

Reports include:

- Calorie tracking
- Macro distribution
- Weight progress
- Body measurement progress
- Daily and historical summaries

Chart-based visualization helps users understand their progress more clearly and stay motivated.

---

### 8. Profile, Settings & Reminders

The profile module allows users to manage personal settings and preferences.

Users can manage:

- Profile information
- Weight history
- Body measurements
- Theme preferences
- Language preferences
- Notification settings
- Privacy options
- Reminder settings

Fitto includes reminder and notification support for meals, water intake, exercise, weight tracking and custom reminders.

---

## AI-Supported Features

Fitto uses Gemini API to provide intelligent guidance throughout the user journey.

AI-supported features include:

- Target weight guidance
- Healthy goal evaluation support
- Personalized plan explanation
- Daily progress interpretation
- Feedback based on meals, water, exercise and weight entries
- Motivational and informative responses

These features help Fitto act as a smart healthy lifestyle assistant rather than a passive data tracking application.

---

## Technologies Used

- Kotlin
- Android
- XML Layout
- Material Design / Material 3
- Room / SQLite
- Firebase Authentication
- Cloud Firestore
- Firebase Realtime Database
- Firebase Storage
- Gemini API
- USDA FoodData Central API
- LogMeal API
- Health Connect
- CameraX
- OkHttp
- MPAndroidChart
- WorkManager
- AlarmManager
- SharedPreferences
- Repository and DAO structures
- Fragment-based navigation

---

## Architecture

Fitto was developed using a modular Android architecture with a single Activity and multiple Fragment-based navigation structure.

Main architectural components include:

- Application layer
- Activity and Fragment-based presentation layer
- ViewModel-based state management
- Repository layer
- Local data layer with Room and DAO structures
- Remote data layer with Firebase and external APIs
- Preference / Store layer for lightweight local states
- Notification and reminder scheduling components

This structure improves maintainability, separates responsibilities and makes the application easier to extend.

---

## Data Management

Fitto uses both local and remote data sources.

### Local Data

Room database is used for structured local data such as:

- Foods
- Favorite foods
- Meal entries
- Meal entry foods
- Diet plans
- Planned meals
- Planned meal foods
- Recipes
- User profile cache

SharedPreferences-based stores are used for lightweight user states such as:

- Water tracking
- Sleep records
- Exercise logs
- Step counter state
- Reminder preferences
- Theme and language preferences
- Nutrition targets
- Diet plan preferences
- Fasting session data
- Notification history

### Remote Data

Firebase is used for cloud-based and social features.

Firebase services include:

- Firebase Authentication for user login
- Cloud Firestore for user profiles, posts, comments, follows and message threads
- Firebase Realtime Database for online status and typing indicators
- Firebase Storage for profile photos, post images, recipe images, group photos and message attachments

---

## External API Integrations

Fitto includes several external integrations:

- **Gemini API** for AI-supported goal guidance and progress interpretation
- **USDA FoodData Central API** for online food search
- **LogMeal API** for camera-based food recognition
- **Health Connect** for step count and active calorie data

---

## Privacy and Security

Fitto includes several privacy and security-focused mechanisms:

- Firebase Authentication for secure login
- Email/password and Google sign-in support
- API keys managed outside source code
- User-specific profile and tracking data
- Health Connect data access only with user permission
- Camera and microphone permissions requested only when needed
- Notification permissions handled according to Android requirements
- Profile visibility and social interaction controls
- Reporting and blocking mechanisms for social features
- Reminder and session management support

---

## Screenshots

| Welcome | Target Weight | AI / Daily Needs Result |
|---|---|---|
| <img src="docs/screenshots/welcome.png" width="220"/> | <img src="docs/screenshots/onboarding-targetweight.jpeg" width="220"/> | <img src="docs/screenshots/Dailyneedsresult.jpeg" width="220"/> |

| Daily Tracking | Food Entry | Recipes |
|---|---|---|
| <img src="docs/screenshots/daily-tracking.jpeg" width="220"/> | <img src="docs/screenshots/foodentry.jpeg" width="220"/> | <img src="docs/screenshots/recipes.jpeg" width="220"/> |

| Recipe Detail | Diet Plans | Social Feed |
|---|---|---|
| <img src="docs/screenshots/recipesdetail.jpeg" width="220"/> | <img src="docs/screenshots/diet-plans.jpeg" width="220"/> | <img src="docs/screenshots/social-feed.jpeg" width="220"/> |

| Messaging | Reports | Profile |
|---|---|---|
| <img src="docs/screenshots/messaging.jpeg" width="220"/> | <img src="docs/screenshots/reports.jpeg" width="220"/> | <img src="docs/screenshots/profile.jpeg" width="220"/> |

> Screenshots were captured using demo data. Personal health data, private messages, real user information and API keys are not shown.
---

## Contributors

This project was developed by:

- [Nursena Duyku](https://github.com/nursena-pc)
- [İrem Nur Şahin](https://github.com/Iremnursh)

---

## Project Status

Developed in 2026 as an Android mobile programming project and portfolio work.

The project is currently maintained as a portfolio project.

---

## License

This project is shared for academic and portfolio purposes.
