# Purpose/Overview
PingPing is a Discord bot that lets server owners register notification subscriptions for Twitch and Youtube livestreamers. Usage is command based and this bot is intended to be self-hosted rather than public.

# Setup
- Create a Discord Bot Application and save the bot token.
- Create a Google Cloud account and create an API key for Youtube Data API v3.
- Create a Twitch Developer account and create an application/server API key. 
- Create a ".env" file with relavent fields filled out matching .env.example.
- The bot has been developed and tested with JavaSE-21

# Deployment
- Create a .jar file using `maven clean package`.
- Run the jar however you see fit. Personally, I run it with Docker in a Google Cloud VM using eclipse-temurin:21-jre-alpine.

# Main Toolings and Libraries
- Javacord for Discord API interaction
- Twitch4j for Twitch API interaction
- Google APIs Client Library for Java for Youtube API interaction
- TinyLog for logging
- SQLite for database

# Screenshots
<img width="631" height="322" alt="image" src="https://github.com/user-attachments/assets/b3a2db9f-df09-4bb4-9080-e68d7c2d8c41" />
<img width="717" height="391" alt="image" src="https://github.com/user-attachments/assets/9efe39be-8d56-4320-8648-b9588fd142d3" />
