import asyncio
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

async def test_ajio():
    url = "https://www.ajio.com/search/women%20casual%20wear"
    print(f"Fetching Ajio: {url}")
    
    async with AsyncSession(impersonate="chrome110") as s:
        resp = await s.get(url, timeout=20)
        print(f"Status codes: {resp.status_code}")
        
        soup = BeautifulSoup(resp.text, "html.parser")
        
        # Look for script tags that might contain JSON data
        scripts = soup.find_all("script")
        for i, script in enumerate(scripts):
            if script.string and "window.__PRELOADED_STATE__" in script.string:
                print(f"Found __PRELOADED_STATE__ in script {i}")
                with open("ajio_data.json", "w", encoding="utf-8") as f:
                    # quick slice to save the raw JSON representation roughly
                    start = script.string.find("{")
                    f.write(script.string[start:])
                print("Saved potential JSON data to ajio_data.json")
                return
                
        # If no obvious json state, just dump html
        with open("ajio_search.html", "w", encoding="utf-8") as f:
            f.write(resp.text)
        print("Preloaded state not found, saved HTML to ajio_search.html")

if __name__ == "__main__":
    asyncio.run(test_ajio())
