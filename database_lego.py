import requests
import json
import os
import sqlite3
import time
import re
from dotenv import load_dotenv

#might want to make it so that sets with no name aren't added to the database..



load_dotenv()

#Haha no information to my keys
API_KEY = os.getenv('BRICKSET_API_KEY')
USERNAME = os.getenv('BRICKSET_USERNAME')
PASSWORD = os.getenv('BRICKSET_PASSWORD')
BASE_URL = 'https://brickset.com/api/v3.asmx'

class BricksetAPI:
    def __init__(self):
        self.api_key = API_KEY
        self.user_hash = None
    
    #getting the user hash
    def get_user_hash(self):
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

    #getting all the json infromation
    def _extract_json(self, response_text):
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

    #getting the sets
    def get_sets(self, theme=None, year=None, query=None, pageSize=500, pageNumber=1):
        
        #getting the sets
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

#database creations
class LegoDatabase:
    def __init__(self, db_name='lego_sets.db'):
        self.conn = sqlite3.connect(db_name)
        self.cursor = self.conn.cursor()
        self.create_table()
    
    def create_table(self):
        
        #creating the sql table
        self.cursor.execute('''
            CREATE TABLE IF NOT EXISTS sets (
                set_id INTEGER PRIMARY KEY,
                set_number TEXT,
                name TEXT,
                theme TEXT,
                subtheme TEXT,
                year INTEGER,
                pieces INTEGER,
                used_price REAL,
                new_price REAL,
                upc TEXT,
                item_number_na TEXT,
                image_url TEXT,
                thumbnail_url TEXT,
                UNIQUE(set_number)
            )
        ''')
        self.conn.commit()
    
    #to help insert the set into the able
    def insert_set(self, set_data):
        # get barcode info
        barcode = set_data.get('barcode', {})
        upc = barcode.get('UPC', '') if barcode else ''
        
        # get item number
        item_number = set_data.get('itemNumber', {})
        item_na = item_number.get('NA', '') if item_number else ''
        
        # get image URLs
        image = set_data.get('image', {})
        image_url = image.get('imageURL', '') if image else ''
        thumbnail_url = image.get('thumbnailURL', '') if image else ''
        
        try:
            self.cursor.execute('''
                INSERT OR REPLACE INTO sets 
                (set_id, set_number, name, theme, subtheme, year, pieces, 
                 used_price, new_price, upc, item_number_na,
                 image_url, thumbnail_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                set_data.get('setID'),
                set_data.get('number'),
                set_data.get('name'),
                set_data.get('theme'),
                set_data.get('subtheme', ''),
                set_data.get('year'),
                set_data.get('pieces', 0) or 0,
                None,  # used_price - null since we do this later
                None,  # new_price - null since we do this later
                upc,
                item_na,
                image_url,
                thumbnail_url
            ))
            return True
        except sqlite3.Error as e:
            print(f"Error inserting set {set_data.get('number')}: {e}")
            return False
    
    #helper method to see stats
    def get_stats(self):
        """Get database statistics"""
        self.cursor.execute('SELECT COUNT(*) FROM sets')
        total = self.cursor.fetchone()[0]
        
        self.cursor.execute('SELECT COUNT(DISTINCT theme) FROM sets')
        themes = self.cursor.fetchone()[0]
        
        return {'total_sets': total, 'total_themes': themes}
    
    def close(self):
        """Close database connection"""
        self.conn.commit()
        self.conn.close()

#using the file of the specifics sets I wanted to use
def load_themes_from_file(filename='lego_themes.txt', test_mode=False):
    """Load theme names from the text file"""
    themes = []
    with open(filename, 'r', encoding='utf-8') as f:
        for line in f:
            match = re.search(r'^\d+\.\s+(.+?)\s+\(\d+\s+sets?\)', line)
            if not match:
                
                #checker for alternate format
                match = re.search(r'^(.+?)\s+\(\d+\s+sets?\)', line)
            if match:
                theme_name = match.group(1).strip()
                themes.append(theme_name)
                if test_mode:
                    break
    return themes

def fetch_all_sets(test_mode=False):
    """Fetch all sets from selected themes and store in database"""
    print("=" * 70)
    
    #testing mode
    if test_mode:
        print("TEST MODE (Animal Crossing Only)")
    else:
        print("LEGO set")
    print("=" * 70)
    
    #Setting up the api
    api = BricksetAPI()
    print("\n Logging in to Brickset...")
    user_hash = api.get_user_hash()
    
    if user_hash:
        print(f"Logged in successfully")
    else:
        print("didnt log in lol")
    
    #loading the themes
    print("\n Loading themes from file...")
    themes = load_themes_from_file(test_mode=test_mode)
    if test_mode:
        print(f"TEST MODE: Processing only animal crossing theme: '{themes[0]}'")
    else:
        print(f"Found {len(themes)} themes to process")
    
    #creating the database
    print("\n Initializing database...")
    db = LegoDatabase()
    
    
    # fetching the sets
    print("\n Fetching sets from Brickset API...")
    print("-" * 70)
    
    total_sets = 0
    failed_themes = []
    
    for i, theme in enumerate(themes, 1):
        print(f"[{i}/{len(themes)}] Processing: {theme}...", end=' ')
        
        try:
            #get page for the total setes
            result = api.get_sets(theme=theme, pageSize=500, pageNumber=1)
            
            if result and result.get('status') == 'success':
                sets = result.get('sets', [])
                matches = result.get('matches', 0)
                
                #inserting the sets
                for set_data in sets:
                    db.insert_set(set_data)
                
                #If there are more pages, fetch them
                if matches > 500:
                    pages = (matches // 500) + 1
                    for page in range(2, pages + 1):
                        #litming for the .5 second wait
                        time.sleep(0.5)
                        result = api.get_sets(theme=theme, pageSize=500, pageNumber=page)
                        if result and result.get('status') == 'success':
                            for set_data in result.get('sets', []):
                                db.insert_set(set_data)
                
                total_sets += matches
                print(f" {matches} sets")
            else:
                print("Failed")
                failed_themes.append(theme)
            
            #rate limiting so maybe brickset doesnt get mad
            time.sleep(0.3) 
            
        except Exception as e:
            print(f"Error: {e}")
            failed_themes.append(theme)
    
    #showing the stats
    print("-" * 70)
    stats = db.get_stats()
    
    print(f"\n SUMMARY:")
    print(f"  -Total sets in database: {stats['total_sets']}")
    print(f"  -Total themes: {stats['total_themes']}")
    print(f"  -Expected sets: {total_sets}")
    
    if failed_themes:
        print(f"\n Failed themes ({len(failed_themes)}):")
        for theme in failed_themes:
            print(f"  - {theme}")
    
    db.close()
    print(f"\n Database saved")
    print("=" * 70)

if __name__ == "__main__":
    #if true then we are in test mode... only does the first theme (Animal Crossing)
    TEST_MODE = False
    
    fetch_all_sets(test_mode=TEST_MODE)