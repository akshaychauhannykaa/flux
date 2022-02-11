package com.example.experiment.services.impl;

import com.example.experiment.controller.TestController;
import com.example.experiment.enity.People;
import com.example.experiment.services.ApiService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {

    private final Logger LOG = LogManager.getLogger(ApiServiceImpl.class);

    @Autowired
    private WebClient webClient;

    @Override
    public Mono<People> getActualApiCall(Integer sec,String identifier) {

        String serviceUrl = "http://localhost:8191/delay/?secDelay=".concat(sec.toString().concat("&identifier=").concat(identifier));
        LOG.info("getActualApiCall");

        return webClient.method(HttpMethod.GET)
                .uri(serviceUrl)
                .retrieve()
                .bodyToMono(People.class);
    }

    @Override
    public String blockingCall(Integer sec,String identifier) {
        LOG.info("init blocking Call");

        if(true == identifier.isEmpty()){
            identifier = "";
        }

        try{
            Thread.sleep(1000 * sec);
            return "Final String after" + sec.toString();
        }catch (Exception e){

        }
        return "response with exception " + sec.toString().concat(" :: ").concat(identifier);


//        CountDownLatch latch = new CountDownLatch(1);
//        new Thread(){
//            @Override
//            public void run() {
//                super.run();
//                try{
//                    Thread.sleep(sec * 1000);
//                    latch.countDown();
//                }catch (Exception ee){
//
//                }
//            }
//        }.start();
//        try{
//            latch.await();
//        }catch (Exception ee){
//
//        }
//        return "Final String after" + sec.toString();
    }


    @Override
    public Mono<Object> getExceptionAfterTime(Integer sec, String identifier) {
        LOG.info("init Error Service");

        return Mono.fromCallable(() -> {
            try{
                Thread.sleep(1000 * sec);
            }catch(Exception ee){

            }

            throw new Exception("Some Exception :: ".concat(identifier));
        }).subscribeOn(Schedulers.parallel());

    }
}
