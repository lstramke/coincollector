CREATE TABLE IF NOT EXISTS euroCoinTypes (
    type_id TEXT PRIMARY KEY,
    year INTEGER NOT NULL,
    coin_value INTEGER NOT NULL,
    mint_country TEXT NOT NULL,
    mint TEXT,
    UNIQUE (year, coin_value, mint_country, mint)
);

INSERT INTO euroCoinTypes (
    type_id,
    year,
    coin_value,
    mint_country,
    mint
)
SELECT
    coin_id,
    year,
    coin_value,
    mint_country,
    mint
FROM euroCoins;

CREATE TABLE euroCoins_new (
    coin_id TEXT PRIMARY KEY,
    description TEXT NOT NULL,
    collection_id TEXT NOT NULL,
    type_id TEXT NOT NULL,
    FOREIGN KEY (collection_id) REFERENCES euroCoinCollections(collection_id) ON DELETE CASCADE,
    FOREIGN KEY (type_id) REFERENCES euroCoinTypes(type_id) ON DELETE RESTRICT
);

INSERT INTO euroCoins_new (
    coin_id,
    description,
    collection_id,
    type_id
)
SELECT
    lower(
        hex(randomblob(4)) || '-' ||
        hex(randomblob(2)) || '-' ||
        hex(randomblob(2)) || '-' ||
        hex(randomblob(2)) || '-' ||
        hex(randomblob(6))
    ),
    description,
    collection_id,
    coin_id
FROM euroCoins;

DROP TABLE euroCoins;
ALTER TABLE euroCoins_new RENAME TO euroCoins;