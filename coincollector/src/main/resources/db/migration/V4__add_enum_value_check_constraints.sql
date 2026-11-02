ALTER TABLE euroCoinTypes
ADD CONSTRAINT coin_value_enum_check
CHECK (coin_value IN (1, 2, 5, 10, 20, 50, 100, 200));

ALTER TABLE euroCoinTypes
ADD CONSTRAINT mint_country_enum_check
CHECK (
    mint_country IN (
        'AT', 'BE', 'CY', 'DE', 'EE', 'ES', 'FI', 'FR', 'GR', 'IE', 'IT',
        'LT', 'LU', 'LV', 'MT', 'NL', 'PT', 'SI', 'SK', 'SM', 'VA', 'MC',
        'AD', 'BG'
    )
);

ALTER TABLE euroCoinTypes
ADD CONSTRAINT mint_enum_check
CHECK (mint IS NULL OR mint IN ('A', 'D', 'F', 'G', 'J', 'UNKNOWN'));
