package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link EuroCoin} instances when the id and/or other
 * attributes are already known.
 * <p>
 * Use this instead of assembling a {@link EuroCoinBuilder} manually in cases
 * where you need to preserve an existing identifier (database hydration,
 * imports, synchronization, test fixtures). The public construction path via
 * the builder normally generates an id; using this
 * factory makes the intent explicit when an external id must be retained.
 * </p>
 */
public class EuroCoinFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinFactory.class);
    
    /**
     * Build a coin from the current JDBC {@link ResultSet} row (DB hydration).
     * @param resultSet positioned result set
     * @return coin instance with preserved id
     * @throws SQLException if input invalid
     */
    public EuroCoin fromDataBaseEntry(ResultSet resultSet) throws SQLException{
        try {
            return new EuroCoinBuilder()
                .setId(resultSet.getString("coin_id"))
                .setYear(resultSet.getInt("year"))
                .setValue(CoinValue.fromCentValue(resultSet.getInt("coin_value")))
                .setMintCountry(CoinCountry.fromIsoCode(resultSet.getString("mint_country")))
                .setMint(Mint.fromMintMark(resultSet.getString("mint")))
                .setDescription(new CoinDescription(resultSet.getString("description")))
                .setCollectionId(resultSet.getString("collection_id"))
                .build();
            
        } catch (IllegalArgumentException e){
            logger.error("Invalid data in database entry: {}", e.getMessage());
            throw new SQLException("Invalid database entry data", e);
        } catch (IllegalStateException e){
            logger.error("Invalid state when building coin from database entry: {}", e.getMessage());
            throw new SQLException("Invalid coin state from database", e);
        }
    }
}
