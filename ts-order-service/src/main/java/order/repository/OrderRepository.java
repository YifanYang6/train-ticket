package order.repository;

import order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * @author fdse
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    @Override
    Optional<Order> findById(String id);

    @Override
    ArrayList<Order> findAll();

    // Add paginated version of existing methods
    Page<Order> findByAccountId(String accountId, Pageable pageable);
    
    ArrayList<Order> findByAccountId(String accountId);

    Page<Order> findByTravelDateAndTrainNumber(String travelDate, String trainNumber, Pageable pageable);
    
    ArrayList<Order> findByTravelDateAndTrainNumber(String travelDate, String trainNumber);

    // Advanced query with filters for more efficient querying
    @Query("SELECT o FROM Order o WHERE o.accountId = :accountId " +
           "AND (:enableStateQuery = false OR o.status = :state) " +
           "AND (:enableTravelDateQuery = false OR (o.travelDate >= :travelDateStart AND o.travelDate <= :travelDateEnd)) " +
           "AND (:enableBoughtDateQuery = false OR (o.boughtDate >= :boughtDateStart AND o.boughtDate <= :boughtDateEnd))")
    Page<Order> findByAccountIdAndFilters(
            @Param("accountId") String accountId,
            @Param("enableStateQuery") boolean enableStateQuery,
            @Param("state") int state,
            @Param("enableTravelDateQuery") boolean enableTravelDateQuery,
            @Param("travelDateStart") String travelDateStart,
            @Param("travelDateEnd") String travelDateEnd,
            @Param("enableBoughtDateQuery") boolean enableBoughtDateQuery,
            @Param("boughtDateStart") String boughtDateStart,
            @Param("boughtDateEnd") String boughtDateEnd,
            Pageable pageable);

    @Override
    void deleteById(String id);
}
