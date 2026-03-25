import json

def extract_json(text: str) -> str:
    start_idx = text.find('{')
    if start_idx == -1:
        return ""
    
    depth = 0
    in_string = False
    escape = False
    
    for i in range(start_idx, len(text)):
        char = text[i]
        
        if escape:
            escape = False
            continue
            
        if char == '\\':
            escape = True
            continue
            
        if char == '"':
            in_string = not in_string
            continue
            
        if not in_string:
            if char == '{':
                depth += 1
            elif char == '}':
                depth -= 1
                
                if depth == 0:
                    return text[start_idx:i+1]
                    
    return ""

with open("ajio_data.json", "r", encoding="utf-8") as f:
    raw_text = f.read()
    
valid_json_str = extract_json(raw_text)
if valid_json_str:
    data = json.loads(valid_json_str)
    print("KEYS:", list(data.keys()))
    
    # Try to find the search products. Ajio usually places them in "gridColumns" or deep inside "apollo" cache.
    if "gridColumns" in data:
        products = data["gridColumns"]
        print(f"Found {len(products)} products in gridColumns")
    else:
        # Sometimes it's nested under something like 'search' -> 'results' 
        print("gridColumns not found at root.")
        
        # Another common spot is apollo state
        # In modern SPAs using Next.js / React it's often window.__INITIAL_STATE__
        ap_state = data.get("apollo", {})
        count = 0
        for k, v in data.items():
            if isinstance(v, dict) and "results" in v:
                print(f"Found 'results' under {k}")
            
        # Let's just recursively search for something dict-like that has "price" and "name"
        def find_products(obj):
            found = []
            if isinstance(obj, dict):
                if "name" in obj and "price" in obj:
                    found.append(obj)
                for k, v in obj.items():
                    res = find_products(v)
                    if res: found.extend(res)
            elif isinstance(obj, list):
                for item in obj:
                    res = find_products(item)
                    if res: found.extend(res)
            return found
            
        prods = find_products(data)
        print(f"Recursively found {len(prods)} product nodes")
        if prods:
            p = prods[0]
            print(f"Sample product keys: {list(p.keys())}")
            print(f"Name: {p.get('name')}, Price: {p.get('price')}")
else:
    print("Could not extract balanced JSON")
