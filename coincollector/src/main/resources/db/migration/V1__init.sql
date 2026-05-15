CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS euroCoinCollectionGroups (
    group_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    owner_id TEXT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS euroCoinCollections (
    collection_id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    group_id TEXT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES euroCoinCollectionGroups(group_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS euroCoins (
    coin_id TEXT PRIMARY KEY,
    year INTEGER NOT NULL,
    coin_value INTEGER NOT NULL,
    mint_country TEXT NOT NULL,
    mint TEXT,
    description TEXT NOT NULL,
    collection_id TEXT NOT NULL,
    FOREIGN KEY (collection_id) REFERENCES euroCoinCollections(collection_id) ON DELETE CASCADE
);