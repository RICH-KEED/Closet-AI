import asyncio
from curl_cffi.requests import AsyncSession

async def main():
    async with AsyncSession(impersonate='chrome110') as s:
        for name, url in [
            ('Amazon', 'https://www.amazon.in/s?k=women+casual+wear'),
            ('Ajio', 'https://www.ajio.com/search/women%20casual%20wear'),
            ('Nykaa', 'https://www.nykaafashion.com/catalogsearch/result/?q=women+casual+wear')
        ]:
            try:
                r = await s.get(url)
                print(f'{name}: {r.status_code}')
            except Exception as e:
                print(f'{name} Error: {e}')

asyncio.run(main())
