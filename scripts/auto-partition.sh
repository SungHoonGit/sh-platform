#!/bin/bash
DB_NAME="scraper_platform"
DB_USER="root"
LOG_FILE="/var/log/partition-maintenance.log"

NEXT_MONTH=$(date -d "$(date +%Y-%m-15) + 1 month" +%Y-%m)
NEXT_MONTH_END=$(date -d "$NEXT_MONTH + 1 month" +%Y-%m-%d)
PARTITION_NAME="p$(date -d "$NEXT_MONTH" +%Y_%m)"
DAYS_VALUE=$(date -d "$NEXT_MONTH_END" +%s)
DAYS_VALUE=$((DAYS_VALUE / 86400))

echo "$(date): Starting partition maintenance" >> $LOG_FILE

TABLES=("crawl_log" "common_schedule_log" "common_notification_log")

for TABLE in "${TABLES[@]}"; do
    SQL="ALTER TABLE $TABLE REORGANIZE PARTITION p_future INTO (PARTITION $PARTITION_NAME VALUES LESS THAN ($DAYS_VALUE), PARTITION p_future VALUES LESS THAN MAXVALUE);"
    mysql -u $DB_USER $DB_NAME -e "$SQL" 2>> $LOG_FILE
    echo "$(date): Added partition $PARTITION_NAME to $TABLE" >> $LOG_FILE
    
    OLD_MONTH=$(date -d "$(date +%Y-%m-15) - 6 month" +%Y-%m)
    OLD_PARTITION="p$(date -d "$OLD_MONTH" +%Y_%m)"
    SQL="ALTER TABLE $TABLE DROP PARTITION $OLD_PARTITION;"
    mysql -u $DB_USER $DB_NAME -e "$SQL" 2>> $LOG_FILE
    echo "$(date): Dropped partition $OLD_PARTITION from $TABLE" >> $LOG_FILE
done

echo "$(date): Partition maintenance completed" >> $LOG_FILE
