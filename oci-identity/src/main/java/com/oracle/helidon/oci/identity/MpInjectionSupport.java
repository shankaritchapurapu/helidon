/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.helidon.oci.identity;

import java.lang.reflect.Type;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import io.helidon.security.SecurityContext;
import io.helidon.security.Subject;

import com.oracle.pic.identity.authorization.sdk.AuthorizationRequestFactory;

/**
 * Provides the bridge from OCI injectable identity types into CDI.
 */
@ApplicationScoped
public class MpInjectionSupport /*implements Extension*/ {
    static final Subject EMPTY_SUBJECT = Subject.builder().build();
//    private final Set<Type> proxyTypes = new HashSet<>();
//    private final Set<Type> inProcessProxyTypes = new HashSet<>();

    private MpInjectionSupport() {
    }

    // Subject.class is final so can't be proxied so can't be @RequestScoped.
    @Produces
    @Default
    @Dependent
    private static Subject produceDefaultSubject(SecurityContext sc) {
        // TODO:
        return sc.service().orElse(EMPTY_SUBJECT);
    }

    // Subject.class is final so can't be proxied so can't be @RequestScoped.
    @Produces
    @Service
    @Dependent
    private static Subject produceContextualSubject(SecurityContext sc) {
        // TODO:
        return sc.service().orElse(EMPTY_SUBJECT);
    }

    @Produces
    @Default
    //    @PrincipalContext // TODO: this anno can only can be used on parameters
    @Dependent
    public static com.oracle.pic.identity.authentication.Principal produceDefaultPrincipal() {
        // TODO:
        return com.oracle.pic.authproxy.AuthProxyAnonymousPrincipal.builder().build();
    }

    @Produces
    @Dependent
    //    @PrincipalContext // TODO: this anno can only can be used on parameters
    @Service
    public static com.oracle.pic.identity.authentication.Principal produceContextualPrincipal(SecurityContext sc/*,
            InjectionPoint ip*/) {
        // TODO:
        return com.oracle.pic.authproxy.AuthProxyAnonymousPrincipal.builder().build();
    }

    @Produces
    @Dependent
    //    @AuthorizationRequestContext // TODO: this anno can only can be used on parameters
    @Service
    public static com.oracle.pic.identity.authorization.sdk.AuthorizationRequest produceContextualAuthorizationRequest(SecurityContext sc/*,
                                                                                                             InjectionPoint ip*/) {
        // TODO:
        return AuthorizationRequestFactory.serviceRequest("test", produceContextualPrincipal(sc));
    }

    /**
     * Process injection points.
     * <p>
     * In this method all the injection points that have the authorization annotations are processed
     * and their types are stored so that in the {@link #afterBean(javax.enterprise.inject.spi.AfterBeanDiscovery, javax.enterprise.inject.spi.BeanManager)}
     * we can manually create a producer for the correct service proxy type.
     *
     * @param pip  the injection point
     * @param <X> the declared type of the injection point.
     * @param <T> the bean class of the bean that declares the injection point
     */
    public <T, X> void gatherApplications(@Observes ProcessInjectionPoint<T, X> pip) {
        System.out.println("xxxx gatherApplications");
        Annotated annotated = pip.getInjectionPoint().getAnnotated();
        if (annotated.isAnnotationPresent(com.oracle.pic.identity.authorization.sdk.context.PrincipalContext.class)
                || annotated.isAnnotationPresent(com.oracle.pic.identity.authorization.sdk.context.AuthorizationRequestContext.class)) {
            Type type = pip.getInjectionPoint().getType();
//
//            if (annotated.isAnnotationPresent(InProcessGrpcChannel.class)) {
//                inProcessProxyTypes.add(type);
//            } else {
//                proxyTypes.add(type);
//            }
        }
    }

    /**
     * Process the previously captured injection points.
     * <p>
     * For each injection point we create a producer bean for the required type.
     *
     * @param event the {@link javax.enterprise.inject.spi.AfterBeanDiscovery} event
     * @param beanManager the CDI bean manager
     */
    public void afterBean(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        System.out.println("xxxx afterBean");
//        AnnotatedType<GrpcProxyProducer> producerType = beanManager.createAnnotatedType(GrpcProxyProducer.class);
//        AnnotatedMethod<? super GrpcProxyProducer> producerMethod = producerType.getMethods()
//                .stream()
//                .filter(m -> m.isAnnotationPresent(GrpcProxy.class))
//                .filter(m -> m.isAnnotationPresent(GrpcChannel.class))
//                .findFirst()
//                .get();
//
//        AnnotatedMethod<? super GrpcProxyProducer> inProcessMethod = producerType.getMethods()
//                .stream()
//                .filter(m -> m.isAnnotationPresent(GrpcProxy.class))
//                .filter(m -> m.isAnnotationPresent(InProcessGrpcChannel.class))
//                .findFirst()
//                .get();
//
//        for (Type type : proxyTypes) {
//            addProducerBean(event, beanManager, producerMethod, type);
//        }
//
//        for (Type type : inProcessProxyTypes) {
//            addProducerBean(event, beanManager, inProcessMethod, type);
//        }
    }

//    private void addProducerBean(AfterBeanDiscovery event,
//                                 BeanManager beanManager,
//                                 AnnotatedMethod<? super GrpcProxyProducer> producerMethod,
//                                 Type type) {
//
//        BeanAttributes<?> producerAttributes = beanManager.createBeanAttributes(producerMethod);
//        ProducerFactory<GrpcProxyProducer> factory = beanManager.getProducerFactory(producerMethod, null);
//        Set<Type> types = Set.of(Object.class, type);
//        BeanAttributes<?> beanAttributes = DelegatingBeanAttributes.create(producerAttributes, types);
//        event.addBean(beanManager.createBean(beanAttributes, GrpcProxyProducer.class, factory));
//    }

}
