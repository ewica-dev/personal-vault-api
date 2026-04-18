package com.ewicadev.personalvaultapi.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
      String role = jwtService.extractRole(jwt);

      if (userEmail != null
          && SecurityContextHolder.getContext().getAuthentication() == null
          && jwtService.isTokenValid(jwt, userEmail)) {

        var authorities = role != null 
            ? java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            : new ArrayList<SimpleGrantedAuthority>();

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userEmail, null, (Collection<SimpleGrantedAuthority>) authorities);

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