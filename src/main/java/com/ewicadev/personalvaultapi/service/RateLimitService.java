package com.ewicadev.personalvaultapi.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class RateLimitService {

  private static final Duration WINDOW = Duration.ofHours(1);

  private final Cache<String, AtomicInteger> signupIpCache = Caffeine.newBuilder()
      .expireAfterWrite(WINDOW)
      .build();
  private final Cache<String, AtomicInteger> signupEmailCache = Caffeine.newBuilder()
      .expireAfterWrite(WINDOW)
      .build();
  private final Cache<String, AtomicInteger> resendIpCache = Caffeine.newBuilder()
      .expireAfterWrite(WINDOW)
      .build();
  private final Cache<String, AtomicInteger> resendEmailCache = Caffeine.newBuilder()
      .expireAfterWrite(WINDOW)
      .build();
  private final Cache<String, AtomicInteger> verifyIpCache = Caffeine.newBuilder()
      .expireAfterWrite(WINDOW)
      .build();

  public boolean checkSignupIpLimit(String ip) {
    return checkLimit("signup:ip:" + ip, signupIpCache, 5);
  }

  public boolean checkSignupEmailLimit(String email) {
    return checkLimit("signup:email:" + email, signupEmailCache, 3);
  }

  public boolean checkResendIpLimit(String ip) {
    return checkLimit("resend:ip:" + ip, resendIpCache, 5);
  }

  public boolean checkResendEmailLimit(String email) {
    return checkLimit("resend:email:" + email, resendEmailCache, 3);
  }

  public boolean checkVerifyIpLimit(String ip) {
    return checkLimit("verify:ip:" + ip, verifyIpCache, 10);
  }

  private boolean checkLimit(String key, Cache<String, AtomicInteger> cache, int maxRequests) {
    AtomicInteger count = cache.get(key, k -> new AtomicInteger(0));
    return count.incrementAndGet() <= maxRequests;
  }
}