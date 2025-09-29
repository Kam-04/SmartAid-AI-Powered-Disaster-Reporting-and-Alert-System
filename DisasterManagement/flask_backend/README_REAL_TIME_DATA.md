# Real-Time Disaster Data Integration for India

This module adds real-time earthquake and flood data integration to the Disaster Management app, specifically for Indian regions.

## Data Sources

### Earthquake Data
- **USGS Earthquake API**: Fetches real-time earthquake data within Indian geographical boundaries
- Data is filtered to include only earthquakes within India's coordinates
- Each earthquake is mapped to the corresponding Indian state based on coordinates

### Flood Data
- Currently uses simulated data (placeholder implementation)
- In a production environment, this would connect to:
  - **Indian Meteorological Department (IMD)** data (requires partnership)
  - **Central Water Commission (CWC)** data (requires partnership)
  - **NASA FIRMS** (Fire Information for Resource Management System) for satellite imagery
  - **Sentinel** satellite data for flood detection

## Setup and Running

1. Install required dependencies:
   ```
   pip install -r requirement1.txt
   ```

2. Run the data fetcher manually:
   ```
   python real_time_data_fetcher.py
   ```

3. Run the scheduler to fetch data every 3 hours:
   ```
   python data_scheduler.py
   ```

4. Start the Flask backend which will fetch initial data automatically:
   ```
   python app.py
   ```

## Available Endpoints

### Viewing Data

- **GET /api/realtime/disasters**: Get real-time disaster data
  - Query parameters:
    - `type`: Type of disaster (`earthquake` or `flood`)
    - `state`: Filter by Indian state
    - `days`: Number of days to look back (default: 30)

- **GET /api/monitoring/dashboard**: Dashboard data including recent disasters
- **GET /api/monitoring/map**: Map data for disasters
  - Query parameters:
    - `type`: Type of disaster (`earthquake` or `flood`)
    - `state`: Filter by Indian state

### Historical Analysis

- **GET /api/analysis/historical/earthquake**: Historical earthquake analysis
  - Query parameters:
    - `state`: Filter by Indian state
    - `days`: Number of days to analyze (default: 365)

- **GET /api/analysis/historical/flood**: Historical flood analysis
  - Query parameters:
    - `state`: Filter by Indian state
    - `days`: Number of days to analyze (default: 365)

### Prediction

- **POST /api/analysis/predict/earthquake**: Predict earthquakes
  - Request body:
    ```json
    {
      "region": "Kerala"
    }
    ```

- **POST /api/analysis/predict/flood**: Predict floods
  - Request body:
    ```json
    {
      "state": "Assam",
      "district": "Dhemaji"
    }
    ```

### Manual Update

- **POST /api/realtime/update**: Manually trigger data update
  - No parameters required
  - In production, would require authentication

## Integration in AI-Powered Disaster Analysis Module

This real-time data system enhances the AI-Powered Disaster Impact Analysis & Prediction module by:

1. Providing actual, current data instead of simulated data
2. Improving prediction accuracy with real historical patterns
3. Enabling region-specific analysis for Indian states
4. Supporting both earthquake and flood disaster types
5. Offering more detailed and accurate recommendations based on real patterns

## Future Improvements

1. Add authentication for data update endpoints
2. Integrate with official Indian government APIs when available
3. Implement more sophisticated prediction models using the real-time data
4. Add support for more disaster types (cyclones, landslides, etc.)
5. Improve state/district mapping using precise GeoJSON boundaries

## Real-Time Disaster Data Sources

This document outlines the data sources used for real-time disaster monitoring in our application.

## Earthquake Data Sources

### 1. USGS Earthquake API
- **URL**: https://earthquake.usgs.gov/fdsnws/event/1/query
- **Description**: The United States Geological Survey provides a comprehensive API for earthquake data globally.
- **Coverage**: Global, including India
- **Format**: GeoJSON
- **Update Frequency**: Near real-time (typically within minutes of an event)
- **Documentation**: https://earthquake.usgs.gov/fdsnws/event/1/

### 2. National Center for Seismology (NCS) India
- **URL**: https://riseq.seismo.gov.in/
- **Description**: The National Center for Seismology is the nodal agency of the Government of India for monitoring earthquake activity in the country.
- **Coverage**: India and surrounding regions
- **Format**: Web data (HTML parsed)
- **Update Frequency**: Near real-time
- **Notes**: NCS maintains a National Seismological Network of 115 stations across India. This data source provides more localized and specific earthquake data for India.

## Flood Data Sources

Currently, we are using simulated flood data. In a production environment, these should be replaced with real data sources:

### Potential Flood Data Sources:
1. **Indian Meteorological Department (IMD)**
   - Rainfall data that can be used to predict and monitor floods
   - Requires official partnership or approval

2. **Central Water Commission (CWC)**
   - River water level data
   - Flood forecasting information
   - Requires official partnership or approval

3. **INCOIS (Indian National Centre for Ocean Information Services)**
   - Coastal flooding and tsunami warnings
   - Ocean state forecasts

4. **Satellite Data**
   - NASA MODIS/Sentinel satellite imagery APIs
   - Can be processed for flood extent mapping

## Implementation Details

The data fetching and processing pipeline works as follows:

1. **Data Collection**:
   - Scheduled periodic requests to data sources
   - Each source has its own fetcher function

2. **Data Processing**:
   - Normalization of data into a consistent format
   - Filtering for events in relevant regions
   - Enrichment with additional information (e.g., state mapping for coordinates)

3. **Database Storage**:
   - MongoDB collections for each disaster type
   - Deduplication based on time and location
   - Historical data retention

4. **API Endpoints**:
   - REST API endpoints to access processed data
   - Filtering capabilities
   - Integration with prediction models

## Adding New Data Sources

To add a new data source:

1. Create a new fetcher function in `real_time_data_fetcher.py`
2. Process the data into the standard format
3. Update the main data update function to include the new source
4. Add appropriate error handling and logging
5. Document the new source in this README

## Data Refresh Rates

- Earthquake data: Every 30 minutes
- Flood data: Every 1 hour 