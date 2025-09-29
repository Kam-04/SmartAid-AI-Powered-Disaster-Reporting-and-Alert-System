import requests
import json
from datetime import datetime, timedelta
import logging
from pymongo import MongoClient
import time
import os
import re
from bs4 import BeautifulSoup

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# MongoDB connection
def get_mongodb_client():
    try:
        client = MongoClient("mongodb+srv://seismology:seismo-pass@cluster0.fdeuxad.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0",
                    tls=True, 
                    tlsAllowInvalidCertificates=True)
        # Specify your database and collections
        db = client['disaster_management']
        earthquakes_collection = db['earthquakes_data']
        return client, db
    except Exception as e:
        logger.error(f"Failed to connect to MongoDB: {str(e)}")
        return None, None

# Real-time data sources
USGS_EARTHQUAKE_API = "https://earthquake.usgs.gov/fdsnws/event/1/query"
NCS_WEBSITE_URL = "https://riseq.seismo.gov.in/"  # National Center for Seismology India

# Indian coordinates boundary box (approximate)
INDIA_BOUNDS = {
    "min_latitude": 6.5,   # Southern tip
    "max_latitude": 37.5,  # Northern border
    "min_longitude": 68.0, # Western border
    "max_longitude": 98.0  # Eastern border
}

# Function to fetch earthquake data from USGS API
def fetch_usgs_earthquake_data(days=7):
    """
    Fetch earthquake data for India from USGS
    """
    logger.info(f"Fetching earthquake data from USGS for the past {days} days...")
    
    try:
        # Calculate start time
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(days=days)
        
        # Format times for USGS API
        start_str = start_time.strftime("%Y-%m-%dT%H:%M:%S")
        end_str = end_time.strftime("%Y-%m-%dT%H:%M:%S")
        
        # Set parameters for API request
        params = {
            "format": "geojson",
            "starttime": start_str,
            "endtime": end_str,
            "minlatitude": INDIA_BOUNDS["min_latitude"],
            "maxlatitude": INDIA_BOUNDS["max_latitude"],
            "minlongitude": INDIA_BOUNDS["min_longitude"],
            "maxlongitude": INDIA_BOUNDS["max_longitude"],
            "minmagnitude": 2.5  # Only get earthquakes of magnitude 2.5 or greater
        }
        
        # Make request to USGS API
        response = requests.get(USGS_EARTHQUAKE_API, params=params)
        
        if response.status_code == 200:
            data = response.json()
            logger.info(f"Successfully fetched {len(data['features'])} earthquakes from USGS")
            return data["features"]
        else:
            logger.error(f"Failed to fetch earthquake data: {response.status_code}")
            return []
            
    except Exception as e:
        logger.error(f"Error fetching earthquake data: {str(e)}")
        return []

# Function to fetch earthquake data from National Center for Seismology India
def fetch_ncs_earthquake_data(days=7):
    """
    Fetch earthquake data for India from National Center for Seismology (NCS)
    """
    logger.info(f"Fetching earthquake data from NCS India for the past {days} days...")
    
    try:
        # Make request to NCS website
        logger.info(f"Requesting data from NCS website: {NCS_WEBSITE_URL}")
        response = requests.get(NCS_WEBSITE_URL, timeout=30)
        
        if response.status_code != 200:
            logger.error(f"Failed to fetch NCS data: {response.status_code}")
            return []
        
        logger.info(f"Successfully got response from NCS website: {len(response.content)} bytes")
        
        # Parse HTML content
        soup = BeautifulSoup(response.content, 'html.parser')
        
        # Find all tables in the page to figure out which one contains earthquake data
        all_tables = soup.find_all('table')
        logger.info(f"Found {len(all_tables)} tables on the NCS website")
        
        # If no tables found, return empty
        if not all_tables:
            logger.warning("No tables found on the NCS website")
            return []
        
        # Process each table to find earthquake data
        earthquake_data = []
        
        # Approach 1: Try to find earthquake data in all tables
        for table_idx, table in enumerate(all_tables):
            table_rows = table.find_all('tr')
            
            if len(table_rows) <= 1:
                logger.info(f"Table {table_idx} has only {len(table_rows)} rows, skipping")
                continue
                
            logger.info(f"Examining table {table_idx} with {len(table_rows)} rows")
            
            # Check first row for headers
            first_row = table_rows[0]
            header_cells = first_row.find_all(['th', 'td'])
            header_texts = [cell.get_text(strip=True).lower() for cell in header_cells]
            
            logger.info(f"Table {table_idx} headers: {header_texts}")
            
            # Check if this looks like an earthquake table (contains magnitude, lat, lon, etc.)
            earthquake_keywords = ['magnitude', 'mag', 'lat', 'lon', 'depth', 'location', 'time', 'date']
            keyword_matches = sum(1 for keyword in earthquake_keywords if any(keyword in header for header in header_texts))
            
            if keyword_matches < 2:  # Need at least 2 keyword matches to consider it an earthquake table
                logger.info(f"Table {table_idx} doesn't appear to be an earthquake table (only {keyword_matches} keywords matched)")
                continue
                
            logger.info(f"Table {table_idx} appears to be an earthquake table with {keyword_matches} keyword matches")
            
            # Try to determine column mappings
            mag_col = next((i for i, h in enumerate(header_texts) if 'magnitude' in h or 'mag' in h), -1)
            date_col = next((i for i, h in enumerate(header_texts) if 'date' in h or 'time' in h), -1)
            lat_col = next((i for i, h in enumerate(header_texts) if 'lat' in h), -1)
            lon_col = next((i for i, h in enumerate(header_texts) if 'lon' in h or 'long' in h), -1)
            depth_col = next((i for i, h in enumerate(header_texts) if 'depth' in h), -1)
            location_col = next((i for i, h in enumerate(header_texts) if 'region' in h or 'location' in h or 'place' in h), -1)
            
            # If we couldn't identify critical columns, try positional mapping
            if mag_col == -1 or (lat_col == -1 and lon_col == -1):
                logger.info(f"Couldn't identify critical columns, trying positional mapping for table {table_idx}")
                
                # Look at the first data row to see if it contains numeric values in expected positions
                if len(table_rows) > 1:
                    data_cells = table_rows[1].find_all('td')
                    cell_texts = [cell.get_text(strip=True) for cell in data_cells]
                    
                    # Try to find columns with numeric data that could be magnitude, lat, or lon
                    numeric_cols = []
                    for i, text in enumerate(cell_texts):
                        try:
                            float(text.replace(',', '.'))  # Handle different decimal separators
                            numeric_cols.append(i)
                        except (ValueError, TypeError):
                            pass
                    
                    logger.info(f"Numeric columns in first data row: {numeric_cols}")
                    
                    if len(numeric_cols) >= 3:
                        # Assume first numeric is magnitude, others are lat/lon/depth
                        mag_col = numeric_cols[0]
                        lat_col = numeric_cols[1]
                        lon_col = numeric_cols[2]
                        depth_col = numeric_cols[3] if len(numeric_cols) > 3 else -1
                        
                        # Try to find a column with text that could be location
                        text_cols = [i for i, text in enumerate(cell_texts) if i not in numeric_cols and text]
                        location_col = text_cols[0] if text_cols else -1
                        
                        # Try to find a date column
                        for i, text in enumerate(cell_texts):
                            # Check for common date patterns
                            if re.search(r'\d{1,4}[-/]\d{1,2}[-/]\d{1,4}', text):
                                date_col = i
                                break
            
            logger.info(f"Column mapping for table {table_idx}: mag={mag_col}, date={date_col}, lat={lat_col}, lon={lon_col}, depth={depth_col}, location={location_col}")
            
            # Process data rows
            for row_idx, row in enumerate(table_rows[1:], 1):  # Skip header
                try:
                    cells = row.find_all('td')
                    if not cells:
                        continue
                        
                    cell_texts = [cell.get_text(strip=True) for cell in cells]
                    logger.info(f"Row {row_idx} cells: {cell_texts}")
                    
                    # Extract magnitude
                    magnitude = None
                    if mag_col >= 0 and mag_col < len(cells):
                        mag_text = cells[mag_col].get_text(strip=True)
                        mag_match = re.search(r'(\d+\.\d+)', mag_text)
                        if mag_match:
                            magnitude = float(mag_match.group(1))
                    
                    # If magnitude column not found or value extraction failed, try each cell
                    if magnitude is None:
                        for i, cell in enumerate(cells):
                            cell_text = cell.get_text(strip=True)
                            mag_match = re.search(r'(\d+\.\d+)', cell_text)
                            if mag_match:
                                try:
                                    test_mag = float(mag_match.group(1))
                                    if 0 < test_mag < 10:  # Reasonable magnitude range
                                        magnitude = test_mag
                                        mag_col = i  # Update the magnitude column
                                        logger.info(f"Found magnitude {magnitude} in column {i}")
                                        break
                                except ValueError:
                                    pass
                    
                    if magnitude is None:
                        logger.warning(f"Could not extract magnitude from row {row_idx}")
                        continue
                    
                    # Extract latitude and longitude
                    lat, lon = None, None
                    
                    # Try to get from designated columns
                    if lat_col >= 0 and lat_col < len(cells) and lon_col >= 0 and lon_col < len(cells):
                        try:
                            lat_text = cells[lat_col].get_text(strip=True)
                            lon_text = cells[lon_col].get_text(strip=True)
                            lat = float(lat_text)
                            lon = float(lon_text)
                        except (ValueError, TypeError):
                            pass
                    
                    # If lat/lon extraction failed, try to find them in any cell
                    if lat is None or lon is None:
                        # Look for patterns like "12.34, 56.78" or "12.34 N, 56.78 E"
                        for cell in cells:
                            cell_text = cell.get_text(strip=True)
                            latlon_match = re.search(r'(\d+\.\d+)[,\s]+(\d+\.\d+)', cell_text)
                            if latlon_match:
                                try:
                                    lat = float(latlon_match.group(1))
                                    lon = float(latlon_match.group(2))
                                    break
                                except ValueError:
                                    pass
                    
                    # If we still don't have lat/lon, skip this row
                    if lat is None or lon is None:
                        logger.warning(f"Could not extract lat/lon from row {row_idx}")
                        continue
                    
                    # Check if coordinates are within India bounds or nearby
                    if (lat < INDIA_BOUNDS['min_latitude'] - 5 or lat > INDIA_BOUNDS['max_latitude'] + 5 or
                            lon < INDIA_BOUNDS['min_longitude'] - 5 or lon > INDIA_BOUNDS['max_longitude'] + 5):
                        logger.warning(f"Coordinates ({lat}, {lon}) are far outside India bounds, skipping")
                        continue
                    
                    # Extract depth
                    depth = 10.0  # Default depth if not available
                    if depth_col >= 0 and depth_col < len(cells):
                        depth_text = cells[depth_col].get_text(strip=True)
                        depth_match = re.search(r'(\d+(?:\.\d+)?)', depth_text)
                        if depth_match:
                            depth = float(depth_match.group(1))
                    
                    # Extract or generate event time
                    event_time = None
                    if date_col >= 0 and date_col < len(cells):
                        date_text = cells[date_col].get_text(strip=True)
                        
                        # Try common date formats
                        date_formats = [
                            '%Y-%m-%d %H:%M:%S',
                            '%Y-%m-%d %H:%M:%S IST',
                            '%d-%m-%Y %H:%M:%S',
                            '%d-%m-%Y %H:%M',
                            '%d/%m/%Y %H:%M:%S',
                            '%d/%m/%Y %H:%M',
                            '%Y/%m/%d %H:%M:%S'
                        ]
                        
                        for date_format in date_formats:
                            try:
                                event_time = datetime.strptime(date_text, date_format)
                                break
                            except ValueError:
                                continue
                    
                    # If date parsing failed or column not found, use current time
                    if event_time is None:
                        event_time = datetime.now()
                        logger.warning(f"Using current time for row {row_idx} as date parsing failed")
                    
                    # Check if in time range
                    if event_time < datetime.now() - timedelta(days=days):
                        logger.info(f"Event from row {row_idx} is older than {days} days, skipping")
                        continue
                    
                    # Extract or generate location text
                    location_text = None
                    if location_col >= 0 and location_col < len(cells):
                        location_text = cells[location_col].get_text(strip=True)
                    
                    if not location_text:
                        # Generate location from coordinates
                        state = get_indian_state_from_coordinates(lat, lon)
                        location_text = f"Earthquake near {state}, India"
                    
                    # Create earthquake feature
                    feature = {
                        "properties": {
                            "mag": magnitude,
                            "place": location_text,
                            "time": int(event_time.timestamp() * 1000),
                            "magType": "ML",  # Default magnitude type
                            "felt": None,
                            "tsunami": 0
                        },
                        "geometry": {
                            "coordinates": [lon, lat, depth]
                        }
                    }
                    
                    earthquake_data.append(feature)
                    logger.info(f"Processed earthquake from table {table_idx}, row {row_idx}: M{magnitude} at {lat},{lon} depth:{depth}km {event_time}")
                    
                except Exception as e:
                    logger.error(f"Error processing row {row_idx} in table {table_idx}: {str(e)}")
                    continue
        
        # If we found no data in any table, try one more approach
        if not earthquake_data:
            logger.info("No earthquake data found in tables, trying alternative approach with direct text parsing")
            
            # Look for text that contains earthquake information
            page_text = soup.get_text()
            
            # Look for patterns in the text
            earthquake_patterns = [
                r'M(?:agnitude)?\s*(\d+\.\d+).*?(\d+\.\d+)[°\s]*[NS][,\s]+(\d+\.\d+)[°\s]*[EW]',
                r'(\d+\.\d+)[°\s]*[NS][,\s]+(\d+\.\d+)[°\s]*[EW].*?M(?:agnitude)?\s*(\d+\.\d+)',
                r'(\d+\.\d+)[°\s]*[NS][,\s]+(\d+\.\d+)[°\s]*[EW].*?depth\s*(\d+(?:\.\d+)?)'
            ]
            
            for pattern in earthquake_patterns:
                matches = re.findall(pattern, page_text)
                if matches:
                    logger.info(f"Found {len(matches)} potential earthquakes using text pattern")
                    
                    for match in matches:
                        try:
                            # Extract data based on the pattern
                            if 'NS' in pattern:
                                # Pattern is lat, lon, mag or similar
                                lat, lon, mag_or_depth = float(match[0]), float(match[1]), float(match[2])
                                magnitude = mag_or_depth if mag_or_depth < 10 else 5.0
                                depth = mag_or_depth if mag_or_depth >= 10 else 10.0
                            else:
                                # Pattern is mag, lat, lon
                                magnitude, lat, lon = float(match[0]), float(match[1]), float(match[2])
                                depth = 10.0
                            
                            # Add to earthquake data
                            feature = {
                                "properties": {
                                    "mag": magnitude,
                                    "place": f"Earthquake near {get_indian_state_from_coordinates(lat, lon)}, India",
                                    "time": int(datetime.now().timestamp() * 1000),
                                    "magType": "ML",
                                    "felt": None,
                                    "tsunami": 0
                                },
                                "geometry": {
                                    "coordinates": [lon, lat, depth]
                                }
                            }
                            
                            earthquake_data.append(feature)
                            logger.info(f"Added earthquake from text pattern: M{magnitude} at {lat},{lon} depth:{depth}km")
                            
                        except Exception as e:
                            logger.error(f"Error processing text match: {str(e)}")
                            continue
        
        logger.info(f"Successfully fetched {len(earthquake_data)} earthquakes from NCS India")
        return earthquake_data
        
    except Exception as e:
        logger.error(f"Error fetching NCS earthquake data: {str(e)}")
        return []

# Function to map Indian state based on coordinates
def get_indian_state_from_coordinates(latitude, longitude):
    """
    Map coordinates to Indian state (simplified method)
    In a production environment, this should use GeoJSON boundaries or a geocoding service
    """
    # This is a simplified version - in production you would use a proper geocoding service
    # or GeoJSON boundaries for Indian states
    
    # North India
    if latitude > 28.0:
        if longitude < 77.0:
            return "Jammu and Kashmir"
        elif longitude < 80.0:
            return "Himachal Pradesh"
        elif longitude < 85.0:
            return "Uttarakhand"
        elif longitude < 90.0:
            return "Sikkim"
        else:
            return "Arunachal Pradesh"
    
    # Central India
    elif latitude > 20.0:
        if longitude < 75.0:
            return "Gujarat"
        elif longitude < 78.0:
            return "Madhya Pradesh"
        elif longitude < 82.0:
            return "Uttar Pradesh"
        elif longitude < 87.0:
            return "Bihar"
        else:
            return "West Bengal"
    
    # South India
    else:
        if longitude < 75.0:
            return "Kerala"
        elif longitude < 80.0:
            return "Tamil Nadu"
        elif longitude < 85.0:
            return "Andhra Pradesh"
        else:
            return "Odisha"

# Function to process earthquake data
def process_earthquake_data(earthquake_features, source="USGS"):
    """
    Process earthquake data and format for our database
    """
    processed_data = []
    
    for feature in earthquake_features:
        properties = feature.get("properties", {})
        geometry = feature.get("geometry", {})
        coordinates = geometry.get("coordinates", [0, 0, 0])
        
        # Extract location info
        longitude = coordinates[0]
        latitude = coordinates[1]
        depth = coordinates[2]
        
        # Extract time
        time_ms = properties.get("time")
        if time_ms:
            earthquake_time = datetime.fromtimestamp(time_ms / 1000)
        else:
            earthquake_time = datetime.utcnow()
            
        # Get Indian state from coordinates
        indian_state = get_indian_state_from_coordinates(latitude, longitude)
        
        # Format data for our database
        earthquake_data = {
            "time": earthquake_time,
            "latitude": latitude,
            "longitude": longitude,
            "depth": depth,
            "magnitude": properties.get("mag", 0),
            "magnitude_type": properties.get("magType", "ML"),
            "place": properties.get("place", "Unknown location"),
            "region": indian_state,
            "location": properties.get("place", "Unknown location"),
            "source": source,
            "felt": properties.get("felt"),
            "tsunami": properties.get("tsunami", 0),
            "source_url": properties.get("url"),
            "data_updated_at": datetime.utcnow()
        }
        
        processed_data.append(earthquake_data)
    
    return processed_data

# Function to fetch flood data from satellite APIs or process rainfall data
def fetch_flood_data():
    """
    Fetch flood data for India from available sources
    Note: This is a placeholder - in a real implementation, you would use:
    - NASA MODIS/Sentinel satellite data APIs
    - Indian Meteorological Department (IMD) data (requires partnership/approval)
    - Central Water Commission (CWC) data (requires partnership/approval)
    - INCOIS (Indian National Centre for Ocean Information Services)
    """
    try:
        # Placeholder for real API integration
        # For now, simulate flood data
        simulated_flood_data = [
            {
                "state": "Assam",
                "district": "Dhemaji",
                "severity": "high",
                "affected_area_sq_km": 120.5,
                "affected_population": 15000,
                "start_date": datetime.now() - timedelta(days=3),
                "river_level": "above danger mark",
                "rainfall_mm": 205.5,
                "source": "simulated",
                "latitude": 27.4833,
                "longitude": 94.5833
            },
            {
                "state": "Bihar",
                "district": "Darbhanga",
                "severity": "medium",
                "affected_area_sq_km": 80.2,
                "affected_population": 8500,
                "start_date": datetime.now() - timedelta(days=5),
                "river_level": "rising",
                "rainfall_mm": 150.8,
                "source": "simulated",
                "latitude": 26.1542,
                "longitude": 85.8918
            },
            {
                "state": "Kerala",
                "district": "Wayanad",
                "severity": "medium",
                "affected_area_sq_km": 45.0,
                "affected_population": 5000,
                "start_date": datetime.now() - timedelta(days=2),
                "river_level": "warning level",
                "rainfall_mm": 180.2,
                "source": "simulated",
                "latitude": 11.6854,
                "longitude": 76.1320
            }
        ]
        
        # Add data_updated_at timestamp
        for flood in simulated_flood_data:
            flood["data_updated_at"] = datetime.utcnow()
        
        return simulated_flood_data
    
    except Exception as e:
        logger.error(f"Error fetching flood data: {str(e)}")
        return []

# Function to update earthquake data in database
def update_earthquake_data(db):
    try:
        # Fetch data from multiple sources
        usgs_earthquake_features = fetch_usgs_earthquake_data()
        ncs_earthquake_features = fetch_ncs_earthquake_data()
        
        all_earthquake_features = []
        
        # Process USGS data if available
        if usgs_earthquake_features:
            usgs_processed_data = process_earthquake_data(usgs_earthquake_features, source="USGS")
            all_earthquake_features.extend(usgs_processed_data)
            logger.info(f"Processed {len(usgs_processed_data)} USGS earthquake records")
        else:
            logger.warning("No USGS earthquake data fetched")
        
        # Process NCS data if available
        if ncs_earthquake_features:
            ncs_processed_data = process_earthquake_data(ncs_earthquake_features, source="NCS India")
            all_earthquake_features.extend(ncs_processed_data)
            logger.info(f"Processed {len(ncs_processed_data)} NCS India earthquake records")
        else:
            logger.warning("No NCS India earthquake data fetched")
        
        if not all_earthquake_features:
            logger.warning("No earthquake data available from any source")
            return 0
        
        # Get the earthquakes collection
        earthquakes_collection = db['earthquakes_data']
        
        # Track number of updates
        new_count = 0
        updated_count = 0
        
        # Update database
        for earthquake in all_earthquake_features:
            # Check if earthquake already exists (based on time and location)
            existing = earthquakes_collection.find_one({
                "$and": [
                    {"time": {"$gte": earthquake["time"] - timedelta(minutes=5), 
                              "$lte": earthquake["time"] + timedelta(minutes=5)}},
                    {"latitude": {"$gte": earthquake["latitude"] - 0.1, 
                                 "$lte": earthquake["latitude"] + 0.1}},
                    {"longitude": {"$gte": earthquake["longitude"] - 0.1, 
                                  "$lte": earthquake["longitude"] + 0.1}}
                ]
            })
            
            if existing:
                # Update existing record
                result = earthquakes_collection.update_one(
                    {"_id": existing["_id"]},
                    {"$set": earthquake}
                )
                if result.modified_count > 0:
                    updated_count += 1
                    logger.info(f"Updated earthquake: M{earthquake['magnitude']} at {earthquake['latitude']},{earthquake['longitude']}")
            else:
                # Insert new record
                earthquakes_collection.insert_one(earthquake)
                new_count += 1
                logger.info(f"Inserted new earthquake: M{earthquake['magnitude']} at {earthquake['latitude']},{earthquake['longitude']}")
        
        logger.info(f"Earthquake data update: {new_count} new records, {updated_count} updated records")
        return new_count + updated_count
        
    except Exception as e:
        logger.error(f"Error updating earthquake data: {str(e)}")
        return 0

# Function to update flood data in database
def update_flood_data(db):
    try:
        # Fetch flood data
        flood_data = fetch_flood_data()
        
        if not flood_data:
            logger.warning("No flood data fetched")
            return 0
        
        # Get the floods collection
        floods_collection = db['floods_data']
        
        # Track number of updates
        new_count = 0
        updated_count = 0
        
        # Update database
        for flood in flood_data:
            # Check if flood already exists (based on location and start date)
            existing = floods_collection.find_one({
                "state": flood["state"],
                "district": flood["district"],
                "start_date": flood["start_date"]
            })
            
            if existing:
                # Update existing record
                result = floods_collection.update_one(
                    {"_id": existing["_id"]},
                    {"$set": flood}
                )
                if result.modified_count > 0:
                    updated_count += 1
            else:
                # Insert new record
                floods_collection.insert_one(flood)
                new_count += 1
        
        logger.info(f"Flood data update: {new_count} new records, {updated_count} updated records")
        return new_count + updated_count
        
    except Exception as e:
        logger.error(f"Error updating flood data: {str(e)}")
        return 0

# Main function to update all data
def update_all_data():
    """
    Update all disaster data in the database
    """
    client, db = get_mongodb_client()
    
    if client is None:
        logger.error("Failed to get MongoDB client")
        return
    
    try:
        # Update earthquake data
        earthquake_updates = update_earthquake_data(db)
        logger.info(f"Completed earthquake data update: {earthquake_updates} records processed")
        
        # Update flood data
        flood_updates = update_flood_data(db)
        logger.info(f"Completed flood data update: {flood_updates} records processed")
        
    except Exception as e:
        logger.error(f"Error in update_all_data: {str(e)}")
    finally:
        client.close()

# Run as script
if __name__ == "__main__":
    update_all_data() 