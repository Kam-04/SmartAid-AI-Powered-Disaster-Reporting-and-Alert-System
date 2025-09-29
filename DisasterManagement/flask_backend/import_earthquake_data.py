from pymongo import MongoClient
import datetime
import re

# Connect to MongoDB Atlas
client = MongoClient("mongodb+srv://seismology:seismo-pass@cluster0.fdeuxad.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0", 
                    tls=True, 
                    tlsAllowInvalidCertificates=True)

# Specify your database and collection
db = client['seismology_db']
earthquakes_collection = db['earthquakes_data']

# Sample data to import
sample_data = [
    {
        "origin_time": "2013-04-16 08:36:40 IST",
        "latitude": 19.96,
        "longitude": 86.42,
        "depth": 10,
        "magnitude": 3.5,
        "magnitude_type": "ML",
        "location": "63km ENE of Puri, Odisha, India",
        "region": "Odisha"
    },
    {
        "origin_time": "2013-04-16 14:04:12 IST",
        "latitude": 28.99,
        "longitude": 95.23,
        "depth": 40,
        "magnitude": 5.1,
        "magnitude_type": "ML",
        "location": "94km N of Pangin, Arunachal Pradesh, India",
        "region": "Arunachal Pradesh"
    },
    {
        "origin_time": "2013-04-16 16:14:18 IST",
        "latitude": 28.6,
        "longitude": 62.13,
        "depth": 66,
        "magnitude": 7.2,
        "magnitude_type": "MS",
        "location": "671km NNE of Muscat, Oman",
        "region": "Oman"
    },
    {
        "origin_time": "2013-04-16 21:11:12 IST",
        "latitude": 25.68,
        "longitude": 90.87,
        "depth": 10,
        "magnitude": 3.2,
        "magnitude_type": "ML",
        "location": "69km ENE of Tura, Meghalaya, India",
        "region": "Meghalaya"
    },
    {
        "origin_time": "2013-04-17 07:15:58 IST",
        "latitude": 25.99,
        "longitude": 99.89,
        "depth": 38,
        "magnitude": 5.1,
        "magnitude_type": "ML",
        "location": "376km ESE of Changlang, Arunachal Pradesh, India",
        "region": "Arunachal Pradesh"
    },
    {
        "origin_time": "2013-04-17 08:31:36 IST",
        "latitude": 33.17,
        "longitude": 76.39,
        "depth": 10,
        "magnitude": 3.4,
        "magnitude_type": "ML",
        "location": "105km N of Dharamshala, Himachal Pradesh, India",
        "region": "Himachal Pradesh"
    }
]

# Convert string dates to datetime objects and parse magnitude types
for eq in sample_data:
    # Parse the origin time
    time_str = eq["origin_time"]
    eq["time"] = datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S IST")
    
    # Extract magnitude type from the magnitude field if it exists
    magnitude_type = eq.get("magnitude_type", "")
    
    # Place is derived from the location for consistency with API
    eq["place"] = eq["location"]
    
    # Add a formatted location or region if needed
    region_match = re.search(r'([^,]+)(?:, India)?$', eq["location"])
    if region_match:
        eq["region"] = region_match.group(1).strip()

# Insert the data into MongoDB
if earthquakes_collection.count_documents({}) == 0:
    print("Inserting sample earthquake data...")
    result = earthquakes_collection.insert_many(sample_data)
    print(f"Inserted {len(result.inserted_ids)} documents")
else:
    print("Earthquake collection already has data. Skipping sample data insertion.")
    print(f"Current document count: {earthquakes_collection.count_documents({})}")

# Display the data in the collection
print("\nCurrent earthquake data in database:")
for doc in earthquakes_collection.find().sort("time", -1).limit(10):
    print(f"{doc.get('time')} - M{doc.get('magnitude')}{doc.get('magnitude_type', '')} - {doc.get('location')}")

print("\nImport completed successfully!") 