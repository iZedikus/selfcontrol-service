package ru.stepanov.selfcontrol.common;
import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestControllerAdvice
public class GlobalExceptionHandler { @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,String>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));} @ExceptionHandler(IllegalStateException.class) ResponseEntity<Map<String,String>> state(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",e.getMessage()));} }
