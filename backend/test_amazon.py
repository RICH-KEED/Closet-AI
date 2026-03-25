import asyncio
import httpx
from bs4 import BeautifulSoup

async def test_amazon():
    url = "https://www.amazon.in/s?k=women+casual+wear"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1"
    }

    async with httpx.AsyncClient(headers=headers, follow_redirects=True) as client:
        resp = await client.get(url)
        print(f"Status: {resp.status_code}")
        
        soup = BeautifulSoup(resp.text, "html.parser")
        
        # Amazon search results usually have data-component-type="s-search-result"
        results = soup.find_all("div", attrs={"data-component-type": "s-search-result"})
        print(f"Found {len(results)} results")
        
        for i, item in enumerate(results[:5]):
            title_el = item.find("h2")
            title = title_el.text.strip() if title_el else "Unknown Title"
            
            price_el = item.find("span", class_="a-price-whole")
            price = price_el.text.strip() if price_el else "Unknown Price"
            
            img_el = item.find("img", class_="s-image")
            img_url = img_el.get("src") if img_el else "Unknown Image"
            
            link_el = item.find("a", class_="a-link-normal")
            link = "https://www.amazon.in" + link_el.get("href") if link_el else "Unknown Link"
            
            print(f"{i+1}. {title[:50]} | ₹{price}")
            print(f"   Img: {img_url}")
            print(f"   Link: {link}")

if __name__ == "__main__":
    asyncio.run(test_amazon())
