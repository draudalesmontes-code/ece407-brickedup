import sqlite3
import time
from bricklink import BricklinkAPI

# Bricklink API credentials
CONSUMER_KEY = ""
CONSUMER_SECRET = ""
TOKEN_VALUE = ""
TOKEN_SECRET = ""

def update_set_prices(db_path='lego_sets.db', max_api_calls=4800):
    # Create the bricklink API client
    client = BricklinkAPI(
        consumer_key=CONSUMER_KEY,
        consumer_secret=CONSUMER_SECRET,
        token_value=TOKEN_VALUE,
        token_secret=TOKEN_SECRET
    )
    
    try:
        # Connect to the database
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Order by set_id DESC to start from highest to lowest
        cursor.execute("""
            SELECT set_id, set_number, name 
            FROM sets 
            WHERE set_number IS NOT NULL 
            AND set_number != '' 
            AND new_price IS NULL 
            AND used_price IS NULL
            ORDER BY set_id DESC  -- Start from highest set_id
        """)
        
        sets = cursor.fetchall()
        
        total_sets = len(sets)
        max_sets = max_api_calls // 2
        sets_to_process = min(total_sets, max_sets)
        
        print(f"Found {total_sets} sets without prices")
        print(f"Ordering by set_id DESC (highest to lowest)")
        print(f"Will process up to {sets_to_process} sets")
        print("="*60)
        
        updated_count = 0
        error_count = 0
        api_call_count = 0
        
        # Process sets
        for idx, (set_id, set_number, name) in enumerate(sets[:sets_to_process], 1):
            if api_call_count >= max_api_calls:
                print("="*60)
                print(f"Reached API call limit ({max_api_calls})")
                print(f"Processed {updated_count} sets successfully")
                break
            
            # needs the -1 cause bricklink uses this
            api_set_num = set_number if set_number.endswith('-1') else f"{set_number}-1"
            
            print(f"[{idx}/{sets_to_process}] Processing {api_set_num} - {name}")
            print(f"  Set ID: {set_id}, API calls: {api_call_count}/{max_api_calls}")
            
            try:
                # getting the new price
                new_price = None
                try:
                    new_data = client.get_set_price_guide(
                        api_set_num,
                        guide_type="sold",
                        new_or_used="N",
                        country_code="US",
                        currency_code="USD"
                    )
                    api_call_count += 1
                    if new_data.get('data') and new_data['data'].get('avg_price'):
                        new_price = float(new_data['data']['avg_price'])
                        print(f"  NEW price: ${new_price:.2f}")
                
                except Exception as e:
                    print(f"  Failed to fetch NEW price: {e}")
                    api_call_count += 1
                
                # adding a delay
                time.sleep(0.5)
                
                # getting the used price
                used_price = None
                try:
                    used_data = client.get_set_price_guide(
                        api_set_num,
                        guide_type="sold",
                        new_or_used="U",
                        country_code="US",
                        currency_code="USD"
                    )
                    
                    api_call_count += 1
                    if used_data.get('data') and used_data['data'].get('avg_price'):
                        used_price = float(used_data['data']['avg_price'])
                        print(f"  USED price: ${used_price:.2f}")
                        
                except Exception as e:
                    print(f"  Not able to fetch USED price: {e}")
                    api_call_count += 1
                
                # updating the database - using set_id as the unique identifier
                cursor.execute("""
                    UPDATE sets 
                    SET new_price = ?, used_price = ? 
                    WHERE set_id = ?
                """, (new_price, used_price, set_id))
                
                updated_count += 1
                
                # commit every 10 updates
                if updated_count % 10 == 0:
                    conn.commit()
                    print(f"  ✓ Committed {updated_count} updates so far")
                
                # rate limiting
                time.sleep(1)
                
            except Exception as e:
                print(f"  ✗ ERROR processing set: {e}")
                error_count += 1
                continue
        
        # final commit
        conn.commit()
        
        print("="*60)
        print("PROCESSING SUMMARY")
        print(f"Successfully updated: {updated_count} sets")
        print(f"Errors encountered: {error_count}")
        print(f"Total API calls used: {api_call_count}/{max_api_calls}")
        print(f"Remaining sets without prices: {total_sets - updated_count}")
        
        # Show progress percentage
        if total_sets > 0:
            percent_complete = (updated_count / total_sets) * 100
            print(f"Overall progress: {percent_complete:.1f}%")
            
            # Show the range of set_ids processed
            if sets_to_process > 0 and len(sets) > 0:
                first_set_id = sets[0][0]  # Highest set_id we started with
                last_processed_idx = min(sets_to_process - 1, len(sets) - 1)
                last_set_id_processed = sets[last_processed_idx][0] if last_processed_idx >= 0 else None
                print(f"Processed set_id range: {first_set_id} → {last_set_id_processed}")
        
        conn.close()
        
    except Exception as e:
        print(f"Database error: {e}")
        raise

if __name__ == "__main__":
    update_set_prices()