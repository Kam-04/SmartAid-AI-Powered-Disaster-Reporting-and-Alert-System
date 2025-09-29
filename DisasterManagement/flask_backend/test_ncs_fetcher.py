import logging
import sys
from real_time_data_fetcher import fetch_ncs_earthquake_data

# Configure more detailed logging for debugging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

# Set logger level to INFO
logger = logging.getLogger()
logger.setLevel(logging.INFO)

print("Starting NCS earthquake data fetcher test...")

# Fetch earthquake data
earthquakes = fetch_ncs_earthquake_data()

# Print results
print(f"\nResults: Found {len(earthquakes)} earthquakes from NCS")

if earthquakes:
    print("\nEarthquake details:")
    for i, quake in enumerate(earthquakes):
        props = quake.get("properties", {})
        coords = quake.get("geometry", {}).get("coordinates", [0, 0, 0])
        print(f"{i+1}. Magnitude {props.get('mag', 'N/A')} at {coords[1]},{coords[0]} depth:{coords[2]}km - {props.get('place', 'Unknown location')}")
else:
    print("No earthquakes found. Check the logs for details on what went wrong.")

print("\nTest completed!") 