import time
import schedule
import logging
from real_time_data_fetcher import update_all_data

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def scheduled_job():
    """Run the data update job"""
    logger.info("Starting scheduled data update...")
    update_all_data()
    logger.info("Scheduled data update completed")

# Schedule the job to run every 3 hours
schedule.every(3).hours.do(scheduled_job)

# Run immediately on startup
scheduled_job()

# Keep the scheduler running
if __name__ == "__main__":
    logger.info("Data scheduler started...")
    while True:
        schedule.run_pending()
        time.sleep(60)  # Sleep for 1 minute between checks 