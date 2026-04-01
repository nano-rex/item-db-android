# Item DB Schema Notes

## Database model
- SQLite local database (`itemdb.db`)
- Items with many-to-many topic tags
- Historical values in append-only style table (`item_history`)

## Core tables
- `items(id, name, description, created_at)`
- `topics(id, name)`
- `item_topics(item_id, topic_id)`
- `item_history(id, item_id, price, observed_date, location, note)`
