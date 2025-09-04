package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    public List<Notification> findByReceiverId(UUID receiverId);

//      /* CrudRepository의 기본 메소드 */
//    public Notification save(Notification notification);
//
//      /* CrudRepository의 기본 메소드 */
//    public Optional<Notification> findById(UUID notificationId);
//
//      /* JpaRepository의 기본 메소드 */
//    public List<Notification> findAll();
//
//      /* CrudRepository의 기본 메소드 */
//    public boolean existsById(UUID notificationId);
//
//      /* CrudRepository의 기본 메소드 */
//    public void deleteById(UUID notificationId);
}
