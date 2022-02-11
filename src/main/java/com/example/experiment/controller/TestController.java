package com.example.experiment.controller;

import com.example.experiment.enity.People;
import com.example.experiment.services.ApiService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/test")
@RestController
@RequiredArgsConstructor
public class TestController {

    private final Logger LOG = LogManager.getLogger(TestController.class);

    @Autowired
    ApiService apiService;


    @GetMapping("/oneHttpCall")
    public Mono<People> justoneCall() {
        Mono<People> v1 = apiService.getActualApiCall(1,"i1").log();
        return v1;
    }


    @GetMapping("/allparallel")
    public Flux<People> allParallel() {
        Mono<People> v1 = apiService.getActualApiCall(1,"i1");
        Mono<People> v2 = apiService.getActualApiCall(2,"i2");
        Mono<People> v3 = apiService.getActualApiCall(3,"i3");
        Mono<People> v4 = apiService.getActualApiCall(4,"i4");
        Mono<People> v5 = apiService.getActualApiCall(5,"i5");

        return Flux.merge(v1,v2,v3,v4,v5);
    }

    @GetMapping("/allparallel1sec")
    public Flux<People> allParallel1S() {
        Mono<People> v1 = apiService.getActualApiCall(1,"i1").log();
        Mono<People> v2 = apiService.getActualApiCall(1,"i2").log();
        Mono<People> v3 = apiService.getActualApiCall(1,"i3").log();
        Mono<People> v4 = apiService.getActualApiCall(1,"i4").log();
        Mono<People> v5 = apiService.getActualApiCall(1,"i5").log();

        return Flux.merge(v1,v2,v3,v4,v5);
    }


    @GetMapping("/coldPublisherExample")
    public void coldSubscriber(){
        try{
            // GENERATES A STREAM
            // MULTIPLE SUBSCRIBERS
            // Different thread for both subscriber block
            // flux is a publisher
            // NOTE : every subscriber will start from starting unless we make it a hot publisher by calling

            LOG.info("Main Thread");

            Flux<Long> flux =  Flux.interval(Duration.ofSeconds(1));

            flux.subscribe(i -> {
                LOG.info("first_subscriber received value:" + i );
            });

            Thread.sleep(3000);

            // SECOND SUBSCRIBER START AFTER 3 SEC
            flux.subscribe(i -> {
                LOG.info("second_subscriber received value:" + i );
            });

        }catch(Exception ee){

        }
    }


    @GetMapping("/hotSubscriber")
    public void howSubscriber(){
        try{

            // EVERY subscriber is handled with same thread

            LOG.info("Main Thread");

            Flux<Long> flux =  Flux.interval(Duration.ofSeconds(1));
            ConnectableFlux<Long> connectableFlux   = flux.publish(); // will convert this to hot publisher

            connectableFlux.subscribe(i -> {
                LOG.info("first_subscriber received value:" + i );
            });

            Thread.sleep(3000);

            connectableFlux.connect();

            connectableFlux.subscribe(i -> {
                LOG.info("second_subscriber received value:" + i );
            });

        }catch(Exception ee){

        }
    }

    @GetMapping("/blockingList")
    public Mono<List<String>> blockinglist(){
        // {blocking Call} as thread will be blocked and will not be released

        // Obtaining a Flux or a Mono does not necessarily mean that it runs in a dedicated Thread.
        // Instead, most operators continue working in the Thread on which the previous operator executed.
        // Unless specified, the topmost operator (the source)
        // itself runs on the Thread in which the subscribe() call was made.

        LOG.info("start");
        return Flux.just(1,2,3,4,5)
                .log()
                .flatMap(a -> Mono.just(apiService.blockingCall(a,"i".concat(a.toString()))))
                .collect(Collectors.toList());
    }

    @GetMapping("/blockingToNonBlocking")
    public Mono<List<String>> wayToDefineSubs(){

        // Converting from blocking to nonBlocking

        return Flux.just(1,2,3,4,5)
                .log()
                .flatMap(p -> {

                    // Returns a non-blocking Publisher with a Single Value (Mono)
                    return Mono.fromCallable(() -> {
                        return apiService.blockingCall(p,"i".concat(p.toString()));
                    }).subscribeOn(Schedulers.parallel());

                }).collect(Collectors.toList());
    }


    @GetMapping("/chainingResponse")
    public Mono<List<People>> chainingResponse(){

        // Request to {getActualApiCall} needs response from {blockingCall}
        // SEQUENCE TIME    :  1+2 + 1+2
        // TIME TAKEN       : 2 + 2

        return Flux.just(1,2)
                .log()
                .flatMap(p -> {

                    return Mono.fromCallable(() -> {
                        return apiService.blockingCall(p,"i1");
                    }).subscribeOn(Schedulers.parallel());

                })
                .flatMap(p -> {
                    String responseFromBlockingCall = p;
                    String numberOnly = responseFromBlockingCall.replaceAll("[^0-9]", "");
                    return apiService.getActualApiCall(Integer.parseInt(numberOnly),"i2");
                }).collect(Collectors.toList());
    }


    @GetMapping("/zippingResponse")
    public Mono<List<People>> zippingResponse(){

        // Chaining and zipping Response

        return Flux.just(1,2)
                .flatMap(p -> {
                   return apiService.getActualApiCall(p,"i1");
                })
                .flatMap(p -> {
                    Mono<People> peopleFlux =  apiService.getActualApiCall(p.getDelay(),"i2");
                    return peopleFlux.zipWith(Mono.just(p))
                            .flatMapMany(tuple -> {

                                People newp = new People();
                                newp.setEmail(tuple.getT1().getEmail() .concat(" - ").concat(tuple.getT2().getEmail()));
                                newp.setName(tuple.getT1().getName() .concat(" - ").concat(tuple.getT2().getName()));
                                newp.setDelay(tuple.getT1().getDelay());
                                newp.setIdentifier(tuple.getT1().getIdentifier() .concat(" - ").concat(tuple.getT2().getIdentifier()));
                                return Mono.just(newp);

                            });
                }).collect(Collectors.toList());
    }



    @GetMapping("/buildObjectByChaining")
    public Flux<Object> buildObjectByChaining() {

        // Chaining and zipping Response

        return Flux.just(1, 2)
                .flatMap(p -> {
                    People buildingPeopleObject = new People();
                    buildingPeopleObject.setName("");
                    buildingPeopleObject.setDelay(0);
                    buildingPeopleObject.setEmail("");
                    buildingPeopleObject.setIdentifier("");

                    return apiService.getActualApiCall(p, "i1")
                            .flatMap(t -> {

                                buildingPeopleObject.setName(buildingPeopleObject.getName().concat(" - ").concat(t.getName()));
                                buildingPeopleObject.setEmail(buildingPeopleObject.getEmail().concat(" - ").concat(t.getEmail()));
                                buildingPeopleObject.setIdentifier(buildingPeopleObject.getIdentifier().concat(" - ").concat(t.getIdentifier()));
                                buildingPeopleObject.setDelay(buildingPeopleObject.getDelay() + t.getDelay());
                                return Mono.just(buildingPeopleObject);

                            })
                            .flatMap(t -> {
                                return apiService.getActualApiCall(p, "i2")
                                        .flatMap(u -> {

                                            t.setName(t.getName().concat(" - ").concat(u.getName()));
                                            t.setEmail(t.getEmail().concat(" - ").concat(u.getEmail()));
                                            t.setIdentifier(t.getIdentifier().concat(" - ").concat(u.getIdentifier()));
                                            t.setDelay(t.getDelay() + u.getDelay());
                                            return Mono.just(t);

                                        });
                            });
                });
    }


    @GetMapping("/exceptionHandling")
    public Mono exceptionHandling() {
        return apiService.getExceptionAfterTime(2, "i1")
                .flatMap((response) -> {
                    Map<String,Object> successMap = new HashMap<>();
                    successMap.put("status",200);
                    successMap.put("data",response);
                    return Mono.just(new ResponseEntity<>(successMap,HttpStatus.BAD_REQUEST));
                })
                .onErrorResume((error) -> {
                    Map<String,Object> errorMap = new HashMap<>();
                    errorMap.put("status",400);
                    errorMap.put("errorMessage", "Message Will go here" );
                    return Mono.just(new ResponseEntity<>(errorMap,HttpStatus.BAD_REQUEST));
                } );
    }






}
