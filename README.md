# Fitto - AI-Supported Calorie & Healthy Lifestyle Tracker

> 🇹🇷 Fitto, kullanıcıların kalori, makro besin, su, egzersiz, uyku, kilo ve sağlıklı yaşam verilerini takip edebildiği; Gemini API desteğiyle hedef kilo belirleme ve günlük ilerleme yorumlama süreçlerinde yapay zekâ destekli yönlendirme sunan Android mobil uygulamasıdır.

Fitto is an AI-supported Android mobile application designed for calorie, nutrition, activity, water, sleep and weight tracking. The application combines personalized onboarding, nutrition tracking, AI-powered guidance, Firebase-based social features and progress reporting in a single mobile platform.

## Project Overview

Maintaining healthy eating habits and tracking daily lifestyle data can be difficult without a structured and personalized system. Fitto aims to make this process easier by allowing users to track their meals, calories, macros, water intake, exercise, sleep, weight and body measurements from one mobile application.

The application also includes AI-supported features using Gemini API. During onboarding, Fitto helps users define a target weight more consciously. As users enter daily data such as meals, water, exercise and weight updates, the AI-supported feedback mechanism can interpret the user's current progress and provide meaningful guidance.

## Project Highlights

- Personalized calorie and macro target calculation
- AI-supported target weight guidance with Gemini API
- AI-powered daily progress interpretation based on user inputs
- Meal, calorie and macro tracking
- Water, exercise, sleep, step and weight tracking
- Diet plans and recipe management
- Firebase-based authentication and social features
- Camera-supported food entry flow
- Health Connect integration for activity data
- Progress reports and charts
- Reminder and notification system
- Modern Android UI with Material Design

## Key Features

### Personalized Onboarding

Fitto collects user information such as age, height, weight, gender, activity level, goal type and target weight to calculate personalized daily calorie and macro goals.

The application evaluates whether the selected goal is healthy, aggressive or not recommended, and provides safer guidance when needed.

### AI-Supported Guidance

Fitto uses Gemini API to support users during the goal-setting and daily tracking process.

AI-supported features include:

- Target weight guidance
- Daily progress interpretation
- Feedback based on meals, water, exercise and weight entries
- Personalized motivational and informative responses

### Daily Tracking

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

### Recipes and Diet Plans

Fitto provides recipe and diet plan features to support healthy eating habits.

Users can:

- View recipes
- Filter recipes by category
- Save favorite recipes
- Create custom recipes
- Follow diet plans
- Create personalized diet plans

### Social Features

The application includes social interaction features supported by Firebase.

Users can:

- Create posts
- Like and comment on posts
- Follow other users
- Send messages
- Manage profile visibility and social interactions

### Reports and Progress Tracking

Fitto visualizes user progress with charts and reports.

Reports include:

- Calorie tracking
- Macro distribution
- Weight progress
- Daily and historical progress summaries

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

## Data Management

Fitto uses both local and remote data sources.

### Local Data

Room database is used for structured local data such as:

- Foods
- Favorite foods
- Meal entries
- Diet plans
- Planned meals
- Recipes
- User profile cache

SharedPreferences-based stores are used for lightweight user states such as:

- Water tracking
- Sleep records
- Exercise logs
- Reminder preferences
- Theme and language preferences
- Nutrition targets

### Remote Data

Firebase is used for:

- User authentication
- User profiles
- Social posts
- Comments
- Follow relationships
- Messaging
- Media storage

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
- Reminder and notification permissions handled according to Android requirements

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
