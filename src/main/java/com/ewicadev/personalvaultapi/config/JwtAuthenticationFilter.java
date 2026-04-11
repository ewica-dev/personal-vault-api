package com.ewicadev.personalvaultapi.config;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ewicadev.personalvaultapi.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String jwt = authHeader.substring(7);
      String userEmail = jwtService.extractEmail(jwt);

      if (userEmail != null
          && SecurityContextHolder.getContext().getAuthentication() == null
          && jwtService.isTokenValid(jwt, userEmail)) {

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userEmail, null, new ArrayList<>());

        authToken.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    } catch (Exception e) {
      // invalid token, continue without authentication
    }

    filterChain.doFilter(request, response);
  }
}