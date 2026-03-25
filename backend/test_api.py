import asyncio
import httpx
import logging

logging.basicConfig(level=logging.INFO)

async def test_api():
    url = "http://127.0.0.1:8081/api/v1/recommendations"
    payload = {
        "user_profile": {
            "uid": "test_user",
            "gender": "women",
            "bodyType": "hourglass",
            "clothingSize": "M",
            "budget": "under2000",
            "styles": ["casual", "streetwear"],
            "favoriteColors": ["black", "blue"],
            "occasions": ["everyday"]
        },
        "context": {
            "weather": "Sunny",
            "occasion": "casual outing"
        }
    }

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(url, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                print(f"✅ API Success! Returned {len(data)} recommendations.")
                for i, r in enumerate(data[:10]):
                    img = r.get('image_url', '')
                    has_img = "🖼️ YES" if img else "❌ NO"
                    print(f"  {i+1}. [{r.get('match_score')}] [{r.get('platform')}] {r.get('brand')} - {r.get('title')[:30]} | Image: {has_img} ({img})")
            else:
                print(f"❌ API Error {resp.status_code}: {resp.text}")
    except Exception as e:
        print(f"Error calling API: {e}")

if __name__ == "__main__":
    asyncio.run(test_api())
