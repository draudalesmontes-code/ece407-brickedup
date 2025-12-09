import requests
import json
import os
from dotenv import load_dotenv

load_dotenv()

API_KEY = os.getenv('BRICKSET_API_KEY')
USERNAME = os.getenv('BRICKSET_USERNAME')
PASSWORD = os.getenv('BRICKSET_PASSWORD')
BASE_URL = 'https://brickset.com/api/v3.asmx'

class BricksetAPI:
    def __init__(self):
        self.api_key = API_KEY
        self.user_hash = None
    
    def get_user_hash(self):
        """Get userHash by logging in"""
        if self.user_hash:
            return self.user_hash
            
        soap_body = f'''<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <login xmlns="https://brickset.com/api/">
      <apiKey>{self.api_key}</apiKey>
      <username>{USERNAME}</username>
      <password>{PASSWORD}</password>
    </login>
  </soap:Body>
</soap:Envelope>'''
        
        headers = {
            'Content-Type': 'text/xml; charset=utf-8',
            'SOAPAction': 'https://brickset.com/api/login'
        }
        
        response = requests.post(BASE_URL, data=soap_body, headers=headers)
        
        if response.status_code == 200:
            response_text = response.text
            xml_start = response_text.find('<?xml')
            if xml_start > 0:
                json_part = response_text[:xml_start].strip()
                try:
                    data = json.loads(json_part)
                    if data.get('status') == 'success':
                        self.user_hash = data.get('hash')
                        return self.user_hash
                except json.JSONDecodeError:
                    pass
        return None

    def _extract_json(self, response_text):
        """Extract JSON from SOAP response"""
        brace_count = 0
        json_end = 0
        in_string = False
        escape_next = False
        
        for i, char in enumerate(response_text):
            if escape_next:
                escape_next = False
                continue
                
            if char == '\\':
                escape_next = True
                continue
                
            if char == '"':
                in_string = not in_string
                continue
                
            if not in_string:
                if char == '{':
                    brace_count += 1
                elif char == '}':
                    brace_count -= 1
                    if brace_count == 0:
                        json_end = i + 1
                        break
        
        if json_end > 0:
            json_str = response_text[:json_end]
            try:
                return json.loads(json_str)
            except json.JSONDecodeError:
                pass
        return None

    def get_sets(self, theme=None, year=None, query=None, pageSize=50, pageNumber=1):
        """Get LEGO sets with various filters"""
        params_dict = {
            'pageSize': pageSize,
            'pageNumber': pageNumber
        }
        
        if theme:
            params_dict['theme'] = theme
        if year:
            params_dict['year'] = year
        if query:
            params_dict['query'] = query
        
        soap_body = f'''<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getSets xmlns="https://brickset.com/api/">
      <apiKey>{self.api_key}</apiKey>
      <userHash>{self.user_hash if self.user_hash else ""}</userHash>
      <params>{json.dumps(params_dict)}</params>
    </getSets>
  </soap:Body>
</soap:Envelope>'''
        
        headers = {
            'Content-Type': 'text/xml; charset=utf-8',
            'SOAPAction': 'https://brickset.com/api/getSets'
        }
        
        response = requests.post(BASE_URL, data=soap_body, headers=headers)
        
        if response.status_code == 200:
            return self._extract_json(response.text)
        return None

    def get_themes(self):
        """Get all LEGO themes"""
        soap_body = f'''<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getThemes xmlns="https://brickset.com/api/">
      <apiKey>{self.api_key}</apiKey>
    </getThemes>
  </soap:Body>
</soap:Envelope>'''
        
        headers = {
            'Content-Type': 'text/xml; charset=utf-8',
            'SOAPAction': 'https://brickset.com/api/getThemes'
        }
        
        response = requests.post(BASE_URL, data=soap_body, headers=headers)
        
        if response.status_code == 200:
            return self._extract_json(response.text)
        return None

    def search_sets(self, query, theme=None, year=None, pageSize=20):
        """Search for LEGO sets"""
        return self.get_sets(query=query, theme=theme, year=year, pageSize=pageSize)
    
    def save_themes_to_file(self, filename='lego_themes.txt'):
        """Save all themes to a text file"""
        themes = self.get_themes()
        
        if themes and themes.get('status') == 'success':
            theme_list = themes.get('themes', [])
            
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(f"LEGO Themes - Total: {len(theme_list)}\n")
                f.write("=" * 60 + "\n\n")
                
                for i, theme in enumerate(theme_list, 1):
                    theme_name = theme.get('theme', 'Unknown')
                    set_count = theme.get('setCount', 0)
                    f.write(f"{i}. {theme_name} ({set_count} sets)\n")
            
            print(f"Successfully saved {len(theme_list)} themes to '{filename}'")
            return True
        else:
            print("Failed to retrieve themes")
            return False

# Create API instance
brickset = BricksetAPI()

# Test the complete functionality
if __name__ == "__main__":
    print("BRICKSET API - COMPLETE WORKING VERSION")
    
    # Get the user hash
    user_hash = brickset.get_user_hash()
    if user_hash:
        print(f" Logged in with userHash: {user_hash}")
    else:
        print("  Using API without user authentication")
    
    # Get all themes
    print("\n GETTING ALL THEMES...")
    themes = brickset.get_themes()
    if themes and themes.get('status') == 'success':
        theme_count = len(themes.get('themes', []))
        print(f" Found {theme_count} themes")
        #printing the first 10 themes found
        for i, theme in enumerate(themes.get('themes', [])[:10]):
            print(f"  {i+1}. {theme.get('theme')}")
        if theme_count > 10:
            print(f"  ... and {theme_count - 10} more themes")
    
    print("\n Saving all themes to file...")
    #brickset.save_themes_to_file('lego_themes.txt')
    
    '''
    
    # Get Star Wars sets
    print("\n GETTING STAR WARS SETS...")
    star_wars_sets = brickset.get_sets(theme="Star Wars", pageSize=10)
    if star_wars_sets and star_wars_sets.get('status') == 'success':
        print(f" Found {star_wars_sets.get('matches')} Star Wars sets total")
        print("First 5 sets:")
        for i, lego_set in enumerate(star_wars_sets.get('sets', [])[:5]):
            print(f"  {i+1}. {lego_set.get('number')}: {lego_set.get('name')}")
            print(f"     Year: {lego_set.get('year')}, Pieces: {lego_set.get('pieces')}")
    
    # Get recent sets
    print("\n GETTING RECENT SETS...")
    recent_sets = brickset.get_sets(year="2024", pageSize=5)
    if recent_sets and recent_sets.get('status') == 'success':
        print(f" Found {recent_sets.get('matches')} sets from 2024")
        for i, lego_set in enumerate(recent_sets.get('sets', [])):
            print(f"  {i+1}. {lego_set.get('number')}: {lego_set.get('name')}")
            print(f"     Theme: {lego_set.get('theme')}, Pieces: {lego_set.get('pieces')}")
            
            '''
    
    # Search sets
    print("\n SEARCHING FOR SETS...")
    car_sets = brickset.search_sets(query="lamborghini", pageSize=1)
    if car_sets and car_sets.get('status') == 'success':
        print(f"Found {car_sets.get('matches')} sets with 'car'")
        for i, lego_set in enumerate(car_sets.get('sets', [])):
            print(f"  {i+1}. {lego_set.get('number')}: {lego_set.get('name')}")
            print(f"     Theme: {lego_set.get('theme')}, Year: {lego_set.get('year')}")
    
    
    print("\n Something actually works!")
    
    
    
    #car_sets = brickset.search_sets(query="lamborghini", pageSize=1)
    print(car_sets)