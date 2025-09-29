import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
import joblib
import os
import logging
from datetime import datetime, timedelta

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Define paths for saving models
MODEL_DIR = os.path.join(os.path.dirname(__file__), 'models')
if not os.path.exists(MODEL_DIR):
    os.makedirs(MODEL_DIR)

EARTHQUAKE_MODEL_PATH = os.path.join(MODEL_DIR, 'earthquake_model.joblib')
FLOOD_MODEL_PATH = os.path.join(MODEL_DIR, 'flood_model.joblib')

# Indian States and UTs
INDIAN_STATES = [
    'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 
    'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka', 
    'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram', 
    'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu', 
    'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
    'Andaman and Nicobar Islands', 'Chandigarh', 'Dadra and Nagar Haveli and Daman and Diu', 
    'Delhi', 'Jammu and Kashmir', 'Ladakh', 'Lakshadweep', 'Puducherry'
]

# Risk zones for earthquakes (based on seismic zones in India)
EARTHQUAKE_RISK_ZONES = {
    'Very High Risk': ['Jammu and Kashmir', 'Himachal Pradesh', 'Uttarakhand', 'Sikkim', 'Assam', 
                      'Arunachal Pradesh', 'Nagaland', 'Manipur', 'Mizoram', 'Tripura', 'Meghalaya', 'Andaman and Nicobar Islands'],
    'High Risk': ['Delhi', 'Bihar', 'West Bengal', 'Gujarat', 'Maharashtra'],
    'Moderate Risk': ['Rajasthan', 'Haryana', 'Uttar Pradesh', 'Madhya Pradesh', 'Jharkhand', 'Odisha'],
    'Low Risk': ['Punjab', 'Chandigarh', 'Goa', 'Karnataka', 'Andhra Pradesh', 'Telangana', 'Tamil Nadu', 
                'Kerala', 'Puducherry', 'Lakshadweep', 'Dadra and Nagar Haveli and Daman and Diu']
}

# Risk zones for floods (based on historical flood data in India)
FLOOD_RISK_ZONES = {
    'Very High Risk': ['Assam', 'Bihar', 'West Bengal', 'Uttar Pradesh', 'Kerala', 'Odisha'],
    'High Risk': ['Gujarat', 'Maharashtra', 'Tamil Nadu', 'Andhra Pradesh', 'Telangana', 'Karnataka'],
    'Moderate Risk': ['Madhya Pradesh', 'Jharkhand', 'Chhattisgarh', 'Punjab', 'Haryana', 'Rajasthan'],
    'Low Risk': ['Himachal Pradesh', 'Uttarakhand', 'Jammu and Kashmir', 'Goa', 'Sikkim', 
                'Manipur', 'Mizoram', 'Nagaland', 'Meghalaya', 'Tripura', 'Arunachal Pradesh']
}

# Risk zones for cyclones (based on historical data in India)
CYCLONE_RISK_ZONES = {
    'Very High Risk': ['Odisha', 'West Bengal', 'Andhra Pradesh', 'Tamil Nadu', 'Gujarat', 'Kerala'],
    'High Risk': ['Maharashtra', 'Karnataka', 'Goa', 'Puducherry', 'Andaman and Nicobar Islands'],
    'Moderate Risk': ['Telangana', 'Lakshadweep'],
    'Low Risk': [state for state in INDIAN_STATES if state not in 
                ['Odisha', 'West Bengal', 'Andhra Pradesh', 'Tamil Nadu', 'Gujarat', 'Kerala', 
                 'Maharashtra', 'Karnataka', 'Goa', 'Puducherry', 'Andaman and Nicobar Islands',
                 'Telangana', 'Lakshadweep']]
}

class EarthquakePredictionModel:
    def __init__(self):
        self.model = None
        self.scaler = StandardScaler()
        self.load_or_train_model()
    
    def prepare_data(self, data):
        """Convert MongoDB documents to pandas DataFrame for model training"""
        if not data:
            return None
            
        rows = []
        for quake in data:
            try:
                # Extract region/state
                region = quake.get('region', '')
                if not region:
                    region = quake.get('place', '').split(', ')[-1] if 'place' in quake else 'Unknown'
                
                # Extract features
                row = {
                    'magnitude': quake.get('magnitude', 0),
                    'depth': quake.get('depth', 0),
                    'month': quake.get('time', datetime.now()).month if 'time' in quake else datetime.now().month,
                    'year': quake.get('time', datetime.now()).year if 'time' in quake else datetime.now().year,
                    'region': region
                }
                
                # Get historical frequency for this region
                rows.append(row)
            except Exception as e:
                logger.error(f"Error processing earthquake data: {str(e)}")
                continue
                
        return pd.DataFrame(rows)
    
    def get_region_risk_factor(self, region):
        """Get seismic risk factor based on region"""
        if region in EARTHQUAKE_RISK_ZONES['Very High Risk']:
            return 0.9
        elif region in EARTHQUAKE_RISK_ZONES['High Risk']:
            return 0.7
        elif region in EARTHQUAKE_RISK_ZONES['Moderate Risk']:
            return 0.5
        elif region in EARTHQUAKE_RISK_ZONES['Low Risk']:
            return 0.3
        else:
            return 0.5  # Default to moderate risk
    
    def train_model(self, data):
        """Train RandomForest model for earthquake prediction"""
        df = self.prepare_data(data)
        if df is None or len(df) < 10:
            logger.warning("Insufficient data for training earthquake model")
            return False
            
        # Add risk factor based on region
        df['risk_factor'] = df['region'].apply(self.get_region_risk_factor)
        
        # Encode categorical variables
        df_encoded = pd.get_dummies(df, columns=['region'])
        
        # Define features and target
        X = df_encoded.drop(['magnitude'], axis=1)
        y = df_encoded['magnitude']
        
        # Train-test split
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        # Create and train model
        self.model = Pipeline([
            ('scaler', StandardScaler()),
            ('regressor', RandomForestRegressor(n_estimators=100, random_state=42))
        ])
        
        self.model.fit(X_train, y_train)
        
        # Save model
        joblib.dump(self.model, EARTHQUAKE_MODEL_PATH)
        logger.info("Earthquake prediction model trained and saved")
        return True
    
    def load_or_train_model(self):
        """Load existing model or train new one"""
        try:
            if os.path.exists(EARTHQUAKE_MODEL_PATH):
                self.model = joblib.load(EARTHQUAKE_MODEL_PATH)
                logger.info("Loaded existing earthquake model")
                return True
        except Exception as e:
            logger.error(f"Error loading earthquake model: {str(e)}")
        
        logger.info("No existing earthquake model found")
        return False
    
    def predict(self, region, historical_data):
        """Generate earthquake prediction for a region"""
        if self.model is None and not self.train_model(historical_data):
            # Fallback if model training fails
            return self._generate_rule_based_prediction(region, historical_data)
        
        try:
            # Create feature vector for prediction
            risk_factor = self.get_region_risk_factor(region)
            
            # Get recent earthquake stats for this region
            recent_quakes = [q for q in historical_data if 
                            (q.get('region', '') == region or 
                             (q.get('place', '').split(', ')[-1] if 'place' in q else '') == region)]
            
            recent_mags = [q.get('magnitude', 0) for q in recent_quakes if 'magnitude' in q]
            avg_magnitude = sum(recent_mags) / len(recent_mags) if recent_mags else 5.0
            
            # Create features dataframe with one-hot encoding for region
            features = pd.DataFrame({
                'depth': [15.0],  # Default depth
                'month': [datetime.now().month],
                'year': [datetime.now().year],
                'risk_factor': [risk_factor]
            })
            
            # Add one-hot encoding for region
            for state in INDIAN_STATES:
                features[f'region_{state}'] = [1 if state == region else 0]
            
            # Make prediction
            predicted_magnitude = self.model.predict(features)[0]
            
            # Calculate probability based on risk factor and recent activity
            quake_frequency = len(recent_quakes) / 90  # quakes per day in last 90 days
            probability = min(0.9, (risk_factor * 0.7) + (quake_frequency * 0.3))
            
            # Other prediction attributes
            confidence = 0.7  # Higher confidence with ML model
            
            return {
                'probability': probability,
                'magnitude': predicted_magnitude,
                'confidence': confidence,
                'risk_factor': risk_factor
            }
            
        except Exception as e:
            logger.error(f"Error making earthquake prediction: {str(e)}")
            # Fallback to rule-based prediction
            return self._generate_rule_based_prediction(region, historical_data)
    
    def _generate_rule_based_prediction(self, region, historical_data):
        """Rule-based fallback if ML prediction fails"""
        risk_factor = self.get_region_risk_factor(region)
        
        # Get recent magnitude average
        recent_quakes = [q for q in historical_data if 
                        (q.get('region', '') == region or 
                         (q.get('place', '').split(', ')[-1] if 'place' in q else '') == region)]
        
        recent_mags = [q.get('magnitude', 0) for q in recent_quakes if 'magnitude' in q]
        avg_magnitude = sum(recent_mags) / len(recent_mags) if recent_mags else 5.0
        
        # Adjust magnitude based on risk zone
        if region in EARTHQUAKE_RISK_ZONES['Very High Risk']:
            predicted_magnitude = avg_magnitude + 0.5
        elif region in EARTHQUAKE_RISK_ZONES['High Risk']:
            predicted_magnitude = avg_magnitude + 0.3
        elif region in EARTHQUAKE_RISK_ZONES['Moderate Risk']:
            predicted_magnitude = avg_magnitude + 0.1
        else:
            predicted_magnitude = max(4.0, avg_magnitude - 0.2)
        
        # Calculate probability based on risk factor
        probability = risk_factor
        
        return {
            'probability': probability,
            'magnitude': predicted_magnitude,
            'confidence': 0.5,  # Lower confidence for rule-based
            'risk_factor': risk_factor
        }

class FloodPredictionModel:
    def __init__(self):
        self.model = None
        self.scaler = StandardScaler()
        self.load_or_train_model()
    
    def prepare_data(self, data):
        """Convert MongoDB documents to pandas DataFrame for model training"""
        if not data:
            return None
            
        rows = []
        for flood in data:
            try:
                # Extract state
                state = flood.get('state', 'Unknown')
                
                # Extract features
                row = {
                    'severity': self._severity_to_numeric(flood.get('severity', 'low')),
                    'affected_area': flood.get('affected_area', 0),
                    'rainfall': flood.get('rainfall', 0),
                    'month': flood.get('start_date', datetime.now()).month if 'start_date' in flood else datetime.now().month,
                    'duration_days': flood.get('duration_days', 0),
                    'state': state
                }
                
                rows.append(row)
            except Exception as e:
                logger.error(f"Error processing flood data: {str(e)}")
                continue
                
        return pd.DataFrame(rows)
    
    def _severity_to_numeric(self, severity):
        """Convert severity string to numeric value"""
        severity_map = {'low': 1, 'medium': 2, 'high': 3}
        return severity_map.get(severity.lower(), 1)
    
    def _numeric_to_severity(self, value):
        """Convert numeric value to severity string"""
        if value >= 2.5:
            return 'high'
        elif value >= 1.5:
            return 'medium'
        else:
            return 'low'
    
    def get_region_risk_factor(self, region):
        """Get flood risk factor based on region"""
        if region in FLOOD_RISK_ZONES['Very High Risk']:
            return 0.9
        elif region in FLOOD_RISK_ZONES['High Risk']:
            return 0.7
        elif region in FLOOD_RISK_ZONES['Moderate Risk']:
            return 0.5
        elif region in FLOOD_RISK_ZONES['Low Risk']:
            return 0.3
        else:
            return 0.5  # Default to moderate risk
    
    def train_model(self, data):
        """Train GradientBoosting model for flood prediction"""
        df = self.prepare_data(data)
        if df is None or len(df) < 10:
            logger.warning("Insufficient data for training flood model")
            return False
            
        # Add risk factor based on state
        df['risk_factor'] = df['state'].apply(self.get_region_risk_factor)
        
        # Add monsoon indicator (June-September)
        df['is_monsoon'] = df['month'].apply(lambda m: 1 if 6 <= m <= 9 else 0)
        
        # Encode categorical variables
        df_encoded = pd.get_dummies(df, columns=['state'])
        
        # Define features and target
        X = df_encoded.drop(['severity'], axis=1)
        y = df_encoded['severity']
        
        # Train-test split
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        # Create and train model
        self.model = Pipeline([
            ('scaler', StandardScaler()),
            ('regressor', GradientBoostingRegressor(n_estimators=100, random_state=42))
        ])
        
        self.model.fit(X_train, y_train)
        
        # Save model
        joblib.dump(self.model, FLOOD_MODEL_PATH)
        logger.info("Flood prediction model trained and saved")
        return True
    
    def load_or_train_model(self):
        """Load existing model or train new one"""
        try:
            if os.path.exists(FLOOD_MODEL_PATH):
                self.model = joblib.load(FLOOD_MODEL_PATH)
                logger.info("Loaded existing flood model")
                return True
        except Exception as e:
            logger.error(f"Error loading flood model: {str(e)}")
        
        logger.info("No existing flood model found")
        return False
    
    def predict(self, state, historical_data, rainfall_forecast=None):
        """Generate flood prediction for a state"""
        if self.model is None and not self.train_model(historical_data):
            # Fallback if model training fails
            return self._generate_rule_based_prediction(state, historical_data, rainfall_forecast)
        
        try:
            # Create feature vector for prediction
            risk_factor = self.get_region_risk_factor(state)
            
            # Get current month for seasonality
            current_month = datetime.now().month
            is_monsoon = 1 if 6 <= current_month <= 9 else 0
            
            # Get recent flood stats for this state
            recent_floods = [f for f in historical_data if f.get('state', '') == state]
            
            # Use provided rainfall or estimate from historical data
            if rainfall_forecast is None:
                # Estimate from historical data during similar month
                similar_month_floods = [f for f in recent_floods if f.get('start_date', datetime.now()).month == current_month]
                rainfall_values = [f.get('rainfall', 0) for f in similar_month_floods]
                rainfall_forecast = sum(rainfall_values) / len(rainfall_values) if rainfall_values else 150
            
            # Get average affected area from historical data
            affected_areas = [f.get('affected_area', 0) for f in recent_floods]
            avg_affected_area = sum(affected_areas) / len(affected_areas) if affected_areas else 100
            
            # Get average duration from historical data
            durations = [f.get('duration_days', 0) for f in recent_floods]
            avg_duration = sum(durations) / len(durations) if durations else 7
            
            # Create features dataframe with one-hot encoding for state
            features = pd.DataFrame({
                'affected_area': [avg_affected_area],
                'rainfall': [rainfall_forecast],
                'month': [current_month],
                'duration_days': [avg_duration],
                'risk_factor': [risk_factor],
                'is_monsoon': [is_monsoon]
            })
            
            # Add one-hot encoding for state
            for s in INDIAN_STATES:
                features[f'state_{s}'] = [1 if s == state else 0]
            
            # Make prediction
            predicted_severity_numeric = self.model.predict(features)[0]
            predicted_severity = self._numeric_to_severity(predicted_severity_numeric)
            
            # Calculate probability based on risk factor, rainfall and seasonality
            base_probability = risk_factor
            rainfall_factor = min(1.0, rainfall_forecast / 300)  # Normalize rainfall (300mm is high)
            season_factor = 1.5 if is_monsoon else 0.7
            
            flood_probability = min(0.95, base_probability * rainfall_factor * season_factor)
            
            return {
                'probability': flood_probability,
                'predicted_rainfall': rainfall_forecast,
                'severity': predicted_severity,
                'confidence': 0.7,  # Higher confidence with ML model
                'risk_factor': risk_factor
            }
            
        except Exception as e:
            logger.error(f"Error making flood prediction: {str(e)}")
            # Fallback to rule-based prediction
            return self._generate_rule_based_prediction(state, historical_data, rainfall_forecast)
    
    def _generate_rule_based_prediction(self, state, historical_data, rainfall_forecast=None):
        """Rule-based fallback if ML prediction fails"""
        risk_factor = self.get_region_risk_factor(state)
        
        # Get current month for seasonality
        current_month = datetime.now().month
        is_monsoon = 6 <= current_month <= 9
        
        # Use provided rainfall or set based on month and risk
        if rainfall_forecast is None:
            if is_monsoon:
                if state in FLOOD_RISK_ZONES['Very High Risk']:
                    rainfall_forecast = 250
                elif state in FLOOD_RISK_ZONES['High Risk']:
                    rainfall_forecast = 200
                else:
                    rainfall_forecast = 150
            else:
                rainfall_forecast = 100
        
        # Determine severity based on risk zone and season
        if state in FLOOD_RISK_ZONES['Very High Risk'] and is_monsoon:
            severity = 'high'
        elif state in FLOOD_RISK_ZONES['Very High Risk'] or (state in FLOOD_RISK_ZONES['High Risk'] and is_monsoon):
            severity = 'medium'
        else:
            severity = 'low'
        
        # Calculate probability based on risk factor and season
        base_probability = risk_factor
        season_modifier = 1.5 if is_monsoon else 0.7
        flood_probability = min(0.9, base_probability * season_modifier)
        
        return {
            'probability': flood_probability,
            'predicted_rainfall': rainfall_forecast,
            'severity': severity,
            'confidence': 0.5,  # Lower confidence for rule-based
            'risk_factor': risk_factor
        }

class CyclonePredictionModel:
    def __init__(self):
        self.model = None
        self.scaler = StandardScaler()
        self.load_or_train_model()
    
    def prepare_data(self, data):
        """Convert MongoDB documents to pandas DataFrame for model training"""
        if not data:
            return None
            
        rows = []
        for cyclone in data:
            try:
                # Extract state
                state = cyclone.get('affected_state', 'Unknown')
                if state == 'Unknown':
                    continue  # Skip entries without state information
                
                # Extract features
                row = {
                    'wind_speed': cyclone.get('wind_speed', 0),
                    'pressure': cyclone.get('pressure', 1000),
                    'month': cyclone.get('start_date', datetime.now()).month if 'start_date' in cyclone else datetime.now().month,
                    'duration_days': cyclone.get('duration_days', 0),
                    'severity': self._severity_to_numeric(cyclone.get('severity', 'low')),
                    'state': state
                }
                
                rows.append(row)
            except Exception as e:
                logger.error(f"Error processing cyclone data: {str(e)}")
                continue
                
        return pd.DataFrame(rows)
    
    def _severity_to_numeric(self, severity):
        """Convert severity string to numeric value"""
        severity_map = {'low': 1, 'medium': 2, 'high': 3, 'severe': 4, 'catastrophic': 5}
        return severity_map.get(severity.lower(), 1)
    
    def _numeric_to_severity(self, value):
        """Convert numeric value to severity string"""
        if value >= 4:
            return 'catastrophic'
        elif value >= 3:
            return 'severe'
        elif value >= 2:
            return 'high'
        elif value >= 1.5:
            return 'medium'
        else:
            return 'low'
    
    def get_region_risk_factor(self, region):
        """Get cyclone risk factor based on region"""
        if region in CYCLONE_RISK_ZONES['Very High Risk']:
            return 0.9
        elif region in CYCLONE_RISK_ZONES['High Risk']:
            return 0.7
        elif region in CYCLONE_RISK_ZONES['Moderate Risk']:
            return 0.5
        elif region in CYCLONE_RISK_ZONES['Low Risk']:
            return 0.2
        else:
            return 0.5  # Default to moderate risk
    
    def train_model(self, data):
        """Train GradientBoosting model for cyclone prediction"""
        df = self.prepare_data(data)
        if df is None or len(df) < 10:
            logger.warning("Insufficient data for training cyclone model")
            return False
            
        # Add risk factor based on state
        df['risk_factor'] = df['state'].apply(self.get_region_risk_factor)
        
        # Add cyclone season indicator
        # Pre-monsoon (Apr-May) and post-monsoon (Oct-Dec) are cyclone seasons in India
        df['is_cyclone_season'] = df['month'].apply(lambda m: 1 if m in [4, 5, 10, 11, 12] else 0)
        
        # Encode categorical variables
        df_encoded = pd.get_dummies(df, columns=['state'])
        
        # Define features and target
        X = df_encoded.drop(['severity'], axis=1)
        y = df_encoded['severity']
        
        # Train-test split
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        # Create and train model
        self.model = Pipeline([
            ('scaler', StandardScaler()),
            ('regressor', GradientBoostingRegressor(n_estimators=100, random_state=42))
        ])
        
        self.model.fit(X_train, y_train)
        
        # Save model
        CYCLONE_MODEL_PATH = os.path.join(MODEL_DIR, 'cyclone_model.joblib')
        joblib.dump(self.model, CYCLONE_MODEL_PATH)
        logger.info("Cyclone prediction model trained and saved")
        return True
    
    def load_or_train_model(self):
        """Load existing model or train new one"""
        try:
            CYCLONE_MODEL_PATH = os.path.join(MODEL_DIR, 'cyclone_model.joblib')
            if os.path.exists(CYCLONE_MODEL_PATH):
                self.model = joblib.load(CYCLONE_MODEL_PATH)
                logger.info("Loaded existing cyclone model")
                return True
        except Exception as e:
            logger.error(f"Error loading cyclone model: {str(e)}")
        
        logger.info("No existing cyclone model found")
        return False
    
    def predict(self, state, historical_data):
        """Generate cyclone prediction for a state"""
        if self.model is None and not self.train_model(historical_data):
            # Fallback if model training fails
            return self._generate_rule_based_prediction(state, historical_data)
        
        try:
            # Create feature vector for prediction
            risk_factor = self.get_region_risk_factor(state)
            
            # Get current month for seasonality
            current_month = datetime.now().month
            is_cyclone_season = 1 if current_month in [4, 5, 10, 11, 12] else 0
            
            # Get average cyclone statistics from historical data
            state_cyclones = [c for c in historical_data if c.get('affected_state', '') == state]
            
            # If no data for this state, use all data
            if not state_cyclones:
                state_cyclones = historical_data
            
            avg_wind_speed = 120  # Default value if no data
            avg_pressure = 980    # Default value if no data
            avg_duration = 3      # Default value if no data
            
            if state_cyclones:
                wind_speeds = [c.get('wind_speed', 0) for c in state_cyclones if 'wind_speed' in c]
                pressures = [c.get('pressure', 0) for c in state_cyclones if 'pressure' in c]
                durations = [c.get('duration_days', 0) for c in state_cyclones if 'duration_days' in c]
                
                if wind_speeds:
                    avg_wind_speed = sum(wind_speeds) / len(wind_speeds)
                if pressures:
                    avg_pressure = sum(pressures) / len(pressures)
                if durations:
                    avg_duration = sum(durations) / len(durations)
            
            # Create features dataframe with one-hot encoding for state
            features = pd.DataFrame({
                'wind_speed': [avg_wind_speed],
                'pressure': [avg_pressure],
                'month': [current_month],
                'duration_days': [avg_duration],
                'risk_factor': [risk_factor],
                'is_cyclone_season': [is_cyclone_season]
            })
            
            # Add one-hot encoding for state
            for s in INDIAN_STATES:
                features[f'state_{s}'] = [1 if s == state else 0]
            
            # Make prediction
            predicted_severity_numeric = self.model.predict(features)[0]
            predicted_severity = self._numeric_to_severity(predicted_severity_numeric)
            
            # Calculate probability based on risk factor and season
            base_probability = risk_factor
            season_factor = 1.5 if is_cyclone_season else 0.6
            
            cyclone_probability = min(0.95, base_probability * season_factor)
            
            return {
                'probability': cyclone_probability,
                'wind_speed': avg_wind_speed,
                'pressure': avg_pressure,
                'severity': predicted_severity,
                'confidence': 0.7,  # Higher confidence with ML model
                'risk_factor': risk_factor,
                'is_cyclone_season': is_cyclone_season == 1
            }
            
        except Exception as e:
            logger.error(f"Error making cyclone prediction: {str(e)}")
            # Fallback to rule-based prediction
            return self._generate_rule_based_prediction(state, historical_data)
    
    def _generate_rule_based_prediction(self, state, historical_data):
        """Rule-based fallback if ML prediction fails"""
        risk_factor = self.get_region_risk_factor(state)
        
        # Get current month for seasonality
        current_month = datetime.now().month
        is_cyclone_season = current_month in [4, 5, 10, 11, 12]
        
        # Default wind speed and pressure based on risk zone
        wind_speed = 80
        pressure = 990
        
        if state in CYCLONE_RISK_ZONES['Very High Risk']:
            wind_speed = 140
            pressure = 960
            severity = 'high' if is_cyclone_season else 'medium'
        elif state in CYCLONE_RISK_ZONES['High Risk']:
            wind_speed = 120
            pressure = 970
            severity = 'medium' if is_cyclone_season else 'low'
        else:
            wind_speed = 100
            pressure = 980
            severity = 'low'
        
        # Calculate probability based on risk factor and season
        base_probability = risk_factor
        season_modifier = 1.5 if is_cyclone_season else 0.6
        cyclone_probability = min(0.9, base_probability * season_modifier)
        
        return {
            'probability': cyclone_probability,
            'wind_speed': wind_speed,
            'pressure': pressure,
            'severity': severity,
            'confidence': 0.5,  # Lower confidence for rule-based
            'risk_factor': risk_factor,
            'is_cyclone_season': is_cyclone_season
        }

# Global model instances
earthquake_model = None
flood_model = None
cyclone_model = None

def initialize_models():
    """Initialize prediction models"""
    global earthquake_model, flood_model, cyclone_model
    earthquake_model = EarthquakePredictionModel()
    flood_model = FloodPredictionModel()
    cyclone_model = CyclonePredictionModel()
    logger.info("ML prediction models initialized")

def get_earthquake_prediction(region, historical_data):
    """Get earthquake prediction for a region"""
    global earthquake_model
    if earthquake_model is None:
        earthquake_model = EarthquakePredictionModel()
    return earthquake_model.predict(region, historical_data)

def get_flood_prediction(state, historical_data, rainfall_forecast=None):
    """Get flood prediction for a state"""
    global flood_model
    if flood_model is None:
        flood_model = FloodPredictionModel()
    return flood_model.predict(state, historical_data, rainfall_forecast)

def get_cyclone_prediction(state, historical_data):
    """Get cyclone prediction for a state"""
    global cyclone_model
    if cyclone_model is None:
        cyclone_model = CyclonePredictionModel()
    return cyclone_model.predict(state, historical_data)

# Initialize models on module load
initialize_models() 