"""
MongoDB Earthquake Data Update Script

This script fetches earthquake data from NCS India and updates the MongoDB collection.
"""

import logging
import sys
from datetime import datetime
from real_time_data_fetcher import get_mongodb_client, fetch_ncs_earthquake_data, process_earthquake_data, update_earthquake_data

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def main():
    print(f"Starting MongoDB earthquake data update at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Connect to MongoDB
    client, db = get_mongodb_client()
    
    if client is None or db is None:
        print("Failed to connect to MongoDB, exiting.")
        return
    
    try:
        # Update earthquake data
        earthquake_count = update_earthquake_data(db)
        print(f"Completed earthquake data update: {earthquake_count} records processed")
        
    except Exception as e:
        print(f"Error during update: {str(e)}")
    finally:
        # Close MongoDB connection
        client.close()
        print("MongoDB connection closed")
    
    print(f"Update completed at {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

if __name__ == "__main__":
    main() 