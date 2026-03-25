import os
import logging
import aiohttp
from typing import Optional

logger = logging.getLogger(__name__)

class WeatherService:
    def __init__(self):
        self.base_url = "https://api.openweathermap.org/data/2.5/weather"

    async def get_weather(self, lat: float, lon: float) -> Optional[dict]:
        # `main.py` loads dotenv after importing `weather_service`, so re-check env here.
        # Try multiple keys because you may have multiple "active" keys configured in OpenWeather.
        keys: list[str] = []
        for env_var in ("OPENWEATHER_API_KEY", "OPENWEATHER_API_KEY_DEFAULT", "OPENWEATHER_API_KEY_CLOSETAI"):
            val = os.getenv(env_var)
            if val and val not in keys:
                keys.append(val)

        if not keys:
            logger.warning("OpenWeather API key(s) not set. Weather context disabled.")
            return None

        params = {
            "lat": lat,
            "lon": lon,
            "units": "metric"  # Get temperature in Celsius
        }

        try:
            # Disable proxy auto-detection; it can cause long timeouts in some setups.
            timeout = aiohttp.ClientTimeout(total=15, sock_connect=10, sock_read=10)
            async with aiohttp.ClientSession(timeout=timeout, trust_env=False) as session:
                for api_key in keys:
                    params["appid"] = api_key
                    async with session.get(self.base_url, params=params) as response:
                        if response.status == 200:
                            data = await response.json()
                            return {
                                "temp": data["main"]["temp"],
                                "condition": data["weather"][0]["main"].lower(),  # e.g., 'rain', 'clear', 'clouds'
                                "description": data["weather"][0]["description"],
                            }
                        if response.status == 401:
                            # Try next key if one is invalid.
                            logger.warning("Weather API key rejected (401). Trying next key if available.")
                            continue

                        # For non-401 errors, fail fast.
                        logger.error(f"Weather API error: {response.status} - {await response.text()}")
                        return None

                return None
        except Exception as e:
            logger.error(f"Failed to fetch weather data: {e}")
            return None

weather_service = WeatherService()
