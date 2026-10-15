package com.zkt.backend.vehicle;

import com.zkt.backend.activity.ActivityService;
import com.zkt.backend.activity.Signup;
import com.zkt.backend.activity.SignupRepository;
import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.common.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VehicleService {
    private final VehicleRepository vehicles;
    private final VehiclePassengerRepository passengers;
    private final SignupRepository signups;
    private final UserRepository users;
    private final ActivityService activities;

    public VehicleService(VehicleRepository vehicles, VehiclePassengerRepository passengers, SignupRepository signups,
                          UserRepository users, ActivityService activities) {
        this.vehicles = vehicles; this.passengers = passengers; this.signups = signups; this.users = users; this.activities = activities;
    }

    @Transactional(readOnly = true)
    public List<VehicleView> list(Long userId, Long activityId) {
        activities.requireMember(activityId, userId);
        return views(activityId, vehicles.findByActivityIdOrderByPickupTime(activityId), userId);
    }

    @Transactional
    public VehicleView create(Long userId, Long activityId, Command c) {
        activities.requireActive(activities.creatorActivity(userId, activityId));
        Vehicle v = new Vehicle();
        v.setActivityId(activityId); v.setCreatedBy(userId);
        apply(v, c);
        return view(vehicles.save(v), userId);
    }

    // READ_COMMITTED: after taking the row lock, the seat/member count must see rows committed by the previous
    // lock holder. Under MySQL's default REPEATABLE READ an earlier plain read would pin an older snapshot.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VehicleView update(Long userId, Long activityId, Long vehicleId, Command c) {
        activities.requireActive(activities.creatorActivity(userId, activityId));
        Vehicle v = lockedVehicle(activityId, vehicleId);
        if (occupiedSeats(activityId, vehicleId) > c.seats())
            throw DomainException.conflict("SEATS_BELOW_OCCUPIED", "座位数不能少于已安排的乘客人数");
        apply(v, c);
        return view(v, userId);
    }

    @Transactional
    public void delete(Long userId, Long activityId, Long vehicleId) {
        activities.creatorActivity(userId, activityId);
        Vehicle v = vehicle(activityId, vehicleId);
        passengers.deleteByVehicleId(vehicleId);
        vehicles.delete(v);
    }

    /** Seats a member: members may seat themselves, the leader may seat anyone. Moving to another vehicle frees the old seat. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VehicleView board(Long actorId, Long activityId, Long vehicleId, Long targetUserId) {
        Long target = targetUserId == null ? actorId : targetUserId;
        if (target.equals(actorId)) activities.requireMember(activityId, actorId);
        else activities.creatorActivity(actorId, activityId);
        activities.activeActivity(activityId);
        Signup signup = signups.findByActivityIdAndUserId(activityId, target)
                .orElseThrow(() -> DomainException.badRequest("NOT_MEMBER", "只能安排活动成员乘车"));
        Vehicle v = lockedVehicle(activityId, vehicleId);
        Optional<VehiclePassenger> current = passengers.findByActivityIdAndUserId(activityId, target);
        if (current.isPresent() && current.get().getVehicleId().equals(vehicleId)) return view(v, actorId);
        // The row lock on the vehicle makes this check-then-insert safe against concurrent boarding.
        if (occupiedSeats(activityId, vehicleId) + seatsNeeded(signup) > v.getSeats())
            throw DomainException.conflict("VEHICLE_FULL", "该车剩余座位不足");
        current.ifPresent(passenger -> { passengers.delete(passenger); passengers.flush(); });
        VehiclePassenger p = new VehiclePassenger();
        p.setVehicleId(vehicleId); p.setActivityId(activityId); p.setUserId(target);
        passengers.save(p);
        return view(v, actorId);
    }

    @Transactional
    public VehicleView alight(Long actorId, Long activityId, Long vehicleId, Long targetUserId) {
        if (targetUserId.equals(actorId)) activities.requireMember(activityId, actorId);
        else activities.creatorActivity(actorId, activityId);
        Vehicle v = vehicle(activityId, vehicleId);
        passengers.findByActivityIdAndUserId(activityId, targetUserId)
                .filter(p -> p.getVehicleId().equals(vehicleId))
                .ifPresent(passengers::delete);
        return view(v, actorId);
    }

    private void apply(Vehicle v, Command c) {
        v.setPlateNumber(c.plateNumber().trim().toUpperCase()); v.setDriverName(c.driverName().trim());
        v.setPickupTime(c.pickupTime()); v.setPickupLocation(c.pickupLocation().trim()); v.setSeats(c.seats());
    }
    private Vehicle vehicle(Long activityId, Long vehicleId) {
        return vehicles.findById(vehicleId).filter(v -> v.getActivityId().equals(activityId))
                .orElseThrow(() -> DomainException.notFound("车辆不存在"));
    }
    private Vehicle lockedVehicle(Long activityId, Long vehicleId) {
        return vehicles.findForUpdate(vehicleId).filter(v -> v.getActivityId().equals(activityId))
                .orElseThrow(() -> DomainException.notFound("车辆不存在"));
    }
    /** A sign-up occupies the number of riders it registered (passengerCount), at least one seat. */
    private static int seatsNeeded(Signup signup) {
        return signup.getPassengerCount() == null ? 1 : Math.max(1, signup.getPassengerCount());
    }
    private int occupiedSeats(Long activityId, Long vehicleId) {
        Set<Long> riders = passengers.findByVehicleId(vehicleId).stream().map(VehiclePassenger::getUserId).collect(Collectors.toSet());
        return signups.findByActivityIdOrderByJoinedAt(activityId).stream()
                .filter(s -> riders.contains(s.getUserId())).mapToInt(VehicleService::seatsNeeded).sum();
    }

    private VehicleView view(Vehicle v, Long viewerId) { return views(v.getActivityId(), List.of(v), viewerId).get(0); }

    private List<VehicleView> views(Long activityId, List<Vehicle> list, Long viewerId) {
        if (list.isEmpty()) return List.of();
        Map<Long, List<VehiclePassenger>> byVehicle = passengers.findByActivityIdOrderByCreatedAt(activityId).stream()
                .collect(Collectors.groupingBy(VehiclePassenger::getVehicleId));
        Map<Long, Signup> signupByUser = signups.findByActivityIdOrderByJoinedAt(activityId).stream()
                .collect(Collectors.toMap(Signup::getUserId, Function.identity()));
        Set<Long> riderIds = byVehicle.values().stream().flatMap(List::stream).map(VehiclePassenger::getUserId).collect(Collectors.toSet());
        Map<Long, User> usersById = riderIds.isEmpty() ? Map.of()
                : users.findAllById(riderIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream().map(v -> {
            List<PassengerView> riders = byVehicle.getOrDefault(v.getId(), List.of()).stream().map(p -> {
                Signup s = signupByUser.get(p.getUserId());
                return new PassengerView(p.getUserId(), usersById.get(p.getUserId()).getNickname(), s == null ? 1 : seatsNeeded(s));
            }).toList();
            int occupied = riders.stream().mapToInt(PassengerView::seats).sum();
            boolean mine = riders.stream().anyMatch(r -> r.userId().equals(viewerId));
            return new VehicleView(v.getId(), v.getPlateNumber(), v.getDriverName(), v.getPickupTime(), v.getPickupLocation(),
                    v.getSeats(), occupied, riders, mine, v.getCreatedAt());
        }).toList();
    }

    public record Command(String plateNumber, String driverName, LocalDateTime pickupTime, String pickupLocation, int seats) {}
    public record PassengerView(Long userId, String nickname, int seats) {}
    public record VehicleView(Long id, String plateNumber, String driverName, LocalDateTime pickupTime, String pickupLocation,
                              int seats, int occupiedSeats, List<PassengerView> passengers, boolean mine, LocalDateTime createdAt) {}
}
