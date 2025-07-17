/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
 */

package com.oracle.test.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.pic.identity.authentication.Principal;
import com.oracle.pic.identity.authorization.permissions.annotations.AuthorizationPermission;
import com.oracle.pic.identity.authorization.sdk.AuthorizationRequest;
import com.oracle.test.model.Car;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CarResource extends AbstractCarBaseResource {

    private final Map<Integer, Car> cars = new ConcurrentHashMap<>();

    @Override
    @AuthorizationPermission("CAR_WRITE")
    public Car addCar(Car car,
                      com.oracle.pic.identity.authentication.Principal principal,
                      com.oracle.pic.identity.authorization.sdk.AuthorizationRequest authorizationRequest) {
        cars.put(car.getId(), car);
        return car;
    }

    @Override
    @AuthorizationPermission("CAR_READ")
    public Car getCarById(Integer id, Principal principal, AuthorizationRequest authorizationRequest) {
        return cars.get(id);
    }
}