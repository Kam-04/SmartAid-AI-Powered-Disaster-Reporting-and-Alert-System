from flask import Flask, request, jsonify
from flask_cors import CORS
from pymongo import MongoClient
from datetime import datetime, timedelta
import json
from bson import json_util, ObjectId
import random
import logging
from threading import Thread
import os
from real_time_data_fetcher import update_all_data
from ml_prediction_models import get_earthquake_prediction, get_flood_prediction, get_cyclone_prediction, INDIAN_STATES

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Connect to MongoDB Atlas
client = MongoClient("mongodb+srv://seismology:seismo-pass@cluster0.fdeuxad.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0", 
                    tls=True, 
                    tlsAllowInvalidCertificates=True)

# Specify your database and collections
db = client['disaster_management']
earthquakes_collection = db['earthquakes_data']
floods_collection = db['floods_data']
alerts_collection = db['alerts']
reports_collection = db['disaster_reports']
users_collection = db['users']
customer_location_collection = db['customer_location']
cyclones_collection = db['cyclones_data']
landslides_collection = db['landslides_data']
wildfires_collection = db['wildfires_data']

# Run initial earthquake data update
logger.info("Running initial earthquake data update...")
update_all_data()
logger.info("Initial earthquake data update completed")

# Sample data for initialization
def initialize_sample_data():
    try:
        # Check if collections are empty
        if users_collection.count_documents({}) == 0:
            # Sample users
            users = [
                {
                    "name": "John Doe",
                    "username": "john",
                    "email": "john@example.com",
                    "password": "password123",  # In real app, this would be hashed
                    "phone": "1234567890",
                    "location": "Chennai, Tamil Nadu",
                    "role": "user",
                    "created_at": datetime.now()
                },
                # ... existing sample users ...
            ]
            users_collection.insert_many(users)
            print("Sample users initialized")
            
        # Initialize sample cyclone data if collection is empty
        if cyclones_collection.count_documents({}) == 0:
            # Sample cyclone data
            cyclones = [
                {
                    "name": "Cyclone Amphan",
                    "affected_state": "West Bengal",
                    "affected_districts": ["Kolkata", "North 24 Parganas", "South 24 Parganas"],
                    "start_date": datetime(2020, 5, 16),
                    "end_date": datetime(2020, 5, 21),
                    "duration_days": 5,
                    "wind_speed": 185, # km/h
                    "pressure": 925, # hPa
                    "severity": "severe",
                    "deaths": 128,
                    "damage_estimate": 13.6, # Billion USD
                    "description": "Super Cyclonic Storm Amphan was a powerful and deadly tropical cyclone that caused widespread damage in Eastern India, especially West Bengal.",
                    "relief_measures": ["Evacuation of 4.3 million people", "Relief camps established", "Emergency supplies distributed"]
                },
                {
                    "name": "Cyclone Fani",
                    "affected_state": "Odisha",
                    "affected_districts": ["Puri", "Khordha", "Cuttack", "Bhubaneswar"],
                    "start_date": datetime(2019, 4, 26),
                    "end_date": datetime(2019, 5, 4),
                    "duration_days": 8,
                    "wind_speed": 175,
                    "pressure": 932,
                    "severity": "high",
                    "deaths": 89,
                    "damage_estimate": 8.1,
                    "description": "Extremely Severe Cyclonic Storm Fani was the strongest tropical cyclone to strike the Indian state of Odisha since Phailin in 2013.",
                    "relief_measures": ["Early evacuation of 1.2 million people", "Military assistance", "International aid"]
                },
                {
                    "name": "Cyclone Tauktae",
                    "affected_state": "Gujarat",
                    "affected_districts": ["Kutch", "Jamnagar", "Porbandar", "Dwarka"],
                    "start_date": datetime(2021, 5, 14),
                    "end_date": datetime(2021, 5, 19),
                    "duration_days": 5,
                    "wind_speed": 185,
                    "pressure": 950,
                    "severity": "high",
                    "deaths": 174,
                    "damage_estimate": 2.1,
                    "description": "Very Severe Cyclonic Storm Tauktae was a powerful tropical cyclone that caused significant damage in Gujarat.",
                    "relief_measures": ["NDRF teams deployed", "Emergency medical services", "Power restoration teams"]
                },
                {
                    "name": "Cyclone Yaas",
                    "affected_state": "Odisha",
                    "affected_districts": ["Balasore", "Bhadrak", "Kendrapara"],
                    "start_date": datetime(2021, 5, 23),
                    "end_date": datetime(2021, 5, 28),
                    "duration_days": 5,
                    "wind_speed": 140,
                    "pressure": 968,
                    "severity": "medium",
                    "deaths": 20,
                    "damage_estimate": 3.0,
                    "description": "Very Severe Cyclonic Storm Yaas was a relatively strong cyclone affecting Odisha and West Bengal.",
                    "relief_measures": ["Pre-emptive evacuation", "Relief camps", "Emergency supplies"]
                },
                {
                    "name": "Cyclone Nisarga",
                    "affected_state": "Maharashtra",
                    "affected_districts": ["Raigad", "Mumbai", "Thane"],
                    "start_date": datetime(2020, 6, 1),
                    "end_date": datetime(2020, 6, 4),
                    "duration_days": 3,
                    "wind_speed": 110,
                    "pressure": 982,
                    "severity": "medium",
                    "deaths": 6,
                    "damage_estimate": 0.8,
                    "description": "Severe Cyclonic Storm Nisarga was a cyclone that struck Maharashtra coast near Alibag.",
                    "relief_measures": ["Evacuation of coastal areas", "NDRF teams deployment"]
                },
                {
                    "name": "Cyclone Nivar",
                    "affected_state": "Tamil Nadu",
                    "affected_districts": ["Chennai", "Cuddalore", "Thanjavur"],
                    "start_date": datetime(2020, 11, 23),
                    "end_date": datetime(2020, 11, 27),
                    "duration_days": 4,
                    "wind_speed": 130,
                    "pressure": 972,
                    "severity": "medium",
                    "deaths": 12,
                    "damage_estimate": 0.6,
                    "description": "Very Severe Cyclonic Storm Nivar affected Tamil Nadu and brought heavy rainfall to Chennai.",
                    "relief_measures": ["Evacuation of low-lying areas", "Relief camps", "Food distribution"]
                },
                {
                    "name": "Cyclone Gaja",
                    "affected_state": "Tamil Nadu",
                    "affected_districts": ["Nagapattinam", "Thanjavur", "Pudukkottai", "Tiruvarur"],
                    "start_date": datetime(2018, 11, 10),
                    "end_date": datetime(2018, 11, 19),
                    "duration_days": 9,
                    "wind_speed": 120,
                    "pressure": 976,
                    "severity": "medium",
                    "deaths": 52,
                    "damage_estimate": 0.9,
                    "description": "Severe Cyclonic Storm Gaja caused extensive damage in Tamil Nadu and affected agricultural lands.",
                    "relief_measures": ["Restoration of power supply", "Compensation for damaged crops"]
                },
                {
                    "name": "Cyclone Titli",
                    "affected_state": "Andhra Pradesh",
                    "affected_districts": ["Srikakulam", "Vizianagaram"],
                    "start_date": datetime(2018, 10, 8),
                    "end_date": datetime(2018, 10, 15),
                    "duration_days": 7,
                    "wind_speed": 150,
                    "pressure": 970,
                    "severity": "high",
                    "deaths": 77,
                    "damage_estimate": 1.2,
                    "description": "Very Severe Cyclonic Storm Titli caused severe damage in Andhra Pradesh and Odisha.",
                    "relief_measures": ["Relief funds disbursed", "Medical camps", "Rehabilitation efforts"]
                },
                {
                    "name": "Cyclone Vardah",
                    "affected_state": "Tamil Nadu",
                    "affected_districts": ["Chennai", "Kanchipuram", "Tiruvallur"],
                    "start_date": datetime(2016, 12, 6),
                    "end_date": datetime(2016, 12, 13),
                    "duration_days": 7,
                    "wind_speed": 130,
                    "pressure": 975,
                    "severity": "medium",
                    "deaths": 38,
                    "damage_estimate": 1.0,
                    "description": "Very Severe Cyclonic Storm Vardah hit Chennai and caused significant urban damage.",
                    "relief_measures": ["Clearing of fallen trees", "Restoration of essential services"]
                },
                {
                    "name": "Cyclone Hudhud",
                    "affected_state": "Andhra Pradesh",
                    "affected_districts": ["Visakhapatnam", "Vizianagaram", "Srikakulam"],
                    "start_date": datetime(2014, 10, 7),
                    "end_date": datetime(2014, 10, 14),
                    "duration_days": 7,
                    "wind_speed": 175,
                    "pressure": 950,
                    "severity": "high",
                    "deaths": 124,
                    "damage_estimate": 3.4,
                    "description": "Very Severe Cyclonic Storm Hudhud caused extensive damage in Visakhapatnam.",
                    "relief_measures": ["Reconstruction of damaged infrastructure", "Rehabilitation packages"]
                }
            ]
            cyclones_collection.insert_many(cyclones)
            print("Sample cyclone data initialized")
            
        # Add more initializations for other collections as needed
        
    except Exception as e:
        print(f"Error in data initialization: {str(e)}")

# Helper function to parse MongoDB data to JSON
def parse_json(data):
    return json.loads(json_util.dumps(data))

# Basic Routes
@app.route('/')
def home():
    return jsonify({
        'name': 'Disaster Management API',
        'status': 'online',
        'timestamp': datetime.now().isoformat()
    })

@app.route('/api/status')
def api_status():
    connected = True
    try:
        # Verify database connection is working
        client.admin.command('ping')
    except Exception as e:
        connected = False
        
    return jsonify({
        'status': 'online',
        'db_connected': connected,
        'version': '1.0',
        'timestamp': datetime.now().isoformat(),
        'collections': {
            'earthquakes': earthquakes_collection.count_documents({}),
            'floods': floods_collection.count_documents({}),
            'alerts': alerts_collection.count_documents({}),
            'reports': reports_collection.count_documents({}),
            'users': users_collection.count_documents({})
        }
    })

# Monitoring Dashboard Routes
@app.route('/api/monitoring/dashboard')
def dashboard_data():
    try:
        # Get earthquake data - safely handle if collection is empty
        recent_earthquakes = list(earthquakes_collection.find().sort('time', -1).limit(5))
        
        # Get recent flood data
        recent_floods = list(floods_collection.find().sort('data_updated_at', -1).limit(5))
        
        # Safely get active alerts count
        active_alerts_count = alerts_collection.count_documents({'active': True})
        
        # Safely get affected regions
        affected_regions = list(earthquakes_collection.distinct('region'))
        if not affected_regions and recent_earthquakes:
            # Extract regions from places if region field not available
            affected_regions = list(set([quake.get('place', '').split(', ')[-1] for quake in recent_earthquakes if 'place' in quake]))
        
        # If still no regions, provide dummy data
        if not affected_regions:
            affected_regions = ["Maharashtra", "Kerala", "Tamil Nadu", "Gujarat"]
            
        return jsonify({
            'recent_earthquakes': parse_json(recent_earthquakes),
            'recent_floods': parse_json(recent_floods),
            'active_alerts': active_alerts_count,
            'affected_regions': affected_regions,
            'statistics': {
                'total_earthquakes': earthquakes_collection.count_documents({}),
                'total_floods': floods_collection.count_documents({}),
                'reports_submitted': reports_collection.count_documents({}),
                'registered_users': users_collection.count_documents({})
            }
        })
    except Exception as e:
        print(f"Error in dashboard data: {str(e)}")
        return jsonify({
            'error': str(e),
            'message': 'Error retrieving dashboard data'
        }), 500

@app.route('/api/monitoring/map')
def map_data():
    try:
        disaster_type = request.args.get('type', 'earthquake')
        state = request.args.get('state', None)
        
        query = {}
        if state:
            query['region'] = state
        
        events = []
        if disaster_type == 'earthquake':
            # Get earthquake data
            events = list(earthquakes_collection.find(query).sort('time', -1).limit(100))
        elif disaster_type == 'flood':
            # Get flood data
            events = list(floods_collection.find(query).sort('data_updated_at', -1).limit(100))
            
        return jsonify({
            'disaster_type': disaster_type,
            'state': state,
            'events': parse_json(events)
        })
    except Exception as e:
        return jsonify({
            'error': str(e),
            'message': 'Error retrieving map data'
        }), 500

# Emergency Routes
@app.route('/api/emergency/sos', methods=['POST'])
def create_sos():
    try:
        sos_data = request.json
        sos_data['created_at'] = datetime.now()
        sos_data['status'] = 'active'
        sos_data['active'] = True
        
        result = alerts_collection.insert_one(sos_data)
        
        return jsonify({
            'success': True,
            'alert_id': str(result.inserted_id),
            'message': 'SOS alert created successfully'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to create SOS alert'
        }), 500

@app.route('/api/emergency/sos/<alert_id>')
def get_sos_alert(alert_id):
    try:
        alert = alerts_collection.find_one({'_id': ObjectId(alert_id)})
        if alert:
            return jsonify({
                'success': True,
                'alert': parse_json(alert)
            })
        else:
            return jsonify({
                'success': False,
                'message': 'Alert not found'
            }), 404
    except Exception as e:
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

# SOS Location Sharing Routes
@app.route('/api/emergency/sos/location', methods=['POST'])
def share_sos_location():
    try:
        location_data = request.json
        
        # Required fields
        required_fields = ['user_id', 'latitude', 'longitude']
        for field in required_fields:
            if field not in location_data:
                return jsonify({
                    'success': False,
                    'message': f'Missing required field: {field}'
                }), 400
        
        # Add timestamp and status
        location_data['created_at'] = datetime.now()
        location_data['updated_at'] = datetime.now()
        location_data['active'] = True
        
        # Prepare coordinates for GeoJSON format
        location_data['location'] = {
            'type': 'Point',
            'coordinates': [location_data['longitude'], location_data['latitude']]
        }
        
        # Add optional address if provided
        if 'address' not in location_data:
            location_data['address'] = 'Unknown location'
            
        # Save to database
        result = customer_location_collection.insert_one(location_data)
        
        # Also create an SOS alert
        sos_data = {
            'user_id': location_data['user_id'],
            'location': {'lat': location_data['latitude'], 'lng': location_data['longitude']},
            'disaster_type': location_data.get('disaster_type', 'unknown'),
            'message': location_data.get('message', 'Emergency SOS location shared'),
            'created_at': datetime.now(),
            'location_id': str(result.inserted_id)
        }
        
        alert_result = alerts_collection.insert_one(sos_data)
        
        return jsonify({
            'success': True,
            'location_id': str(result.inserted_id),
            'alert_id': str(alert_result.inserted_id),
            'message': 'SOS location shared successfully'
        })
    except Exception as e:
        logger.error(f"Error sharing SOS location: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to share SOS location'
        }), 500

@app.route('/api/emergency/sos/location/<location_id>', methods=['GET'])
def get_sos_location(location_id):
    try:
        location = customer_location_collection.find_one({'_id': ObjectId(location_id)})
        if location:
            return jsonify({
                'success': True,
                'location': parse_json(location)
            })
        else:
            return jsonify({
                'success': False,
                'message': 'Location not found'
            }), 404
    except Exception as e:
        logger.error(f"Error retrieving SOS location: {str(e)}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

@app.route('/api/emergency/sos/location/user/<user_id>', methods=['GET'])
def get_user_sos_locations(user_id):
    try:
        # Get active locations for the user
        locations = list(customer_location_collection.find(
            {'user_id': user_id, 'active': True}
        ).sort('updated_at', -1))
        
        return jsonify({
            'success': True,
            'locations': parse_json(locations)
        })
    except Exception as e:
        logger.error(f"Error retrieving user SOS locations: {str(e)}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

@app.route('/api/emergency/sos/location/<location_id>', methods=['PUT'])
def update_sos_location(location_id):
    try:
        # Get update data
        update_data = request.json
        
        # Don't allow changing user_id
        if 'user_id' in update_data:
            del update_data['user_id']
            
        # Update timestamp
        update_data['updated_at'] = datetime.now()
        
        # If coordinates are provided, update GeoJSON
        if 'latitude' in update_data and 'longitude' in update_data:
            update_data['location'] = {
                'type': 'Point',
                'coordinates': [update_data['longitude'], update_data['latitude']]
            }
        
        result = customer_location_collection.update_one(
            {'_id': ObjectId(location_id)},
            {'$set': update_data}
        )
        
        if result.modified_count:
            return jsonify({
                'success': True,
                'message': 'Location updated successfully'
            })
        else:
            return jsonify({
                'success': False,
                'message': 'Location not found or no changes made'
            }), 404
    except Exception as e:
        logger.error(f"Error updating SOS location: {str(e)}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

@app.route('/api/emergency/sos/location/<location_id>/end', methods=['PUT'])
def end_sos_location_sharing(location_id):
    try:
        result = customer_location_collection.update_one(
            {'_id': ObjectId(location_id)},
            {'$set': {
                'active': False,
                'updated_at': datetime.now()
            }}
        )
        
        if result.modified_count:
            # Also update related alert if exists
            alerts_collection.update_many(
                {'location_id': location_id},
                {'$set': {
                    'active': False,
                    'status': 'resolved'
                }}
            )
            
            return jsonify({
                'success': True,
                'message': 'Location sharing ended successfully'
            })
        else:
            return jsonify({
                'success': False,
                'message': 'Location not found or already inactive'
            }), 404
    except Exception as e:
        logger.error(f"Error ending SOS location sharing: {str(e)}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

# Disaster Reporting Routes
@app.route('/api/reporting/report', methods=['POST'])
def create_report():
    try:
        report_data = request.json
        
        # Add required fields if missing
        report_data['created_at'] = datetime.now()
        
        # Validate required fields
        required_fields = ['disaster_type', 'description', 'location', 'user_id']
        for field in required_fields:
            if field not in report_data:
                return jsonify({
                    'success': False,
                    'error': f'Missing required field: {field}',
                    'message': 'Failed to create disaster report - missing data'
                }), 400
        
        # Format location data if needed
        if 'location' in report_data and isinstance(report_data['location'], dict):
            # Already in correct format
            pass
        elif 'latitude' in report_data and 'longitude' in report_data:
            # Convert from separate fields
            report_data['location'] = {
                'lat': float(report_data['latitude']),
                'lng': float(report_data['longitude'])
            }
            # Remove the separate fields to avoid duplication
            if 'latitude' in report_data:
                del report_data['latitude']
            if 'longitude' in report_data:
                del report_data['longitude']
        else:
            # Default to Mumbai coordinates if location is missing
            report_data['location'] = {'lat': 19.0760, 'lng': 72.8777}
        
        # Initialize empty media arrays
        report_data['images'] = []
        report_data['videos'] = []
        
        # Insert the report into MongoDB
        result = reports_collection.insert_one(report_data)
        
        return jsonify({
            'success': True,
            'report_id': str(result.inserted_id),
            'message': 'Disaster report created successfully'
        })
    except Exception as e:
        logger.error(f"Error creating disaster report: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to create disaster report'
        }), 500

# New endpoint for uploading media to an existing report
@app.route('/api/reporting/report/<report_id>/media', methods=['POST'])
def upload_report_media(report_id):
    try:
        # Check if the report exists
        report = reports_collection.find_one({'_id': ObjectId(report_id)})
        if not report:
            return jsonify({
                'success': False,
                'error': 'Report not found',
                'message': 'The specified report does not exist'
            }), 404
        
        # Check if the request has a file part
        if 'file' not in request.files:
            return jsonify({
                'success': False,
                'error': 'No file part',
                'message': 'No file was uploaded'
            }), 400
            
        # Get the file from the request
        file = request.files['file']
        if file.filename == '':
            return jsonify({
                'success': False,
                'error': 'No selected file',
                'message': 'No file was selected'
            }), 400
            
        # Get the media type (image or video)
        media_type = request.form.get('type', 'image')
        if media_type not in ['image', 'video']:
            media_type = 'image'  # Default to image if not specified
            
        # Generate a unique filename
        filename = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{file.filename}"
        
        # Here you would typically:
        # 1. Save the file to disk or cloud storage
        # 2. Get a URL or path to the saved file
        
        # For this example, we'll simulate saving to storage and return a dummy URL
        # In a real application, you'd integrate with AWS S3, Google Cloud Storage, etc.
        file_url = f"https://storage.example.com/disaster-reports/{media_type}s/{filename}"
        
        # Update the report with the new media file
        update_field = 'images' if media_type == 'image' else 'videos'
        reports_collection.update_one(
            {'_id': ObjectId(report_id)},
            {'$push': {update_field: file_url}}
        )
        
        return jsonify({
            'success': True,
            'report_id': report_id,
            'media_type': media_type,
            'file_url': file_url,
            'message': f'Successfully uploaded {media_type}'
        })
    except Exception as e:
        logger.error(f"Error uploading media to report: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': f'Failed to upload {media_type}'
        }), 500

@app.route('/api/reporting/reports')
def list_reports():
    try:
        status = request.args.get('status')
        disaster_type = request.args.get('disaster_type')
        verified = request.args.get('verified')
        
        query = {}
        if status:
            query['status'] = status
        if disaster_type:
            query['disaster_type'] = disaster_type
        if verified is not None:
            query['verified'] = verified.lower() == 'true'
        
        reports = list(reports_collection.find(query).sort('created_at', -1))
        
        return jsonify({
            'success': True,
            'reports': parse_json(reports)
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to retrieve reports'
        }), 500

# New Route for Real-time Data
@app.route('/api/realtime/disasters')
def get_realtime_disasters():
    try:
        disaster_type = request.args.get('type')
        state = request.args.get('state')
        days = int(request.args.get('days', 30))
        
        # Calculate time filter
        start_date = datetime.now() - timedelta(days=days)
        
        # Base query
        query = {"time": {"$gte": start_date}}
        
        # Add filters if provided
        if state:
            query["region"] = state
            
        # Determine which collection to query
        if disaster_type == 'flood':
            disasters = list(floods_collection.find(query).sort('data_updated_at', -1))
        else:  # Default to earthquakes
            disasters = list(earthquakes_collection.find(query).sort('time', -1))
            
        # Add source information
        result = {
            'source': 'USGS and National Center for Seismology (NCS) India',
            'last_updated': datetime.now().isoformat(),
            'filter': {
                'disaster_type': disaster_type or 'earthquake',
                'state': state,
                'days': days
            },
            'count': len(disasters),
            'disasters': parse_json(disasters)
        }
        
        return jsonify(result)
    except Exception as e:
        logger.error(f"Error in get_realtime_disasters: {str(e)}")
        return jsonify({
            'error': str(e),
            'message': 'Error retrieving real-time disaster data'
        }), 500

@app.route('/api/realtime/update', methods=['POST'])
def trigger_data_update():
    """
    Manually trigger a real-time data update
    Requires admin authentication (in a real app)
    """
    try:
        # Start update in a separate thread to not block the request
        update_thread = Thread(target=update_all_data)
        update_thread.daemon = True
        update_thread.start()
        
        return jsonify({
            'success': True,
            'message': 'Data update initiated',
            'time': datetime.now().isoformat()
        })
    except Exception as e:
        logger.error(f"Error triggering data update: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to initiate data update'
        }), 500

# AI Analysis Routes
@app.route('/api/analysis/historical/earthquake')
def historical_earthquake_data():
    try:
        state = request.args.get('state')
        days = int(request.args.get('days', 365))
        
        # Calculate time filter
        start_date = datetime.now() - timedelta(days=days)
        
        # Base query with time filter
        query = {"time": {"$gte": start_date}}
        
        if state:
            query['region'] = state
        
        # Get historical earthquake data
        historical_data = list(earthquakes_collection.find(query).sort('time', -1))
        
        # Calculate statistics
        total = len(historical_data)
        magnitudes = [quake.get('magnitude', 0) for quake in historical_data if 'magnitude' in quake]
        avg_magnitude = sum(magnitudes) / len(magnitudes) if magnitudes else 0
        
        # Calculate magnitude distribution
        magnitude_ranges = {
            'minor': len([m for m in magnitudes if m < 4.0]),
            'light': len([m for m in magnitudes if 4.0 <= m < 5.0]),
            'moderate': len([m for m in magnitudes if 5.0 <= m < 6.0]),
            'strong': len([m for m in magnitudes if 6.0 <= m < 7.0]),
            'major': len([m for m in magnitudes if 7.0 <= m < 8.0]),
            'great': len([m for m in magnitudes if m >= 8.0])
        }
        
        # Get top affected areas
        if state:
            affected_areas = list(set([quake.get('location', '').split(' of ')[-1] for quake in historical_data if 'location' in quake]))
        else:
            affected_areas = list(earthquakes_collection.distinct('region'))
            
        return jsonify({
            'success': True,
            'state': state,
            'days': days,
            'total_earthquakes': total,
            'average_magnitude': avg_magnitude,
            'magnitude_distribution': magnitude_ranges,
            'affected_areas': affected_areas,
            'earthquakes': parse_json(historical_data)
        })
    except Exception as e:
        print(f"Error in historical earthquake data: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to retrieve historical earthquake data'
        }), 500

# New Route for Flood Analysis
@app.route('/api/analysis/historical/flood')
def historical_flood_data():
    try:
        state = request.args.get('state')
        days = int(request.args.get('days', 365))
        
        # Calculate time filter
        start_date = datetime.now() - timedelta(days=days)
        
        # Base query with time filter
        query = {"start_date": {"$gte": start_date}}
        
        if state:
            query['state'] = state
        
        # Get historical flood data
        historical_data = list(floods_collection.find(query).sort('start_date', -1))
        
        # Calculate statistics
        total = len(historical_data)
        
        # Calculate severity distribution
        severity_distribution = {
            'low': len([f for f in historical_data if f.get('severity') == 'low']),
            'medium': len([f for f in historical_data if f.get('severity') == 'medium']),
            'high': len([f for f in historical_data if f.get('severity') == 'high'])
        }
        
        # Get affected states
        affected_states = list(floods_collection.distinct('state'))
        
        # Calculate total affected population
        total_affected = sum([f.get('affected_population', 0) for f in historical_data])
        
        return jsonify({
            'success': True,
            'state': state,
            'days': days,
            'total_floods': total,
            'total_affected_population': total_affected,
            'severity_distribution': severity_distribution,
            'affected_states': affected_states,
            'floods': parse_json(historical_data)
        })
    except Exception as e:
        logger.error(f"Error in historical flood data: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to retrieve historical flood data'
        }), 500

# AI Analysis Routes - Prediction
@app.route('/api/analysis/predict/earthquake', methods=['POST'])
def predict_earthquake():
    try:
        # Get prediction parameters from request
        prediction_data = request.json
        region = prediction_data.get('region', 'Unknown')
        
        # If region is not in our list of states, default to a known state
        if region not in INDIAN_STATES and region != 'Unknown':
            # Try to find closest match
            for state in INDIAN_STATES:
                if region.lower() in state.lower() or state.lower() in region.lower():
                    region = state
                    break
            else:
                # If no match found, default to a high-risk region
                region = 'Assam'
            
        # Get historical data for the region to inform the prediction
        query = {}
        if region != 'Unknown':
            query['$or'] = [
                {'region': region},
                {'place': {'$regex': region, '$options': 'i'}}
            ]
            
        # Get recent data from the last 90 days
        start_date = datetime.now() - timedelta(days=90)
        query['time'] = {"$gte": start_date}
            
        historical_data = list(earthquakes_collection.find(query).sort('time', -1))
        
        # If not enough data for this region, get data from neighboring regions or all data
        if len(historical_data) < 5:
            # Get data from all regions
            all_historical_data = list(earthquakes_collection.find({'time': {"$gte": start_date}}).sort('time', -1))
            if all_historical_data:
                historical_data = all_historical_data
        
        # Use ML model to generate prediction
        prediction_result = get_earthquake_prediction(region, historical_data)
        
        # Extract prediction values
        probability = prediction_result.get('probability', 0.5)
        predicted_magnitude = prediction_result.get('magnitude', 5.0)
        confidence = prediction_result.get('confidence', 0.6)
        
        # Determine risk level based on predicted magnitude
        risk_level = "Low"
        if predicted_magnitude >= 7.0:
            risk_level = "Extreme"
        elif predicted_magnitude >= 6.0:
            risk_level = "High"
        elif predicted_magnitude >= 5.0:
            risk_level = "Medium"
            
        # Generate affected areas
        affected_areas = {}
        # Add surrounding states as potentially affected
        for state in INDIAN_STATES:
            if state == region:
                affected_areas[state] = round(random.uniform(0.7, 0.9), 2)
            elif prediction_result.get('risk_factor', 0.5) > 0.7 or predicted_magnitude > 6.5:
                # For high risk or high magnitude, more areas are affected
                affected_areas[state] = round(random.uniform(0.1, 0.6), 2)
                if len(affected_areas) >= 5:  # Limit to 5 areas
                    break
            
        # Generate time frame based on probability
        if probability > 0.7:
            time_frame = "1-3 months"
        elif probability > 0.5:
            time_frame = "3-6 months"
        else:
            time_frame = "6-12 months"
            
        # Generate recommendations based on risk level
        recommendations = [
            "Ensure emergency kits are ready",
            "Stay informed through official channels"
        ]
        
        if risk_level == "Medium":
            recommendations.extend([
                "Review evacuation plans",
                "Secure heavy furniture to walls"
            ])
        elif risk_level in ["High", "Extreme"]:
            recommendations.extend([
                "Follow evacuation orders immediately",
                "Prepare for extended utility outages",
                "Implement structural reinforcements where possible",
                "Establish community support networks"
            ])
            
        # Generate historical context
        historicals = []
        if len(historical_data) > 2:
            for i in range(min(3, len(historical_data))):
                quake = historical_data[i]
                historicals.append(f"{quake.get('magnitude', 0)} magnitude earthquake in {quake.get('location', 'Unknown')}")
                
        historical_context = f"Recent seismic activity includes: {', '.join(historicals)}" if historicals else "Limited historical data available for this region."
            
        return jsonify({
            'success': True,
            'probability': probability,
            'magnitude': predicted_magnitude,
            'region': region,
            'confidence': confidence,
            'time_frame': time_frame,
            'risk_level': risk_level,
            'affected_areas': affected_areas,
            'historical_context': historical_context,
            'recommendations': recommendations,
            'data_source': 'Machine Learning model trained on real-time earthquake data from USGS and historical patterns'
        })
    except Exception as e:
        print(f"Error in earthquake prediction: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to generate earthquake prediction'
        }), 500

# Route for flood prediction
@app.route('/api/analysis/predict/flood', methods=['POST'])
def predict_flood():
    try:
        # Get prediction parameters from request
        prediction_data = request.json
        state = prediction_data.get('state', 'Unknown')
        district = prediction_data.get('district')
        
        # If state is not in our list of states, default to a known state
        if state not in INDIAN_STATES and state != 'Unknown':
            # Try to find closest match
            for s in INDIAN_STATES:
                if state.lower() in s.lower() or s.lower() in state.lower():
                    state = s
                    break
            else:
                # If no match found, default to a high-risk region
                state = 'Kerala'
        
        # Base query
        query = {}
        if state != 'Unknown':
            query['state'] = state
        if district:
            query['district'] = district
            
        # Get recent flood data
        start_date = datetime.now() - timedelta(days=180)  # Last 6 months
        if 'start_date' in query:
            query['start_date']['$gte'] = start_date
        else:
            query['start_date'] = {"$gte": start_date}
        
        historical_data = list(floods_collection.find(query).sort('start_date', -1))
        
        # If not enough data for this state, get data from all states
        if len(historical_data) < 5:
            general_query = {'start_date': {"$gte": start_date}}
            all_historical_data = list(floods_collection.find(general_query).sort('start_date', -1))
            if all_historical_data:
                historical_data = all_historical_data
        
        # Check if rainfall forecast is provided
        rainfall_forecast = prediction_data.get('rainfall_forecast')
        
        # Use ML model to generate prediction
        prediction_result = get_flood_prediction(state, historical_data, rainfall_forecast)
        
        # Extract prediction values
        probability = prediction_result.get('probability', 0.5)
        predicted_rainfall = prediction_result.get('predicted_rainfall', 150)
        severity = prediction_result.get('severity', 'medium')
        
        # Set risk level based on severity
        risk_level_map = {'low': 'Low', 'medium': 'Medium', 'high': 'High'}
        risk_level = risk_level_map.get(severity, 'Medium')
        
        # Generate time frame based on probability
        if probability > 0.7:
            time_frame = "1-2 months"
        elif probability > 0.5:
            time_frame = "2-4 months"
        else:
            time_frame = "Next monsoon season"
        
        # Generate affected districts prediction
        affected_districts = {}
        
        if state != 'Unknown':
            # Use actual district data if available or generate reasonable values
            all_districts = list(floods_collection.find({"state": state}).distinct('district'))
            
            if all_districts and len(all_districts) > 0:
                for dist in all_districts:
                    if dist:  # Only include non-empty district names
                        affected_probability = 0
                        if severity == 'high':
                            affected_probability = round(random.uniform(0.6, 0.9), 2)
                        elif severity == 'medium':
                            affected_probability = round(random.uniform(0.4, 0.7), 2)
                        else:
                            affected_probability = round(random.uniform(0.2, 0.5), 2)
                        affected_districts[dist] = affected_probability
            
            # If no districts found, generate some placeholder names
            if not affected_districts:
                for i in range(3):
                    district_name = f"{state} District {i+1}"
                    if severity == 'high':
                        affected_probability = round(random.uniform(0.6, 0.9), 2)
                    elif severity == 'medium':
                        affected_probability = round(random.uniform(0.4, 0.7), 2)
                    else:
                        affected_probability = round(random.uniform(0.2, 0.5), 2)
                    affected_districts[district_name] = affected_probability
        else:
            # If no state specified, include high-risk states
            high_risk_states = [
                'Assam', 'Bihar', 'West Bengal', 'Uttar Pradesh', 
                'Kerala', 'Odisha', 'Gujarat', 'Maharashtra'
            ]
            for risky_state in high_risk_states[:5]:  # Take first 5
                if severity == 'high':
                    affected_probability = round(random.uniform(0.6, 0.9), 2)
                elif severity == 'medium':
                    affected_probability = round(random.uniform(0.4, 0.7), 2)
                else:
                    affected_probability = round(random.uniform(0.2, 0.5), 2)
                affected_districts[risky_state] = affected_probability
        
        # Generate recommendations
        recommendations = [
            "Monitor local weather forecasts",
            "Keep emergency contact information handy"
        ]
        
        if risk_level == "Medium":
            recommendations.extend([
                "Prepare emergency evacuation kit",
                "Identify evacuation routes",
                "Store drinking water and non-perishable food"
            ])
        elif risk_level == "High":
            recommendations.extend([
                "Elevate electrical appliances",
                "Move valuable items to higher floors",
                "Be ready to evacuate on short notice",
                "Prepare for power outages",
                "Stock up on medicines and essential supplies"
            ])
            
        return jsonify({
            'success': True,
            'state': state,
            'district': district,
            'probability': probability,
            'predicted_rainfall': predicted_rainfall,
            'risk_level': risk_level,
            'time_frame': time_frame,
            'affected_areas': affected_districts,
            'recommendations': recommendations,
            'historical_data_count': len(historical_data),
            'data_source': 'Machine Learning model trained on real-time flood data and rainfall patterns'
        })
    except Exception as e:
        logger.error(f"Error in flood prediction: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to generate flood prediction'
        }), 500

# Route for cyclone prediction
@app.route('/api/analysis/predict/cyclone', methods=['POST'])
def predict_cyclone():
    try:
        # Get prediction parameters from request
        prediction_data = request.json
        state = prediction_data.get('state', 'Unknown')
        
        # If state is not in our list of states, default to a known state
        if state not in INDIAN_STATES and state != 'Unknown':
            # Try to find closest match
            for s in INDIAN_STATES:
                if state.lower() in s.lower() or s.lower() in state.lower():
                    state = s
                    break
            else:
                # If no match found, default to a high-risk region like Odisha
                state = 'Odisha'
        
        # Base query
        query = {}
        if state != 'Unknown':
            query['affected_state'] = state
            
        # Get recent cyclone data
        start_date = datetime.now() - timedelta(days=365)  # Last year
        if 'start_date' in query:
            query['start_date']['$gte'] = start_date
        else:
            query['start_date'] = {"$gte": start_date}
        
        # Get historical data from the database
        historical_data = []
        if 'cyclones_collection' in globals():
            historical_data = list(cyclones_collection.find(query).sort('start_date', -1))
        
        # If not enough data for this state, get data from all states
        if len(historical_data) < 5:
            general_query = {'start_date': {"$gte": start_date}} if 'start_date' in globals() else {}
            if 'cyclones_collection' in globals():
                all_historical_data = list(cyclones_collection.find(general_query).sort('start_date', -1))
                if all_historical_data:
                    historical_data = all_historical_data
        
        # Use ML model to generate prediction
        prediction_result = get_cyclone_prediction(state, historical_data)
        
        # Extract prediction values
        probability = prediction_result.get('probability', 0.5)
        wind_speed = prediction_result.get('wind_speed', 120)
        pressure = prediction_result.get('pressure', 980)
        severity = prediction_result.get('severity', 'medium')
        is_cyclone_season = prediction_result.get('is_cyclone_season', False)
        
        # Set risk level based on severity
        risk_level_map = {'low': 'Low', 'medium': 'Medium', 'high': 'High', 'severe': 'Very High', 'catastrophic': 'Extreme'}
        risk_level = risk_level_map.get(severity, 'Medium')
        
        # Generate time frame based on probability and season
        if is_cyclone_season:
            if probability > 0.7:
                time_frame = "1-2 months"
            else:
                time_frame = "2-3 months"
        else:
            if probability > 0.6:
                time_frame = "During next cyclone season"
            else:
                time_frame = "6-12 months"
        
        # Generate affected districts prediction
        affected_areas = {}
        
        if state != 'Unknown':
            # Use actual district data if available or generate reasonable values
            if 'cyclones_collection' in globals():
                all_districts = list(cyclones_collection.find({"affected_state": state}).distinct('affected_districts'))
                flattened_districts = []
                for dist_list in all_districts:
                    if isinstance(dist_list, list):
                        flattened_districts.extend(dist_list)
                    elif dist_list:
                        flattened_districts.append(dist_list)
                
                unique_districts = list(set(flattened_districts))
                
                if unique_districts and len(unique_districts) > 0:
                    for dist in unique_districts[:5]:  # Limit to 5 districts
                        if dist:  # Only include non-empty district names
                            affected_probability = 0
                            if severity in ['catastrophic', 'severe']:
                                affected_probability = round(random.uniform(0.7, 0.95), 2)
                            elif severity == 'high':
                                affected_probability = round(random.uniform(0.6, 0.8), 2)
                            elif severity == 'medium':
                                affected_probability = round(random.uniform(0.4, 0.6), 2)
                            else:
                                affected_probability = round(random.uniform(0.2, 0.4), 2)
                            affected_areas[dist] = affected_probability
            
            # If no districts found, generate some placeholder names
            if not affected_areas:
                for i in range(3):
                    district_name = f"{state} Coastal District {i+1}"
                    if severity in ['catastrophic', 'severe']:
                        affected_probability = round(random.uniform(0.7, 0.95), 2)
                    elif severity == 'high':
                        affected_probability = round(random.uniform(0.6, 0.8), 2)
                    elif severity == 'medium':
                        affected_probability = round(random.uniform(0.4, 0.6), 2)
                    else:
                        affected_probability = round(random.uniform(0.2, 0.4), 2)
                    affected_areas[district_name] = affected_probability
        else:
            # If no state specified, include high-risk coastal states
            high_risk_states = [
                'Odisha', 'West Bengal', 'Andhra Pradesh', 
                'Tamil Nadu', 'Gujarat', 'Kerala'
            ]
            for risky_state in high_risk_states[:5]:  # Take first 5
                if severity in ['catastrophic', 'severe']:
                    affected_probability = round(random.uniform(0.7, 0.95), 2)
                elif severity == 'high':
                    affected_probability = round(random.uniform(0.6, 0.8), 2)
                elif severity == 'medium':
                    affected_probability = round(random.uniform(0.4, 0.6), 2)
                else:
                    affected_probability = round(random.uniform(0.2, 0.4), 2)
                affected_areas[risky_state] = affected_probability
        
        # Generate recommendations
        recommendations = [
            "Monitor weather forecasts regularly",
            "Keep emergency contact information accessible"
        ]
        
        if risk_level in ["Medium", "High"]:
            recommendations.extend([
                "Prepare emergency kit with essentials",
                "Secure loose items outside your home",
                "Know your evacuation route",
                "Stock up on non-perishable food and water"
            ])
        elif risk_level in ["Very High", "Extreme"]:
            recommendations.extend([
                "Follow evacuation orders immediately",
                "Reinforce doors and windows",
                "Move valuable items to higher floors",
                "Prepare for extended power outages",
                "Stay away from coastal areas during warning period",
                "Ensure vehicles have full fuel tanks"
            ])
            
        # Format cyclone information
        cyclone_class = "Undefined"
        if wind_speed >= 222:
            cyclone_class = "Super Cyclonic Storm"
        elif wind_speed >= 166:
            cyclone_class = "Very Severe Cyclonic Storm"
        elif wind_speed >= 118:
            cyclone_class = "Severe Cyclonic Storm"
        elif wind_speed >= 88:
            cyclone_class = "Cyclonic Storm"
        elif wind_speed >= 62:
            cyclone_class = "Deep Depression"
        else:
            cyclone_class = "Depression"
            
        return jsonify({
            'success': True,
            'state': state,
            'probability': probability,
            'wind_speed': wind_speed,
            'pressure': pressure,
            'cyclone_class': cyclone_class,
            'risk_level': risk_level,
            'time_frame': time_frame,
            'affected_areas': affected_areas,
            'recommendations': recommendations,
            'is_cyclone_season': is_cyclone_season,
            'data_source': 'Machine Learning model trained on historical cyclone data and seasonal patterns'
        })
    except Exception as e:
        logger.error(f"Error in cyclone prediction: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to generate cyclone prediction'
        }), 500

# AI Predictions Dashboard route
@app.route('/api/analysis/predictions/dashboard', methods=['GET'])
def get_predictions_dashboard():
    try:
        # Get a subset of states to analyze (major states with disaster risks)
        target_states = [
            # Earthquake-prone states
            'Jammu and Kashmir', 'Himachal Pradesh', 'Uttarakhand', 'Sikkim', 'Gujarat', 
            # Flood-prone states
            'Assam', 'Bihar', 'West Bengal', 'Kerala', 'Odisha',
            # Cyclone-prone states
            'Tamil Nadu', 'Andhra Pradesh', 'Odisha', 'West Bengal', 'Gujarat'
        ]
        # Remove duplicates
        target_states = list(set(target_states))
        
        # Create predictions for each state
        predictions = {}
        
        # Get historical data with reasonable limits to avoid overloading
        earthquake_data = list(earthquakes_collection.find().sort('time', -1).limit(500))
        flood_data = list(floods_collection.find().sort('start_date', -1).limit(500))
        cyclone_data = list(cyclones_collection.find().sort('start_date', -1).limit(500))
        
        for state in target_states:
            state_predictions = {}
            
            # Generate earthquake prediction
            earthquake_pred = get_earthquake_prediction(state, earthquake_data)
            state_predictions['earthquake'] = {
                'probability': round(earthquake_pred.get('probability', 0) * 100),  # Convert to percentage
                'magnitude': round(earthquake_pred.get('magnitude', 0), 1),
                'risk_level': "High" if earthquake_pred.get('magnitude', 0) >= 6.0 else 
                              "Medium" if earthquake_pred.get('magnitude', 0) >= 5.0 else "Low"
            }
            
            # Generate flood prediction
            flood_pred = get_flood_prediction(state, flood_data)
            state_predictions['flood'] = {
                'probability': round(flood_pred.get('probability', 0) * 100),  # Convert to percentage
                'predicted_rainfall': round(flood_pred.get('predicted_rainfall', 0)),
                'risk_level': {'low': 'Low', 'medium': 'Medium', 'high': 'High'}.get(
                    flood_pred.get('severity', 'low'), 'Low')
            }
            
            # Generate cyclone prediction
            cyclone_pred = get_cyclone_prediction(state, cyclone_data)
            state_predictions['cyclone'] = {
                'probability': round(cyclone_pred.get('probability', 0) * 100),  # Convert to percentage
                'wind_speed': round(cyclone_pred.get('wind_speed', 0)),
                'risk_level': {'low': 'Low', 'medium': 'Medium', 'high': 'High', 
                              'severe': 'Very High', 'catastrophic': 'Extreme'}.get(
                    cyclone_pred.get('severity', 'low'), 'Low')
            }
            
            # Find the highest risk disaster type for this state
            highest_risk = max(['earthquake', 'flood', 'cyclone'], 
                              key=lambda disaster_type: state_predictions[disaster_type]['probability'])
            
            # Determine overall risk level
            risk_levels = {
                'earthquake': state_predictions['earthquake']['risk_level'],
                'flood': state_predictions['flood']['risk_level'],
                'cyclone': state_predictions['cyclone']['risk_level']
            }
            
            # Map risk levels to numeric values
            risk_map = {'Low': 1, 'Medium': 2, 'High': 3, 'Very High': 4, 'Extreme': 5}
            max_risk_level = max(risk_levels.values(), key=lambda x: risk_map.get(x, 0))
            
            # Add overall assessment
            state_predictions['overall'] = {
                'highest_risk_type': highest_risk,
                'risk_level': max_risk_level,
                'summary': f"{max_risk_level} risk of {highest_risk} in {state}"
            }
            
            predictions[state] = state_predictions
        
        # Get top risk states for each disaster type
        top_earthquake_states = sorted(
            [(state, predictions[state]['earthquake']['probability']) for state in predictions],
            key=lambda x: x[1], reverse=True
        )[:5]
        
        top_flood_states = sorted(
            [(state, predictions[state]['flood']['probability']) for state in predictions],
            key=lambda x: x[1], reverse=True
        )[:5]
        
        top_cyclone_states = sorted(
            [(state, predictions[state]['cyclone']['probability']) for state in predictions],
            key=lambda x: x[1], reverse=True
        )[:5]
        
        # Format for dashboard
        dashboard_data = {
            'states_data': predictions,
            'top_risks': {
                'earthquake': [{'state': state, 'probability': prob} for state, prob in top_earthquake_states],
                'flood': [{'state': state, 'probability': prob} for state, prob in top_flood_states],
                'cyclone': [{'state': state, 'probability': prob} for state, prob in top_cyclone_states]
            },
            'meta': {
                'generated_at': datetime.now(),
                'model_type': 'Machine Learning Ensemble',
                'prediction_timeframe': '6 months'
            }
        }
        
        return jsonify({
            'success': True,
            'data': dashboard_data
        })
        
    except Exception as e:
        logger.error(f"Error generating predictions dashboard: {str(e)}")
        return jsonify({
            'success': False,
            'error': str(e),
            'message': 'Failed to generate predictions dashboard'
        }), 500

if __name__ == '__main__':
    # Initialize sample data
    initialize_sample_data()
    
    # Start initial data fetch in background
    try:
        logger.info("Starting initial data fetch in background...")
        update_thread = Thread(target=update_all_data)
        update_thread.daemon = True
        update_thread.start()
        logger.info("Background data fetch thread started successfully")
    except Exception as e:
        logger.error(f"Error starting background data fetch: {str(e)}")
    
    # Start the Flask app
    app.run(host='0.0.0.0', debug=True, port=5000) 