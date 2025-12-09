import sqlite3

def remove_empty_sets(db_path='lego_sets.db'):
    """
    Remove all LEGO sets with blank or NULL names from the database.
    
    Args:
        db_path: Path to the SQLite database file
    """
    try:
        # Connect to the database
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Count empty sets before deletion
        cursor.execute("""
            SELECT COUNT(*) FROM sets 
            WHERE name IS NULL OR name = '' OR name = '{?}' OR pieces IS NULL
        """)
        empty_count = cursor.fetchone()[0]
        
        if empty_count == 0:
            print("No empty sets found in the database.")
        else:
            print(f"Found {empty_count} empty set(s) to delete.")
            
            # Delete empty sets
            cursor.execute("""
                DELETE FROM sets 
                WHERE name IS NULL OR name = '' OR name = '{?}' OR pieces IS NULL
            """)
            
            # Commit the changes
            conn.commit()
            
            print(f"Successfully deleted {cursor.rowcount} empty set(s).")
        
        # Close the connection
        conn.close()
        
    except sqlite3.Error as e:
        print(f"Database error: {e}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    remove_empty_sets()