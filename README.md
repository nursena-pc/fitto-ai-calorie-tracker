# Fitto - AI-Supported Healthy Lifestyle, Nutrition & Social Tracking App

> 🇹🇷 Fitto, kullanıcıların kalori, makro besin, su, egzersiz, uyku, kilo ve sağlıklı yaşam verilerini takip edebildiği; tarifler, diyet planları, sosyal medya, mesajlaşma, hatırlatıcılar ve yapay zekâ destekli rehberlik özelliklerini bir araya getiren kapsamlı bir Android mobil uygulamasıdır.

Fitto is an AI-supported Android mobile application that combines calorie and macro tracking, personalized healthy lifestyle guidance, recipe and diet plan management, social sharing, messaging, reminders and progress reporting in a single mobile platform.

## Project Overview

Fitto was designed as a comprehensive healthy lifestyle assistant rather than a simple calorie tracking application. The application helps users track their daily nutrition and lifestyle habits while also offering personalized onboarding, AI-supported guidance, recipe management, diet planning, social interaction, messaging and progress visualization.

The project aims to make healthy living more sustainable by allowing users to manage nutrition, activity, hydration, sleep, weight and personal goals from one integrated mobile experience.

## Project Highlights

- Personalized onboarding and calorie goal calculation
- AI-supported target weight guidance with Gemini API
- AI-powered daily progress interpretation based on user inputs
- Meal, calorie and macro tracking
- Water, exercise, sleep, step and weight tracking
- Weight and body measurement tracking
- Recipe browsing, filtering and custom recipe creation
- Diet plan management and custom diet plan creation
- Intermittent fasting tracking
- Social media features such as posts, likes, comments and following
- Direct messaging and group messaging support
- Firebase-based authentication, social data and media storage
- Camera-supported food recognition flow
- Health Connect integration for step and active calorie data
- Progress reports and chart-based visualization
- Reminder and notification system
- Modern Android UI with Material Design components

## Main Modules

Fitto is structured around five main modules:

### Daily Tracking

The daily tracking module allows users to monitor their daily health and nutrition data.

Users can track:

- Meals
- Calories
- Protein, carbohydrate and fat values
- Water intake
- Exercise records
- Sleep duration
- Step count
- Active calories
- Weight and body measurements

### Recipes and Kitchen Support

The recipe module helps users discover and manage healthy meal options.

Users can:

- Browse recipes
- Filter recipes by category
- View recipe details
- Save favorite recipes
- Create custom recipes
- Add ingredients to a shopping list
- Use kitchen-support features such as fridge and ingredient tracking

### Diet Plans

Fitto supports diet plan management for users with different health and nutrition goals.

Users can:

- View predefined diet plans
- Activate a diet plan
- Follow planned meals
- Create custom diet plans
- Manage intermittent fasting sessions

### Social Media and Messaging

Fitto includes social features to increase user motivation and interaction.

Users can:

- Create posts
- Like and comment on posts
- Follow other users
- Send direct messages
- Join group conversations
- Share healthy lifestyle progress and content

### Reports and Profile

The reports and profile modules allow users to manage their personal settings and track their progress over time.

Users can:

- View calorie reports
- Track macro distribution
- Monitor weight progress
- Manage profile information
- Configure reminders
- Change theme and language preferences
- Manage privacy and notification settings

## AI-Supported Features

Fitto uses Gemini API to support users during goal setting and daily progress tracking.

AI-supported features include:

- Target weight guidance
- Daily progress interpretation
- Feedback based on meals, water, exercise and weight entries
- Personalized motivational and informative responses
- Healthier goal recommendation support during onboarding

These features help Fitto act not only as a tracking application, but also as a smart assistant that guides users throughout their healthy lifestyle journey.

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

## Architecture

Fitto was developed using a modular Android architecture with a single Activity and multiple Fragment-based navigation structure.

Main architectural components include:

- Application layer
- Activity and Fragment-based presentation layer
- Repository layer
- Local data layer with Room and DAO structures
- Remote data layer with Firebase and external APIs
- Preference / Store layer for lightweight local states
- ViewModel-based state management for onboarding and tracking flows

This structure improves maintainability, separates responsibilities and makes the application easier to extend.

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
- Notification history

### Remote Data

Firebase is used for cloud-based and social features.

Firebase services include:

- Firebase Authentication for user login
- Cloud Firestore for user profiles, posts, comments, follows and message threads
- Firebase Realtime Database for online status and typing indicators
- Firebase Storage for profile photos, post images, recipe images, group photos and message attachments

## External API Integrations

Fitto includes several external integrations:

- **Gemini API** for AI-supported goal guidance and progress interpretation
- **USDA FoodData Central API** for online food search
- **LogMeal API** for camera-based food recognition
- **Health Connect** for step count and active calorie data

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

## Screenshots

Screenshots will be added soon.

## Contributors

This project was developed by:

- [Nursena Duyku](https://github.com/nursena-pc)
- [İrem Nur Şahin](https://github.com/Iremnursh)

## Project Status

Developed in 2026 as an Android mobile programming project and portfolio work.

## License

This project is shared for academic and portfolio purposes.
