package com.example.experiment.services;

import com.example.experiment.enity.People;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiService {
   Mono<People> getActualApiCall(Integer sec,String identifier);
   String blockingCall(Integer sec,String identifier);
   Mono<Object> getExceptionAfterTime(Integer sec,String identifier);
}
