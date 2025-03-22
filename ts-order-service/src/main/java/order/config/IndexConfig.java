package order.config;

import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

/**
 * Configuration for database indexes to improve query performance
 */
@Configuration
public class IndexConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates necessary indexes for the orders table to optimize query performance
     * Note: This is meant to run during application startup
     */
    @PostConstruct
    @Transactional
    public void createIndexes() {
        // Index for account_id since we frequently query by it
        entityManager.createNativeQuery(
            "CREATE INDEX IF NOT EXISTS idx_order_account_id ON orders (account_id)"
        ).executeUpdate();
        
        // Index for travel_date and train_number since we query by both
        entityManager.createNativeQuery(
            "CREATE INDEX IF NOT EXISTS idx_order_travel_train ON orders (travel_date, train_number)"
        ).executeUpdate();
        
        // Index for status since we filter by it
        entityManager.createNativeQuery(
            "CREATE INDEX IF NOT EXISTS idx_order_status ON orders (status)"
        ).executeUpdate();
        
        // Index for bought_date since we filter by it
        entityManager.createNativeQuery(
            "CREATE INDEX IF NOT EXISTS idx_order_bought_date ON orders (bought_date)"
        ).executeUpdate();
    }
}
